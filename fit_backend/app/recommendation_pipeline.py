# recommendation_pipeline.py
import json
import math
import requests
from pathlib import Path
from typing import List, Dict, Any, Optional, Tuple, Set
import pandas as pd
from openai import OpenAI  # 🔹 OpenAI 추가
import os

# ============================================================
# 설정 및 상수
# ============================================================

BASE_DIR = Path(__file__).resolve().parent
DATA_DIR = BASE_DIR / "data"

XLSX_SCORING_PATH = DATA_DIR / "캡디_스코어링.xlsx"
OUTER2TOP_PATH = DATA_DIR / "outer2top_map.json"
TOP2BOTTOM_PATH = DATA_DIR / "top2bottom_map.json"

# 날씨 API 키
OPENWEATHER_API_KEY = os.getenv("OPENWEATHER_API_KEY")

# 🔹 OpenAI API 설정 (노트북 키 사용, 실제 배포시 환경변수 사용 권장)
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
if not OPENAI_API_KEY:
    print("[WARN] OPENAI_API_KEY가 설정되지 않았습니다.")

client = OpenAI(api_key=OPENAI_API_KEY)

# 가중치
W1_BASE = 0.7
W2_DETAIL = 0.3

# 카테고리 정의
TOP_MINOR = {"탑", "블라우스", "티셔츠", "니트웨어", "셔츠", "브라탑", "후드티"}
BOTTOM_MINOR = {"청바지", "팬츠", "스커트", "레깅스", "조거팬츠"}
OUTER_MINOR = {"코트", "재킷", "점퍼", "패딩", "베스트", "가디건", "짚업"}
ONEPIECE_MINOR = {"드레스", "점프수트", "수영복"}

# 온도별 허용 카테고리 및 제거 규칙 상수들
ALLOWED_TOPOUTER_MINOR_BY_BAND = {
    "한여름": {"탑", "티셔츠", "블라우스", "셔츠", "브라탑"},
    "봄/가을": {"티셔츠", "블라우스", "셔츠", "니트웨어", "후드티", "가디건", "재킷", "짚업"},
    "늦가을/초겨울": {"니트웨어", "후드티", "가디건", "재킷", "점퍼", "코트", "짚업"},
    "한겨울": {"니트웨어", "후드티", "코트", "패딩", "점퍼", "재킷", "가디건", "짚업", "베스트"},
}

ALLOWED_BOTTOM_MINOR_BY_BAND = {
    "한여름": {"스커트", "팬츠", "청바지", "레깅스", "조거팬츠"},
    "봄/가을": {"팬츠", "청바지", "레깅스", "조거팬츠", "스커트"},
    "늦가을/초겨울": {"팬츠", "청바지", "레깅스", "조거팬츠", "스커트"},
    "한겨울": {"청바지", "레깅스", "팬츠", "조거팬츠"},
}

HOT_REMOVE_MINOR_TOP_OUTER = {"코트", "패딩", "니트웨어", "점퍼", "재킷", "가디건"}
MILD_REMOVE_MINOR_TOP_OUTER = {"패딩", "수영복"}
COOL_REMOVE_MINOR_TOP_OUTER = {"수영복"}
COLD_REMOVE_MINOR_TOP_OUTER = {"수영복", "재킷", "가디건", "베스트", "짚업", "점퍼"}

MILD_REMOVE_SLEEVE = {"민소매"}
COOL_REMOVE_SLEEVE = {"민소매", "반팔", "캡"}
COLD_REMOVE_SLEEVE = {"민소매", "반팔", "캡", "7부소매"}

HOT_REMOVE_NECKLINE = {"터틀넥", "후드"}
MILD_REMOVE_NECKLINE = {"터틀넥", "홀터넥"}
COOL_COLD_REMOVE_NECKLINE = {"홀터넥"}


# ============================================================
# Helper Functions
# ============================================================

def get_today_avg_temp_from_api(city: str = "Seoul", country: str = "KR") -> float:
    if not OPENWEATHER_API_KEY:
        return 20.0 # Default fallback
    url = f"https://api.openweathermap.org/data/2.5/weather?q={city},{country}&appid={OPENWEATHER_API_KEY}&units=metric"
    try:
        resp = requests.get(url, timeout=5)
        resp.raise_for_status()
        data = resp.json()
        main_data = data.get("main", {})
        temp_min = float(main_data.get("temp_min", 20))
        temp_max = float(main_data.get("temp_max", 20))
        return (temp_min + temp_max) / 2.0
    except Exception:
        return 15.0 # Fallback temperature

def coarse_category(major: Optional[str], minor: Optional[str]) -> Optional[str]:
    if minor in TOP_MINOR: return "top"
    if minor in BOTTOM_MINOR: return "bottom"
    if minor in OUTER_MINOR: return "outer"
    if minor in ONEPIECE_MINOR: return "onepiece"
    if major == "상의": return "top"
    if major == "하의": return "bottom"
    if major == "아우터": return "outer"
    if major == "원피스": return "onepiece"
    return None

def temp_to_band(temp_avg: float) -> str:
    if temp_avg > 23: return "한여름"
    elif temp_avg > 17: return "봄/가을"
    elif temp_avg > 9: return "늦가을/초겨울"
    else: return "한겨울"

def band_to_anchor_season(band: str) -> str:
    mapping = {
        "한여름": "summer",
        "봄/가을": "spring_fall",
        "늦가을/초겨울": "late_fall_early_winter",
        "한겨울": "winter"
    }
    return mapping.get(band, "unknown")

def is_item_suitable_category_for_band(item: Dict[str, Any], band: str) -> bool:
    minor = item.get("minor")
    major = item.get("major")
    cat = coarse_category(major, minor)

    if band not in ALLOWED_TOPOUTER_MINOR_BY_BAND:
        return True

    if cat in {"top", "outer"}:
        return minor in ALLOWED_TOPOUTER_MINOR_BY_BAND[band]
    elif cat == "bottom":
        allowed = ALLOWED_BOTTOM_MINOR_BY_BAND.get(band, set())
        return minor in allowed if allowed else True
    elif cat == "onepiece":
        return False if band == "한겨울" else True
    return False

def is_item_allowed_for_temperature(item: Dict[str, Any], temp_avg: float) -> bool:
    band = temp_to_band(temp_avg)
    minor = item.get("minor")
    major = item.get("major")
    cat = coarse_category(major, minor)
    sleeve = item.get("sleeve")
    neckline = item.get("neckline")

    # 1. Category check
    if band == "한여름":
        if cat == "outer": return False
        if cat in {"top", "outer"} and minor in HOT_REMOVE_MINOR_TOP_OUTER: return False
    elif band == "봄/가을":
        if cat in {"top", "outer", "onepiece"} and minor in MILD_REMOVE_MINOR_TOP_OUTER: return False
    elif band == "늦가을/초겨울":
        if cat in {"top", "outer", "onepiece"} and minor in COOL_REMOVE_MINOR_TOP_OUTER: return False
    else: # 한겨울
        if cat in {"top", "outer", "onepiece"} and minor in COLD_REMOVE_MINOR_TOP_OUTER: return False

    # 2. Sleeve check
    if band == "봄/가을" and sleeve in MILD_REMOVE_SLEEVE: return False
    elif band == "늦가을/초겨울" and sleeve in COOL_REMOVE_SLEEVE: return False
    elif band == "한겨울" and sleeve in COLD_REMOVE_SLEEVE: return False

    # 3. Neckline check
    if band == "한여름" and neckline in HOT_REMOVE_NECKLINE: return False
    elif band == "봄/가을" and neckline in MILD_REMOVE_NECKLINE: return False
    elif band in {"늦가을/초겨울", "한겨울"} and neckline in COOL_COLD_REMOVE_NECKLINE: return False

    return True

def filter_by_temperature(wardrobe: List[Dict[str, Any]], temp_avg: float) -> Tuple[str, str, List[Dict[str, Any]]]:
    band = temp_to_band(temp_avg)
    anchor_season = band_to_anchor_season(band)
    filtered = []
    for item in wardrobe:
        if not is_item_suitable_category_for_band(item, band): continue
        if not is_item_allowed_for_temperature(item, temp_avg): continue
        filtered.append(item)
    return anchor_season, band, filtered

# ============================================================
# Loading Data
# ============================================================

def load_color_maps():
    try:
        with open(OUTER2TOP_PATH, encoding="utf-8") as f:
            outer2top = json.load(f)
        with open(TOP2BOTTOM_PATH, encoding="utf-8") as f:
            top2bottom = json.load(f)
        return outer2top, top2bottom
    except Exception as e:
        print(f"Color map load failed: {e}")
        return {}, {}

def load_shape_tag_weight_from_excel(gender: str) -> Dict[str, Dict[str, float]]:
    try:
        sheet_name = "여성 스코어" if gender == "female" else "남성 스코어"
        df = pd.read_excel(XLSX_SCORING_PATH, sheet_name=sheet_name)
        
        shape_cols = ["Unnamed: 2", "Unnamed: 3", "Unnamed: 4", "Unnamed: 5"]
        shape_names = {}
        for col in shape_cols:
            nm = df.loc[1, col]
            shape_names[col] = str(nm).strip()

        weight_table = {sn: {} for sn in shape_names.values()}

        for idx, row in df.iterrows():
            if idx <= 1: continue
            item = row["Unnamed: 1"]
            if not isinstance(item, str) or not item.strip(): continue
            tag_key = item.strip()

            for col in shape_cols:
                val = row[col]
                if pd.isna(val): continue
                if isinstance(val, str): val = val.replace("–", "-")
                try:
                    w = float(val)
                except: continue
                if math.isnan(w): continue
                
                shape = shape_names[col]
                weight_table[shape][tag_key] = w
        return weight_table
    except Exception as e:
        print(f"Excel load failed: {e}")
        return {}

# ============================================================
# Scoring Logic
# ============================================================

def extract_base_tags(item: Dict) -> List[str]:
    tags = []
    for key in ["fit", "length"]:
        val = item.get(key)
        if val: tags.append(str(val))
    return tags

def extract_detail_tags(item: Dict) -> List[str]:
    tags = []
    for key in ["neckline", "collar"]:
        val = item.get(key)
        if val: tags.append(str(val))
    prints = item.get("prints") or []
    for p in prints: tags.append(str(p))
    return tags

def score_item_split(item: Dict, user_shape: str, weight_table: Dict) -> Tuple[float, float]:
    shape_weights = weight_table.get(user_shape, {})
    base_score = sum(shape_weights.get(t, 0.0) for t in extract_base_tags(item))
    detail_score = sum(shape_weights.get(t, 0.0) for t in extract_detail_tags(item))
    return base_score, detail_score

def compute_outfit_score(top, bottom, outer, user_shape, shape_weight_table):
    base_total = 0.0
    detail_total = 0.0
    
    items = [x for x in [top, bottom, outer] if x]
    for it in items:
        b, d = score_item_split(it, user_shape, shape_weight_table)
        base_total += b
        detail_total += d
        
    return W1_BASE * base_total + W2_DETAIL * detail_total

def get_candidates(color: str, map_data: Dict) -> List[str]:
    rows = map_data.get(color, [])
    # prob 높은 순 정렬
    rows = sorted(rows, key=lambda x: x["prob"], reverse=True)
    # color 이름만 반환 (키값: top_color or bottom_color)
    return [list(r.values())[0] for r in rows] 

# ============================================================
# 🔹 LLM Prompt & Generation (새로 추가됨)
# ============================================================

def build_outfit_prompt(user_shape: str,
                        outer: Optional[Dict[str, Any]],
                        top: Optional[Dict[str, Any]],
                        bottom: Optional[Dict[str, Any]]) -> str:
    """outfit dict에서 코디 설명용 프롬프트 생성 (원피스 미고려)"""

    def _fmt_item(item: Optional[Dict[str, Any]]) -> Dict[str, str]:
        if not item:
            return {
                "minor": "없음", "sleeve": "없음", "length": "없음",
                "color": "없음", "prints": "없음", "neckline": "없음", "fit": "없음",
            }
        prints = item.get("prints") or []
        prints_str = ", ".join(map(str, prints)) if prints else "민무늬"
        return {
            "minor": item.get("minor", "없음"),
            "sleeve": item.get("sleeve", "없음"),
            "length": item.get("length", "없음"),
            "color": item.get("color", "없음"),
            "prints": prints_str,
            "neckline": item.get("neckline", "없음"),
            "fit": item.get("fit", "없음"),
        }

    o = _fmt_item(outer)
    t = _fmt_item(top)
    b = _fmt_item(bottom)

    prompt = f"""
너는 전문 패션 스타일리스트다.
아래 사용자의 체형과 옷의 속성을 바탕으로, 2~4줄 이내의 간결한 코디 설명을 작성하라.

[작성 규칙]
1) 첫 문장은 “사용자님은 ‘{user_shape}형’ 입니다.”로 시작한다.
2) 체형 일반 설명을 하지 말고, **현재 상의·하의·아우터 조합이 해당 체형을 어떻게 보완하거나 강조하는지 직접적으로 묘사한다.**
   - 예: “허리선을 더 날씬하게 보이게 한다”, “어깨 비율을 안정감 있게 만든다”, “하체 라인이 길어 보인다” 등.
3) 색상 대비, 소매/기장/핏 조합이 체형 연출에 미치는 시각적 효과를 1회 이상 자연스럽게 포함한다.
4) 마지막 문장은 “Tip:”으로 시작하며, 체형 보완에 도움이 되는 실용적인 착장 팁을 한 줄로 제시한다.
5) 체형 정의 설명, 불필요한 형용사, 과도한 미사여구는 금지하며, 옷 조합이 체형에 미치는 영향만 명확히 설명한다.

[입력 정보]
체형: {user_shape}

아우터:
- 중분류: {o["minor"]}
- 소매기장: {o["sleeve"]}
- 기장: {o["length"]}
- 색상: {o["color"]}
- 프린트: {o["prints"]}
- 넥라인: {o["neckline"]}
- 핏: {o["fit"]}

상의:
- 중분류: {t["minor"]}
- 소매기장: {t["sleeve"]}
- 기장: {t["length"]}
- 색상: {t["color"]}
- 프린트: {t["prints"]}
- 넥라인: {t["neckline"]}
- 핏: {t["fit"]}

하의:
- 중분류: {b["minor"]}
- 기장: {b["length"]}
- 색상: {b["color"]}
- 프린트: 없음
- 넥라인: 없음
- 핏: {b["fit"]}
"""
    return prompt.strip()

def generate_outfit_comment(user_shape: str,
                            outer: Optional[Dict[str, Any]],
                            top: Optional[Dict[str, Any]],
                            bottom: Optional[Dict[str, Any]]) -> str:
    """OpenAI API로 코디 설명 생성"""
    if not OPENAI_API_KEY:
        return "API 키 설정이 필요합니다."

    user_prompt = build_outfit_prompt(user_shape, outer, top, bottom)

    try:
        completion = client.chat.completions.create(
            model="gpt-4o-mini", # 또는 gpt-3.5-turbo / gpt-4
            messages=[
                {
                    "role": "system",
                    "content": "너는 사용자의 체형과 오늘의 코디를 설명해주는 전문 패션 스타일리스트다."
                },
                {"role": "user", "content": user_prompt},
            ],
            temperature=0.7,
            max_tokens=300,
        )
        return completion.choices[0].message.content.strip()
    except Exception as e:
        return f"코디 설명 생성 실패: {str(e)}"

# ============================================================
# Recommendation Generator
# ============================================================

def recommend_outfits(
    wardrobe: List[Dict[str, Any]],
    user_gender: str,
    user_shape: str,
    current_temp: Optional[float] = None,
    top_k: int = 5
) -> Dict[str, Any]:
    
    # 1. Load Resources
    shape_weight_table = load_shape_tag_weight_from_excel(user_gender)
    outer2top_map, top2bottom_map = load_color_maps()
    
    # 2. Temperature
    if current_temp is None:
        current_temp = get_today_avg_temp_from_api()
        
    anchor_season, band_label, seasonal_items = filter_by_temperature(wardrobe, current_temp)
    
    outers = [i for i in seasonal_items if coarse_category(i.get("major"), i.get("minor")) == "outer"]
    tops = [i for i in seasonal_items if coarse_category(i.get("major"), i.get("minor")) == "top"]
    bottoms = [i for i in seasonal_items if coarse_category(i.get("major"), i.get("minor")) == "bottom"]
    
    outfits = []
    
    # 3. Generate Combinations
    # Helper to create outfit dict
    def make_outfit(o, t, b, score):
        return {
            "outer": o, "top": t, "bottom": b, "score": round(score, 3)
        }

    # Strategy A: Top Anchor (No Outer) -> Summer or warm Spring/Fall
    if anchor_season == "summer" or (anchor_season in ["spring_fall", "late_fall_early_winter"] and not outers):
        force_no_short = (anchor_season != "summer") 
        
        for top in tops:
            if force_no_short and top.get("sleeve") == "반팔": continue
            t_color = top.get("color")
            if not t_color: continue
            
            valid_bottom_colors = get_candidates(t_color, top2bottom_map)
            candidate_bottoms = [b for b in bottoms if b.get("color") in valid_bottom_colors]
            
            for bot in candidate_bottoms:
                score = compute_outfit_score(top, bot, None, user_shape, shape_weight_table)
                outfits.append(make_outfit(None, top, bot, score))

    # Strategy B: Outer Anchor -> Winter or Cool seasons
    else:
        for outer in outers:
            o_color = outer.get("color")
            if not o_color: continue
            
            valid_top_colors = get_candidates(o_color, outer2top_map)
            candidate_tops = [t for t in tops if t.get("color") in valid_top_colors]
            
            for top in candidate_tops:
                t_color = top.get("color")
                if not t_color: continue
                
                valid_bottom_colors = get_candidates(t_color, top2bottom_map)
                candidate_bottoms = [b for b in bottoms if b.get("color") in valid_bottom_colors]
                
                for bot in candidate_bottoms:
                    score = compute_outfit_score(top, bot, outer, user_shape, shape_weight_table)
                    outfits.append(make_outfit(outer, top, bot, score))

    # 4. Sort and Return
    outfits.sort(key=lambda x: x["score"], reverse=True)
    final_results = outfits[:top_k]
    
    # 🔹 Top-3 (or less) 에 대해 코디 설명 생성
    for i in range(min(len(final_results), 3)):
        item = final_results[i]
        comment = generate_outfit_comment(
            user_shape=user_shape,
            outer=item["outer"],
            top=item["top"],
            bottom=item["bottom"]
        )
        item["comment"] = comment
    
    return {
        "date": str(pd.Timestamp.now().date()),
        "temperature": current_temp,
        "season_band": band_label,
        "user_shape": user_shape,
        "recommendations": final_results
    }