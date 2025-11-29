# clothes_pipeline.py
import os
import io
from pathlib import Path

import torch
import torch.nn as nn
import torch.nn.functional as F
import timm
from torchvision import transforms
from PIL import Image
from ultralytics import YOLO
from rembg import remove as rembg_remove  # 🔹 배경 제거 라이브러리 추가

# ================================
# 1) 경로/환경 설정
# ================================
BASE_DIR = Path(__file__).resolve().parent

YOLO_MODEL_PATH = os.getenv(
    "YOLO_MODEL_PATH",
    str(BASE_DIR / "models" / "best.pt")  # ./models/best.pt
)

ATTR_MODEL_PATH = os.getenv(
    "ATTR_MODEL_PATH",
    str(BASE_DIR / "models" / "ckpt_attrlite_best_final_tf_efficientnet_b5_ns.pth")
)

device = "cuda" if torch.cuda.is_available() else "cpu"
backbone_name = "tf_efficientnet_b5_ns"
img_size = 456

YOLO_CONF_THRES = 0.25   # YOLO confidence
SCORE_THRES = 0.40       # 최종 박스 필터 threshold

# ================================
# 2) YOLO 모델 로드
# ================================
yolo_model = YOLO(YOLO_MODEL_PATH)

# ================================
# 3) EfficientNet 속성 분류 모델 구성
# ================================
ckpt = torch.load(ATTR_MODEL_PATH, map_location="cpu")
LS = ckpt.get("labelspace")

major_to_idx   = LS["major_to_idx"]
minor_to_idx   = LS["minor_to_idx"]
color_to_idx   = LS["color_to_idx"]
sleeve_to_idx  = LS["sleeve_to_idx"]
neck_to_idx    = LS["neckline_to_idx"]
collar_to_idx  = LS["collar_to_idx"]
fit_to_idx     = LS["fit_to_idx"]
length_to_idx  = LS["length_to_idx"]
print_to_idx   = LS["print_to_idx"]

idx_to = lambda d: {v: k for k, v in d.items()}
idx_to_major   = idx_to(major_to_idx)
idx_to_color   = idx_to(color_to_idx)
idx_to_sleeve  = idx_to(sleeve_to_idx)
idx_to_neck    = idx_to(neck_to_idx)
idx_to_collar  = idx_to(collar_to_idx)
idx_to_fit     = idx_to(fit_to_idx)
idx_to_length  = idx_to(length_to_idx)
idx_to_print   = idx_to(print_to_idx)

thr_prints = float(ckpt.get("thr_map_taskwise", {}).get("prints", 0.5))


class GeM(nn.Module):
    def __init__(self, p=3.0, eps=1e-6, learn_p=True):
        super().__init__()
        self.p = nn.Parameter(torch.ones(1) * p) if learn_p else torch.tensor([p])
        self.eps = eps

    def forward(self, x):
        x = torch.clamp(x, min=self.eps).pow(self.p.unsqueeze(-1).unsqueeze(-1))
        x = F.avg_pool2d(x, kernel_size=(x.size(-2), x.size(-1)))
        return x.pow(1 / self.p).squeeze(-1).squeeze(-1)


class InferenceMultiHead(nn.Module):
    def __init__(self):
        super().__init__()
        self.backbone = timm.create_model(
            backbone_name,
            pretrained=False,
            num_classes=0,
            global_pool=""
        )
        self.pool = GeM(p=3.0, learn_p=True)
        feat_dim = self.backbone.num_features

        self.attr_gate = nn.Sequential(
            nn.LayerNorm(feat_dim),
            nn.Linear(feat_dim, feat_dim // 8),
            nn.ReLU(),
            nn.Linear(feat_dim // 8, feat_dim),
            nn.Sigmoid()
        )

        self.head_major = nn.Linear(feat_dim, len(major_to_idx))
        self.head_minor = nn.ModuleDict({
            maj: nn.Linear(feat_dim, len(m2i)) for maj, m2i in minor_to_idx.items()
        })

        def mlp(n_out):
            if n_out <= 0:
                return None
            return nn.Sequential(
                nn.LayerNorm(feat_dim),
                nn.Dropout(0.4),
                nn.Linear(feat_dim, feat_dim // 2),
                nn.ReLU(),
                nn.Linear(feat_dim // 2, n_out)
            )

        self.head_color   = mlp(len(color_to_idx))
        self.head_sleeve  = mlp(len(sleeve_to_idx))
        self.head_neck    = mlp(len(neck_to_idx))
        self.head_collar  = mlp(len(collar_to_idx))
        self.head_fit     = mlp(len(fit_to_idx))
        self.head_length  = mlp(len(length_to_idx))
        self.head_print   = mlp(len(print_to_idx))

    def forward(self, x):
        fm = self.backbone(x)
        feat = self.pool(fm)

        gate = self.attr_gate(feat)
        feat_single = feat * gate  # 멀티헤드에 공통으로 사용

        out = {}
        out["logit_major"] = self.head_major(feat_single)

        maj_idx = out["logit_major"].argmax(1).item()
        major = idx_to_major[maj_idx]
        out["minor_major_name"] = major
        out["logit_minor"] = self.head_minor[major](feat_single)

        def apply(head):
            return head(feat_single) if head else None

        out["logit_color"]  = apply(self.head_color)
        out["logit_sleeve"] = apply(self.head_sleeve)
        out["logit_neck"]   = apply(self.head_neck)
        out["logit_collar"] = apply(self.head_collar)
        out["logit_fit"]    = apply(self.head_fit)
        out["logit_length"] = apply(self.head_length)
        out["logit_print"]  = apply(self.head_print)

        return out


attr_model = InferenceMultiHead().to(device).eval()
attr_model.load_state_dict(ckpt["model"], strict=False)

# ================================
# 4) 이미지 전처리
# ================================
tf = transforms.Compose([
    transforms.Resize(int(img_size * 1.08)),
    transforms.CenterCrop(img_size),
    transforms.ToTensor(),
    transforms.Normalize((0.485, 0.456, 0.406),
                         (0.229, 0.224, 0.225)),
])


def run_attr_inference(img_path: str):
    img = Image.open(img_path).convert("RGB")
    x = tf(img).unsqueeze(0).to(device)

    with torch.no_grad():
        out = attr_model(x)

    res = {}

    # major
    maj_idx = int(out["logit_major"].argmax(1).item())
    res["major"] = idx_to_major[maj_idx]

    # minor
    minor_major_name = out["minor_major_name"]
    mi = int(out["logit_minor"].argmax(1).item())
    minor_inv = {v: k for k, v in minor_to_idx[minor_major_name].items()}
    res["minor"] = minor_inv.get(mi)

    def pick1(logit, inv):
        if logit is None:
            return None
        idx = int(logit.argmax(1).item())
        return inv.get(idx)

    res["color"]    = pick1(out["logit_color"],  idx_to_color)
    res["sleeve"]   = pick1(out["logit_sleeve"], idx_to_sleeve)
    res["neckline"] = pick1(out["logit_neck"],   idx_to_neck)
    res["collar"]   = pick1(out["logit_collar"], idx_to_collar)
    res["fit"]      = pick1(out["logit_fit"],    idx_to_fit)
    res["length"]   = pick1(out["logit_length"], idx_to_length)

    prints = []
    if out["logit_print"] is not None:
        prob = torch.sigmoid(out["logit_print"][0]).cpu().tolist()
        for idx, p in enumerate(prob):
            if p >= thr_prints:
                prints.append(idx_to_print[idx])
    res["prints"] = prints

    return res


# ================================
# 5) 후처리: major/minor별로 필요한 속성만 유지
# ================================
TOP_NECKLINE = {"니트웨어", "브라탑", "블라우스", "탑", "티셔츠", "후드티"}
TOP_COLLAR   = {"셔츠", "블라우스"}
OUTER_COLLAR = {"재킷", "코트"}
ONEPIECE_NECKLINE = {"드레스", "점프수트"}


def postprocess_by_minor(res: dict) -> dict:
    major = res.get("major")
    minor = res.get("minor")

    base_keep = {"major", "minor", "color", "fit", "length", "prints"}
    keep = set(base_keep)

    if major == "하의":
        pass
    elif major == "상의":
        keep.add("sleeve")
        if minor in TOP_NECKLINE:
            keep.add("neckline")
        if minor in TOP_COLLAR:
            keep.add("collar")
    elif major == "아우터":
        keep.add("sleeve")
        if minor in OUTER_COLLAR:
            keep.add("collar")
    elif major == "원피스":
        keep.add("sleeve")
        if minor in ONEPIECE_NECKLINE:
            keep.add("neckline")

    return {k: v for k, v in res.items() if k in keep}


# ================================
# 6) 배경 제거 함수 (rembg)
# ================================
def remove_background_with_rembg(pil_img: Image.Image, save_path: str):
    """
    rembg(U^2-Net 기반)로 배경 제거 후 RGBA PNG 저장
    """
    try:
        out = rembg_remove(pil_img.convert("RGBA"))
        # rembg 버전에 따라 bytes 또는 PIL.Image가 리턴될 수 있음
        if isinstance(out, bytes):
            out = Image.open(io.BytesIO(out)).convert("RGBA")
        else:
            out = out.convert("RGBA")
        out.save(save_path)
    except Exception as e:
        print(f"[Warn] rembg 배경제거 실패, 원본 저장: {save_path} / {e}")
        pil_img.convert("RGBA").save(save_path)


# ================================
# 7) 전체 파이프라인: YOLO → crop → rembg → attr
# ================================
def process_image(image_path: str,
                  save_crops_dir: str | os.PathLike | None = None):
    """
    하나의 전체 코디 이미지에서 의류 bbox들을 찾아서,
    각 bbox에 대해 속성 분류 결과 리스트를 반환.
    - crops/original: 원본 크롭 (속성 분석용)
    - crops/nobg: 배경 제거 크롭 (프론트엔드 표시용)
    """
    image_path = str(image_path)
    if not os.path.exists(image_path):
        raise FileNotFoundError(f"이미지 없음: {image_path}")

    if save_crops_dir is None:
        save_crops_dir = BASE_DIR / "crops"

    # 폴더 구조 생성
    orig_dir = os.path.join(save_crops_dir, "original")
    nobg_dir = os.path.join(save_crops_dir, "nobg")
    os.makedirs(orig_dir, exist_ok=True)
    os.makedirs(nobg_dir, exist_ok=True)

    # YOLO 추론
    results = yolo_model(
        source=image_path,
        conf=YOLO_CONF_THRES,
        verbose=False
    )

    if len(results) == 0 or results[0].boxes is None or len(results[0].boxes) == 0:
        return []

    img = Image.open(image_path).convert("RGB")
    w, h = img.size

    out = []
    boxes_xyxy = results[0].boxes.xyxy.cpu().tolist()
    scores = results[0].boxes.conf.cpu().tolist()

    base = os.path.splitext(os.path.basename(image_path))[0]

    for i, (box, score) in enumerate(zip(boxes_xyxy, scores)):
        if float(score) < SCORE_THRES:
            continue

        x1, y1, x2, y2 = map(int, box)

        x1 = max(0, min(x1, w - 1))
        x2 = max(0, min(x2, w))
        y1 = max(0, min(y1, h - 1))
        y2 = max(0, min(y2, h))

        if x2 <= x1 or y2 <= y1:
            continue

        crop = img.crop((x1, y1, x2, y2))
        
        # 1) 원본 저장 (JPG)
        crop_filename = f"crop_{base}_{i}.jpg"
        crop_path = os.path.join(orig_dir, crop_filename)
        crop.save(crop_path)

        # 2) 배경제거 저장 (PNG)
        nobg_filename = f"crop_{base}_{i}.png"
        crop_nobg_path = os.path.join(nobg_dir, nobg_filename)
        remove_background_with_rembg(crop, crop_nobg_path)

        # 3) 속성 추론 (원본 crop 사용)
        res = run_attr_inference(crop_path)
        res = postprocess_by_minor(res)

        res["crop_path"] = crop_path
        res["crop_nobg_path"] = crop_nobg_path
        res["bbox"] = [x1, y1, x2, y2]
        res["score"] = float(score)

        out.append(res)

    return out