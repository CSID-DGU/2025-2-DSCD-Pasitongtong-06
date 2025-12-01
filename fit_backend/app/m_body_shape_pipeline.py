import cv2
import mediapipe as mp
import numpy as np

# ============================
# 1. 설정 및 초기화
# ============================
ARM_THICK_PX       = 55    # 팔 제거 라인 두께
EXTEND_TO_KNEE     = True  # 팔 제거 연장
SHOULDER_OFFSET_PX = 15    # 어깨선 위 보호구간

# 미디어 파이프 모델 로드
mp_self  = mp.solutions.selfie_segmentation
mp_pose  = mp.solutions.pose

# 전역 모델 초기화 (서버 시작 시 한 번만 로드됨)
seg  = mp_self.SelfieSegmentation(model_selection=1)
pose = mp_pose.Pose(static_image_mode=True, model_complexity=1, enable_segmentation=False)

# 체형 분류 기준값
TH = {
    "R2_band": 0.12,
    "R1_big":  1.47,
}
b     = float(TH["R2_band"])
t_big = float(TH["R1_big"])


# ============================
# 2. 전처리 및 계산 로직
# ============================
def get_mask(img):
    H, W = img.shape[:2]
    rgb = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
    seg_out = seg.process(rgb).segmentation_mask

    m = (seg_out > 0.60).astype(np.uint8)
    
    # 노이즈 제거
    k3 = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (3, 3))
    k5 = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (5, 5))
    m = cv2.medianBlur(m, 5)
    m = cv2.morphologyEx(m, cv2.MORPH_OPEN, k3, iterations=1)
    m = cv2.morphologyEx(m, cv2.MORPH_CLOSE, k5, iterations=1)

    # 가장 큰 덩어리만 남기기 (사람만)
    num_labels, labels, stats, _ = cv2.connectedComponentsWithStats(m, connectivity=8)
    if num_labels > 1:
        areas = stats[1:, cv2.CC_STAT_AREA]
        idx = 1 + int(np.argmax(areas))
        m = (labels == idx).astype(np.uint8)

    return m

def get_pose(img):
    return pose.process(cv2.cvtColor(img, cv2.COLOR_BGR2RGB))

def _pt(lm, idx, W, H):
    a = lm[idx]
    return np.array([a.x * W, a.y * H], dtype=np.float32)

def remove_arms_all(mask, lm, W, H):
    arm = np.zeros_like(mask, dtype=np.uint8)
    L = mp_pose.PoseLandmark

    LS = _pt(lm, L.LEFT_SHOULDER,  W, H)
    RS = _pt(lm, L.RIGHT_SHOULDER, W, H)
    x1, y1 = LS
    x2, y2 = RS

    below = np.zeros_like(mask, dtype=np.uint8)
    if abs(x2 - x1) < 1e-6:
        y_top = int(np.clip(min(y1, y2), 0, H - 1))
        pts = np.array([[0, y_top], [W - 1, y_top], [W - 1, H - 1], [0, H - 1]], np.int32)
    else:
        m_slope = (y2 - y1) / (x2 - x1)
        y0 = int(np.clip(y1 + m_slope * (0 - x1),        0, H - 1))
        yW = int(np.clip(y1 + m_slope * ((W - 1) - x1),  0, H - 1))
        pts = np.array([[0, y0], [W - 1, yW], [W - 1, H - 1], [0, H - 1]], np.int32)

    cv2.fillConvexPoly(below, pts, 255)
    y_prot = int(min(LS[1], RS[1]) + SHOULDER_OFFSET_PX)
    below[:y_prot] = 0

    def draw_arm(side):
        SH = _pt(lm, getattr(mp_pose.PoseLandmark, f"{side}_SHOULDER"), W, H)
        EL = _pt(lm, getattr(mp_pose.PoseLandmark, f"{side}_ELBOW"),    W, H)
        WR = _pt(lm, getattr(mp_pose.PoseLandmark, f"{side}_WRIST"),    W, H)
        ext_idx = getattr(mp_pose.PoseLandmark, f"{side}_KNEE" if EXTEND_TO_KNEE else f"{side}_ANKLE")
        EXT = _pt(lm, ext_idx, W, H)
        t = int(max(6, ARM_THICK_PX))
        
        for a, b in [(SH, EL), (EL, WR), (WR, EXT)]:
            cv2.line(arm, tuple(a.astype(int)), tuple(b.astype(int)), 255, t)
        
        xw, yw = int(WR[0]), int(WR[1])
        yb = int(min(H - 1, max(yw, EXT[1])))
        cv2.rectangle(arm, (xw - t // 2, yw), (xw + t // 2, yb), 255, -1)

    draw_arm("LEFT")
    draw_arm("RIGHT")

    arm = cv2.bitwise_and(arm, arm, mask=below)
    out = mask.copy()
    out[arm > 0] = 0
    out = cv2.morphologyEx(out, cv2.MORPH_OPEN, cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (9, 9)))
    return out

def chord_ends(mask, y, x_mid=None):
    H, W = mask.shape[:2]
    if y < 0 or y >= H: return None
    row = mask[y] > 0
    if not row.any(): return None

    xs = np.where(row)[0]
    segments = []
    start = xs[0]
    prev = xs[0]
    for x in xs[1:]:
        if x == prev + 1:
            prev = x
        else:
            segments.append((start, prev))
            start = x
            prev = x
    segments.append((start, prev))

    if x_mid is not None:
        for L0, R0 in segments:
            if L0 <= x_mid <= R0 and (R0 - L0) >= 5:
                return int(L0), int(R0)

    L0, R0 = max(segments, key=lambda s: s[1] - s[0])
    if R0 - L0 < 5: return None
    return int(L0), int(R0)

def x_at_y_on_segment(p1, p2, y):
    y0, y1 = float(p1[1]), float(p2[1])
    x0, x1 = float(p1[0]), float(p2[0])
    if abs(y1 - y0) < 1e-6: return int(x0)
    t = (y - y0) / (y1 - y0)
    t = max(0.0, min(1.0, t))
    return int(x0 + t * (x1 - x0))

def measure_oriented(img):
    H, W = img.shape[:2]
    mask = get_mask(img)
    pr = get_pose(img)
    if not pr or not pr.pose_landmarks:
        return None

    lm = pr.pose_landmarks.landmark
    L = mp_pose.PoseLandmark

    sL = _pt(lm, L.LEFT_SHOULDER, W, H)
    sR = _pt(lm, L.RIGHT_SHOULDER, W, H)
    hL = _pt(lm, L.LEFT_HIP,      W, H)
    hR = _pt(lm, L.RIGHT_HIP,     W, H)

    x_mid = int(0.5 * (sL[0] + sR[0]))
    y_sh = int(round(min(sL[1], sR[1]) - 0.01 * H))
    y_sh = max(0, y_sh)
    y_hip = int(round((hL[1] + hR[1]) * 0.5))
    y_w = int(round(y_sh + 0.50 * (y_hip - y_sh)))

    mask2 = remove_arms_all(mask, lm, W, H)
    mask2 = cv2.medianBlur(mask2.astype(np.uint8), 7)
    mask2 = cv2.morphologyEx(mask2, cv2.MORPH_CLOSE, cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (5, 5)))

    # Top
    ends = chord_ends(mask2, y_sh, x_mid=x_mid)
    if not ends: return None
    top = dict(left=ends[0], right=ends[1], width=ends[1] - ends[0], y=y_sh)

    # Waist
    waist = None
    bestW = 1e9
    y_lo = max(y_sh + 5, int(y_w - 15))
    y_hi = min(y_hip - 5, int(y_w + 15))

    for y in range(y_lo, y_hi + 1):
        seg = chord_ends(mask2, y, x_mid=x_mid)
        if not seg: continue
        segL, segR = seg
        
        x_l_frame = x_at_y_on_segment(sL, hL, y)
        x_r_frame = x_at_y_on_segment(sR, hR, y)
        if x_l_frame > x_r_frame: x_l_frame, x_r_frame = x_r_frame, x_l_frame
        
        Lc = max(segL, int(x_l_frame))
        Rc = min(segR, int(x_r_frame))
        if Rc - Lc <= 5: continue
        
        w = Rc - Lc
        shoulder_w = abs(sR[0] - sL[0])
        if w > shoulder_w * 1.4: continue
        
        if w < bestW:
            bestW = w
            waist = dict(left=int(Lc), right=int(Rc), width=int(w), y=int(y))

    if waist is None: return None

    # Hip
    hip = None
    bestH = -1
    base_L = int(min(hL[0], hR[0]))
    base_R = int(max(hL[0], hR[0]))
    margin = int(max(12, 0.06 * W))
    Lb = max(0, base_L - margin)
    Rb = min(W - 1, base_R + margin)
    y_lo_h = max(0, y_hip - 25)
    y_hi_h = min(H - 1, y_hip + 25)

    for y in range(y_lo_h, y_hi_h + 1):
        seg = chord_ends(mask2, y)
        if not seg: continue
        Lc = max(seg[0], Lb)
        Rc = min(seg[1], Rb)
        if Rc - Lc <= 0: continue
        w = Rc - Lc
        if w > bestH:
            bestH = w
            hip = dict(left=int(Lc), right=int(Rc), width=int(w), y=int(y))

    if hip is None: return None
    
    return dict(mask=mask2, pose=pr, top=top, waist=waist, hip=hip)

def zone_r2(r2):
    if r2 > 1.0 + b: return "Up"
    if r2 < 1.0 - b: return "Down"
    return "Mid"

def assign_label_r2gate(r1, r2):
    z = zone_r2(r2)
    if z == "Up":
        return "Small_Inverted_Triangle"
    if r1 >= t_big:
        return "Big_Rectangle"
    return "Rectangle"

# ============================
# [중요] 3. 백엔드 연결 함수
# ============================
def predict_male_body_shape(image_path: str) -> str:
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