import cv2
import mediapipe as mp
import numpy as np

# 런타임 / 전처리
ARM_THICK_PX       = 55    # 팔 제거 라인 두께(픽셀단위)
EXTEND_TO_KNEE     = True  # 팔 제거 연장(손목-모릎까지)
SHOULDER_OFFSET_PX = 15    # 어깨선 위 보호구간 설정 (팔 제거라인이 어깨까지 가지 않게)

# 미디어 파이프
mp_self  = mp.solutions.selfie_segmentation
mp_pose  = mp.solutions.pose
mp_draw  = mp.solutions.drawing_utils
mp_style = mp.solutions.drawing_styles

seg  = mp_self.SelfieSegmentation(model_selection=1)
pose = mp_pose.Pose(static_image_mode=True, model_complexity=1, enable_segmentation=False)

# 규칙 적용
TH = {
    "R2_band": 0.12,
    "R1_big":  1.47,
}
b      = float(TH["R2_band"])
t_big = float(TH["R1_big"])


# 포즈 / 마스크 / 실루엣 조정
def get_mask(img):
    H, W = img.shape[:2]
    rgb = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
    seg_out = seg.process(rgb).segmentation_mask

    # 더 타이트한 실루엣
    m = (seg_out > 0.60).astype(np.uint8)

    k3 = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (3, 3))
    k5 = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (5, 5))

    m = cv2.medianBlur(m, 5)
    m = cv2.morphologyEx(m, cv2.MORPH_OPEN, k3, iterations=1)
    m = cv2.morphologyEx(m, cv2.MORPH_CLOSE, k5, iterations=1)

    # 가장 큰 컴포넌트만 유지
    num_labels, labels, stats, _ = cv2.connectedComponentsWithStats(m, connectivity=8)
    if num_labels > 1:
        areas = stats[1:, cv2.CC_STAT_AREA]
        idx = 1 + int(np.argmax(areas))
        m = (labels == idx).astype(np.uint8)

    return m

def get_pose(img):
    return pose.process(cv2.cvtColor(img, cv2.COLOR_BGR2RGB))

def _pt(lm, idx, W, H): #랜드마크 정규 좌표 → 픽셀 좌표로 변환 함수
    a = lm[idx]
    return np.array([a.x * W, a.y * H], dtype=np.float32)

def remove_arms_all(mask, lm, W, H): # 팔 제거 함수
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
        ext_idx = getattr(
            mp_pose.PoseLandmark,
            f"{side}_KNEE" if EXTEND_TO_KNEE else f"{side}_ANKLE"
        )
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
    out = cv2.morphologyEx(
        out,
        cv2.MORPH_OPEN,
        cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (9, 9))
    )
    return out

def chord_ends(mask, y, x_mid=None):
    H, W = mask.shape[:2]
    if y < 0 or y >= H:
        return None

    row = mask[y] > 0
    if not row.any():
        return None

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

    # 중심(t_big)을 포함하는 실루엣 우선
    if x_mid is not None:
        for L0, R0 in segments:
            if L0 <= x_mid <= R0 and (R0 - L0) >= 5:
                return int(L0), int(R0)

    L0, R0 = max(segments, key=lambda s: s[1] - s[0])
    if R0 - L0 < 5:
        return None
    return int(L0), int(R0)

def x_at_y_on_segment(p1, p2, y):
    y0, y1 = float(p1[1]), float(p2[1])
    x0, x1 = float(p1[0]), float(p2[0])
    if abs(y1 - y0) < 1e-6:
        return int(x0)
    t = (y - y0) / (y1 - y0)
    t = max(0.0, min(1.0, t))
    return int(x0 + t * (x1 - x0))

# 값 측정 함수
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

    mask2 = remove_arms_all(mask, lm, W, H) # 팔 제거+ 전처리된 실루엣(마스크)
    mask2 = cv2.medianBlur(mask2.astype(np.uint8), 7)
    mask2 = cv2.morphologyEx(
        mask2, cv2.MORPH_CLOSE,
        cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (5, 5))
    )

    # Shoulder / top
    ends = chord_ends(mask2, y_sh, x_mid=x_mid)
    if not ends:
        return None
    top = dict(left=ends[0], right=ends[1],
               width=ends[1] - ends[0], y=y_sh)

    # Waist: 중심 + 프레임 + 팔구멍 안건너기
    waist = None
    bestW = 1e9
    y_lo = max(y_sh + 5, int(y_w - 15))
    y_hi = min(y_hip - 5, int(y_w + 15))

    for y in range(y_lo, y_hi + 1):
        seg = chord_ends(mask2, y, x_mid=x_mid)
        if not seg:
            continue
        segL, segR = seg

        x_l_frame = x_at_y_on_segment(sL, hL, y)
        x_r_frame = x_at_y_on_segment(sR, hR, y)
        if x_l_frame > x_r_frame:
            x_l_frame, x_r_frame = x_r_frame, x_l_frame

        Lc = max(segL, int(x_l_frame))
        Rc = min(segR, int(x_r_frame))
        if Rc - Lc <= 5:
            continue

        w = Rc - Lc
        shoulder_w = abs(sR[0] - sL[0])
        if w > shoulder_w * 1.4:
            continue

        if w < bestW:
            bestW = w
            waist = dict(left=int(Lc), right=int(Rc),
                         width=int(w), y=int(y))

    if waist is None:
        return None

    # Hip: 최대폭
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
        if not seg:
            continue
        Lc = max(seg[0], Lb)
        Rc = min(seg[1], Rb)
        if Rc - Lc <= 0:
            continue
        w = Rc - Lc
        if w > bestH:
            bestH = w
            hip = dict(left=int(Lc), right=int(Rc),
                       width=int(w), y=int(y))

    if hip is None:
        return None

    return dict(mask=mask2, pose=pr, top=top, waist=waist, hip=hip)

# 이미지 위에 선 & 도형 그리기
def draw_shape_icon(vis, M, label):
    top, waist, hip = M["top"], M["waist"], M["hip"]
    yT, yH = top["y"], hip["y"]
    overlay = vis.copy()

    def fill(poly, color, alpha=0.28):
        cv2.fillPoly(overlay, [np.array(poly, np.int32)], color)
        cv2.addWeighted(overlay, alpha, vis, 1 - alpha, 0, vis)

    COL = {
        "Rectangle":               (200,   0, 200),
        "Big_Rectangle":           (120,   0, 255),
        "Small_Inverted_Triangle": (255, 160,   0),
    } # 체형별 색상 설정

    if label == "Small_Inverted_Triangle":
        cx = int((top["left"] + top["right"]) / 2)
        fill([(top["left"], yT), (top["right"], yT),
              (hip["right"], yH), (hip["left"], yH)], COL[label])
        cv2.circle(vis, (cx, yT), 6, (0, 0, 0), -1)
    elif label == "Big_Rectangle":
        left = min(top["left"], hip["left"])
        right = max(top["right"], hip["right"])
        fill([(left, yT), (right, yT), (right, yH), (left, yH)], COL[label])
    else:  # Rectangle
        left = max(top["left"], hip["left"])
        right = min(top["right"], hip["right"])
        fill([(left, yT), (right, yT), (right, yH), (left, yH)], COL[label])

def draw_overlay(img, M, label):
    vis = img.copy()
    base = vis.copy()
    base[M["mask"] > 0] = (
        base[M["mask"] > 0] * 0.4
        + np.array([40, 200, 40]) * 0.6
    ).astype(np.uint8)
    vis = cv2.addWeighted(base, 0.6, vis, 0.4, 0)
    if M["pose"] and M["pose"].pose_landmarks:
        mp_draw.draw_landmarks(
            vis, M["pose"].pose_landmarks,
            mp_pose.POSE_CONNECTIONS,
            landmark_drawing_spec=mp_style.get_default_pose_landmarks_style()
        )
    for k, c in [("top", (0, 0, 255)),
                 ("waist", (255, 128, 0)),
                 ("hip", (0, 255, 0))]: # 어깨, 허리, 힙 색 구분 & 측정선 표시
        L = M[k]
        cv2.line(vis, (L["left"], L["y"]), (L["right"], L["y"]), c, 2)

    draw_shape_icon(vis, M, label)
    return vis

# 규칙1 : R2
def zone_r2(r2):
    if r2 > 1.0 + b:
        return "Up" # 역삼각
    if r2 < 1.0 - b:
        return "Down" # 삼각
    return "Mid" # 사각 & 모래시계

# 규칙2 : R1
def assign_label_r2gate(r1, r2):
    z = zone_r2(r2)
    if z == "Up":
        return "Small_Inverted_Triangle" # 작은 역삼각

    if r1 >= t_big:
        return "Big_Rectangle" # 큰사각
    return "Rectangle"         # 사각


# --- 백엔드 호출용 함수 ---
def classify_image(image_bytes, return_overlay=False):
    try:
        np_arr = np.frombuffer(image_bytes, np.uint8)
        img = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)
        if img is None:
            raise ValueError("cv2.imdecode failed")
    except Exception as e:
        return {"ok": False, "error": f"Image decoding failed: {e}"}

    M = measure_oriented(img)
    if M is None:
        return {"ok": False, "error": "Measurement failed (pose not detected or body parts not clear)"}

    shoulder_w = int(M["top"]["width"])
    waist_w = int(M["waist"]["width"])
    hip_w = int(M["hip"]["width"])

    R1_cal = shoulder_w / max(1, waist_w)
    R2_cal = shoulder_w / max(1, hip_w)

    label = assign_label_r2gate(R1_cal, R2_cal)
    zone = zone_r2(R2_cal)

    result = {
        "ok": True,
        "gender": "male",
        "label": label,
        "R1_cal": round(R1_cal, 4),
        "R2_cal": round(R2_cal, 4),
        "zone": zone,
        "measurements": {
            "shoulder_w": shoulder_w,
            "waist_w": waist_w,
            "hip_w": hip_w,
            "waist_y": int(M["waist"]["y"]),
            "hip_y": int(M["hip"]["y"])
        }
    }

    if return_overlay:
        overlay_img = draw_overlay(img, M, label)
        is_success, buffer = cv2.imencode(".png", overlay_img)
        if is_success:
            result["overlay_png_bytes"] = buffer.tobytes()
        else:
            result["overlay_png_bytes"] = None

    return result