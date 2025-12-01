import cv2
import mediapipe as mp
import numpy as np

# ============================
# 1. 설정 및 초기화
# ============================
ARM_THICK_PX       = 55    # 팔 제거 라인 두께
EXTEND_TO_KNEE     = True  # 팔 제거 연장
SHOULDER_OFFSET_PX = 15    # 어깨선 위 보호구간

# 미디어 파이프
mp_self = mp.solutions.selfie_segmentation
mp_pose = mp.solutions.pose

# 전역 모델 초기화
seg = mp_self.SelfieSegmentation(model_selection=1)
pose = mp_pose.Pose(static_image_mode=True, model_complexity=1, enable_segmentation=False)

# [중요] 여성 체형 분류 기준값 (수정하지 않고 유지)
TH = {'R2_band': 0.075, 't_mid': 1.44}
b = float(TH['R2_band'])
t_mid = float(TH['t_mid'])

# ============================
# 2. 전처리 및 계산 로직
# ============================
def get_mask(img):
    m = seg.process(cv2.cvtColor(img, cv2.COLOR_BGR2RGB)).segmentation_mask
    m = (m > 0.40).astype(np.uint8)
    m = cv2.medianBlur(m, 5)
    m = cv2.morphologyEx(m, cv2.MORPH_CLOSE,
                         cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (7, 7)), 2)
    return m

def get_pose(img):
    return pose.process(cv2.cvtColor(img, cv2.COLOR_BGR2RGB))

def _pt(lm, idx, W, H):
    a = lm[idx]
    return np.array([a.x * W, a.y * H], dtype=np.float32)

def remove_arms_all(mask, lm, W, H):
    arm = np.zeros_like(mask, dtype=np.uint8)
    L = mp_pose.PoseLandmark
    LS = _pt(lm, L.LEFT_SHOULDER, W, H)
    RS = _pt(lm, L.RIGHT_SHOULDER, W, H)
    x1, y1 = LS; x2, y2 = RS
    
    below = np.zeros_like(mask, dtype=np.uint8)
    if abs(x2 - x1) < 1e-6:
        y_top = int(np.clip(min(y1, y2), 0, H - 1))
        pts = np.array([[0, y_top], [W - 1, y_top], [W - 1, H - 1], [0, H - 1]], np.int32)
    else:
        m = (y2 - y1) / (x2 - x1)
        y0 = int(np.clip(y1 + m * (0 - x1), 0, H - 1))
        yW = int(np.clip(y1 + m * ((W - 1) - x1), 0, H - 1))
        pts = np.array([[0, y0], [W - 1, yW], [W - 1, H - 1], [0, H - 1]], np.int32)
    
    cv2.fillConvexPoly(below, pts, 255)
    y_prot = int(min(LS[1], RS[1]) + SHOULDER_OFFSET_PX)
    below[:y_prot] = 0

    def draw_arm(side):
        SH = _pt(lm, getattr(mp_pose.PoseLandmark, f"{side}_SHOULDER"), W, H)
        EL = _pt(lm, getattr(mp_pose.PoseLandmark, f"{side}_ELBOW"), W, H)
        WR = _pt(lm, getattr(mp_pose.PoseLandmark, f"{side}_WRIST"), W, H)
        THU = _pt(lm, getattr(mp_pose.PoseLandmark, f"{side}_THUMB"), W, H)
        IN = _pt(lm, getattr(mp_pose.PoseLandmark, f"{side}_INDEX"), W, H)
        PI = _pt(lm, getattr(mp_pose.PoseLandmark, f"{side}_PINKY"), W, H)
        ext_idx = getattr(mp_pose.PoseLandmark, f"{side}_KNEE") if EXTEND_TO_KNEE else getattr(mp_pose.PoseLandmark, f"{side}_ANKLE")
        EXT = _pt(lm, ext_idx, W, H)
        t = int(max(6, ARM_THICK_PX))
        for a, b in [(SH, EL), (EL, WR), (WR, EXT)]:
            cv2.line(arm, tuple(a.astype(int)), tuple(b.astype(int)), 255, t)
        for tip in (THU, IN, PI):
            cv2.line(arm, tuple(WR.astype(int)), tuple(tip.astype(int)), 255, t)
            cv2.circle(arm, tuple(tip.astype(int)), t // 2, 255, -1)
        xw, yw = int(WR[0]), int(WR[1]); yb = int(min(H - 1, max(yw, EXT[1])))
        cv2.rectangle(arm, (xw - t // 2, yw), (xw + t // 2, yb), 255, -1)

    draw_arm("LEFT"); draw_arm("RIGHT")
    arm = cv2.bitwise_and(arm, arm, mask=below)
    out = mask.copy(); out[arm > 0] = 0
    out = cv2.morphologyEx(out, cv2.MORPH_OPEN,
                           cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (9, 9)))
    return out

def chord_ends(mask, y):
    if y < 0 or y >= mask.shape[0]: return None
    row = mask[y] > 0
    if not row.any(): return None
    xs = np.where(row)[0]
    if xs.size < 2: return None
    return int(xs[0]), int(xs[-1])

def x_at_y_on_segment(p1, p2, y):
    y0, y1 = float(p1[1]), float(p2[1]); x0, x1 = float(p1[0]), float(p2[0])
    if abs(y1 - y0) < 1e-6: return int(x0)
    t = (y - y0) / (y1 - y0); t = max(0.0, min(1.0, t))
    return int(x0 + t * (x1 - x0))

def measure_oriented(img):
    H, W = img.shape[:2]
    mask = get_mask(img)
    pr = get_pose(img)
    if not pr or not pr.pose_landmarks: return None
    lm = pr.pose_landmarks.landmark
    L = mp_pose.PoseLandmark

    sL = _pt(lm, L.LEFT_SHOULDER, W, H); sR = _pt(lm, L.RIGHT_SHOULDER, W, H)
    hL = _pt(lm, L.LEFT_HIP, W, H); hR = _pt(lm, L.RIGHT_HIP, W, H)

    y_sh = int(round(min(sL[1], sR[1]) - 0.01 * H)); y_sh = max(0, y_sh)
    y_hip = int(round((hL[1] + hR[1]) * 0.5))
    y_w = int(round(y_sh + 0.50 * (y_hip - y_sh)))

    mask2 = remove_arms_all(mask, lm, W, H)
    mask2 = cv2.medianBlur(mask2.astype(np.uint8), 7)
    mask2 = cv2.morphologyEx(mask2, cv2.MORPH_CLOSE,
                             cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (5, 5)))

    # shoulder/top
    ends = chord_ends(mask2, y_sh)
    if not ends: return None
    L0, R0 = ends
    top = dict(left=L0, right=R0, width=R0 - L0, y=y_sh)

    # waist
    waist = None; bestW = 1e9
    y_lo = max(y_sh + 1, y_w - 5); y_hi = min(y_hip - 1, y_w + 5)
    for y in range(y_lo, y_hi + 1):
        ends = chord_ends(mask2, y)
        if not ends: continue
        L_raw, R_raw = ends
        xL = min(x_at_y_on_segment(sL, hL, y), x_at_y_on_segment(sR, hR, y))
        xR = max(x_at_y_on_segment(sL, hL, y), x_at_y_on_segment(sR, hR, y))
        Lc = max(L_raw, xL); Rc = min(R_raw, xR)
        if Rc - Lc <= 0: continue
        w = Rc - Lc
        if w < bestW:
            bestW = w; waist = dict(left=Lc, right=Rc, width=w, y=y)
    if waist is None: return None

    # hip
    hip = None; bestH = -1
    base_L = int(min(hL[0], hR[0])); base_R = int(max(hL[0], hR[0]))
    margin = int(max(12, 0.06 * W))
    Lb = max(0, base_L - margin); Rb = min(W - 1, base_R + margin)
    y_lo = max(0, y_hip - 25); y_hi = min(H - 1, y_hip + 25)
    for y in range(y_lo, y_hi + 1):
        ends = chord_ends(mask2, y)
        if not ends: continue
        L_raw, R_raw = ends
        Lc = max(L_raw, Lb); Rc = min(R_raw, Rb)
        if Rc - Lc <= 0: continue
        w = Rc - Lc
        if w > bestH:
            bestH = w; hip = dict(left=Lc, right=Rc, width=w, y=y)
    if hip is None: return None

    return dict(mask=mask2, pose=pr, top=top, waist=waist, hip=hip)

# 규칙1 : R2
def zone_r2(r2):
    if r2 > 1.0 + b: return "Up" # 역삼각
    if r2 < 1.0 - b: return "Down" # 삼각
    return "Mid" # 사각 & 모래시계

# 규칙2 : R1
def assign_label_r2gate(r1, r2):
    z = zone_r2(r2)
    if z == "Up": return "Inverted Triangle"
    if z == "Down": return "Triangle"
    return "Hourglass" if r1 >= t_mid else "Rectangle"


# ============================
# [중요] 3. 백엔드 연결 함수
# ============================
def predict_female_body_shape(image_path: str) -> str:
    """
    app.py에서 호출하는 진입점 함수입니다.
    이미지 경로를 받아 분석 후 '체형 문자열'을 반환합니다.
    """
    try:
        # 1. 파일 경로에서 이미지 읽기
        img = cv2.imread(image_path)
        if img is None:
            return "Error: Cannot load image"

        # 2. 체형 측정
        M = measure_oriented(img)
        if M is None:
            return "Unknown" # 측정 실패

        # 3. 수치 계산
        shoulder_w = int(M["top"]["width"])
        waist_w = int(M["waist"]["width"])
        hip_w = int(M["hip"]["width"])

        R1_cal = shoulder_w / max(1, waist_w)
        R2_cal = shoulder_w / max(1, hip_w)

        # 4. 라벨 결정
        label = assign_label_r2gate(R1_cal, R2_cal)
        
        # 5. 결과 문자열 반환 (app.py의 result_shape로 들어감)
        return label

    except Exception as e:
        print(f"Error in prediction: {e}")
        return "Error"