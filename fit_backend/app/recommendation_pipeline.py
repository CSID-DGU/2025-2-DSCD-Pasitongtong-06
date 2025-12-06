# recommendation_pipeline.py
import json
import math
import requests
import os
from pathlib import Path
from typing import List, Dict, Any, Optional, Tuple, Set
from datetime import date
import pandas as pd
from openai import OpenAI

# ============================================================
# 0. 설정 및 상수
# ============================================================

BASE_DIR = Path(__file__).resolve().parent
DATA_DIR = BASE_DIR / "data"

# 데이터 파일 경로 (사용자가 업로드한 파일명에 맞춰 변경 필요)
OUTER2TOP_PATH = DATA_DIR / "outer2top_map.json"
TOP2BOTTOM_PATH = DATA_DIR / "top2bottom_map.json"
SCORING_FEMALE_PATH = DATA_DIR / "scoring_female.csv" 
SCORING_MALE_PATH = DATA_DIR / "scoring_male.csv"

# OpenAI API 설정
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
client = OpenAI(api_key=OPENAI_API_KEY) if OPENAI_API_KEY else None

# 날씨 API 설정
OPENWEATHER_API_KEY = os.getenv("OPENWEATHER_API_KEY") or "de27abae3b89192f4c8dd53b91402717" # Fallback key from notebook (recommend using env var)
DEFAULT_CITY = "Seoul"
DEFAULT_COUNTRY = "KR"

SIM_THRESHOLD = 0.90

# ============================================================
# 1. 카테고리 및 필터링 상수
# ============================================================

TOP_MINOR = {"탑", "블라우스", "티셔츠", "니트웨어", "셔츠", "브라탑", "후드티"}
BOTTOM_MINOR = {"청바지", "팬츠", "스커트", "레깅스", "조거팬츠"}
OUTER_MINOR = {"코트", "재킷", "점퍼", "패딩", "베스트", "가디건", "짚업"}
ONEPIECE_MINOR = {"드레스", "점프수트", "수영복"}

HOT_REMOVE_MINOR_TOP_OUTER = {"코트", "패딩", "니트웨어", "점퍼", "재킷", "가디건"}
MILD_REMOVE_MINOR_TOP_OUTER = {"코트", "패딩"}
ALWAYS_REMOVE_MINOR = {"수영복"}

SLEEVE_REMOVE_BY_BAND = {
    "한여름": set(),
    "봄/가을": {"민소매"},
    "늦가을/초겨울": {"민소매", "반팔", "캡"},
    "한겨울": {"민소매", "반팔", "캡", "7부소매"},
}

HOT_REMOVE_NECKLINE = {"터틀넥", "후드"}
MILD_REMOVE_NECKLINE = {"터틀넥", "홀터넥"}
COOL_COLD_REMOVE_NECKLINE = {"홀터넥"}

LENGTH_REMOVE_BY_BAND = {
    "한여름": set(),
    "봄/가을": {"크롭"},
    "늦가을/초겨울": {"미니", "크롭"},
    "한겨울": {"미니", "크롭"},
}

# ============================================================
# 2. Helper Functions
# ============================================================

def get_today_avg_temp_from_api(city: str = DEFAULT_CITY, country: str = DEFAULT_COUNTRY) -> float:
    try:
        url = f"https://api.openweathermap.org/data/2.5/weather?q={city},{country}&appid={OPENWEATHER_API_KEY}&units=metric"
        resp = requests.get(url, timeout=5)
        resp.raise_for_status()
        data = resp.json()
        temp_min = float(data["main"]["temp_min"])
        temp_max = float(data["main"]["temp_max"])
        return (temp_min + temp_max) / 2.0
    except Exception as e:
        print(f"[WARN] Weather API failed: {e}")
        return 15.0 # Fallback

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

def is_item_allowed_for_temperature(item: Dict[str, Any], temp_avg: float) -> bool:
    band = temp_to_band(temp_avg)
    minor = item.get("minor")
    major = item.get("major")
    cat = coarse_category(major, minor)
    sleeve = item.get("sleeve")
    neckline = item.get("neckline")
    length = item.get("length")

    if minor == "수영복": return False

    if cat in {"top", "outer"}:
        if band == "한여름" and minor in HOT_REMOVE_MINOR_TOP_OUTER: return False
        if band == "봄/가을" and minor in MILD_REMOVE_MINOR_TOP_OUTER: return False

    remove_sleeves = SLEEVE_REMOVE_BY_BAND.get(band, set())
    if sleeve and sleeve in remove_sleeves: return False

    if band == "한여름":
        if neckline in HOT_REMOVE_NECKLINE: return False
    elif band == "봄/가을":
        if neckline in MILD_REMOVE_NECKLINE: return False
    elif band in {"늦가을/초겨울", "한겨울"}:
        if neckline in COOL_COLD_REMOVE_NECKLINE: return False

    remove_lengths = LENGTH_REMOVE_BY_BAND.get(band, set())
    if length and length in remove_lengths: return False

    return True

def filter_by_temperature(wardrobe: List[Dict[str, Any]], temp_avg: float):
    band = temp_to_band(temp_avg)
    anchor_season = band_to_anchor_season(band)
    filtered = []
    for item in wardrobe:
        if is_item_allowed_for_temperature(item, temp_avg):
            filtered.append(item)
    return anchor_season, band, filtered

# ============================================================
# 3. Data Loading
# ============================================================

def load_color_maps():
    try:
        with open(OUTER2TOP_PATH, encoding="utf-8") as f:
            outer2top_map = json.load(f)
        with open(TOP2BOTTOM_PATH, encoding="utf-8") as f:
            top2bottom_map = json.load(f)
        return outer2top_map, top2bottom_map
    except Exception as e:
        print(f"[Error] Color map load failed: {e}")
        return {}, {}

def load_shape_tag_weight_B(gender: str) -> Dict[Tuple[str, str], Dict[str, float]]:
    """
    CSV 파일에서 체형별 가중치를 로드합니다.
    성별에 따라 다른 파일과 컬럼명을 사용합니다.
    """
    csv_path = SCORING_FEMALE_PATH if gender == "female" else SCORING_MALE_PATH
    
    if not csv_path.exists():
        print(f"[Error] Scoring file not found: {csv_path}")
        return {}

    try:
        # pd.read_excel 대신 pd.read_csv 사용 (업로드된 파일이 CSV임)
        df = pd.read_csv(csv_path)
        scoring = {}
        
        # 성별에 따른 체형 컬럼 매핑
        if gender == "female":
            shape_cols = ["모래시계", "삼각", "역삼각", "직사각"]
            col_map = {
                "모래시계": "모래시계_score",
                "삼각": "삼각_score",
                "역삼각": "역삼각_score",
                "직사각": "직사각_score"
            }
        else: # male
            shape_cols = ["작은 역삼각", "사각", "큰사각"]
            col_map = {
                "작은 역삼각": "작은역삼각_score",
                "사각": "사각_score",
                "큰사각": "큰사각_score"
            }

        for _, row in df.iterrows():
            # category, tag가 비어있지 않은지 확인
            if pd.isna(row.get("category")) or pd.isna(row.get("tag")):
                continue
                
            key = (str(row["category"]).strip(), str(row["tag"]).strip())
            
            scoring[key] = {}
            for shape in shape_cols:
                col_name = col_map.get(shape)
                val = row.get(col_name, 0)
                # 데이터 정제 (– 같은 특수문자 처리)
                if isinstance(val, str):
                    val = val.replace("–", "-")
                try:
                    scoring[key][shape] = float(val)
                except (ValueError, TypeError):
                    scoring[key][shape] = 0.0
                    
        return scoring
    except Exception as e:
        print(f"[Error] Scoring table load failed: {e}")
        return {}

# ============================================================
# 4. Scoring Logic
# ============================================================

def map_json_to_category_tags(item: Dict[str, Any]) -> List[Tuple[str, str]]:
    major = item.get("major")
    minor = item.get("minor")
    cat = coarse_category(major, minor)

    fit = item.get("fit")
    length = item.get("length")
    neckline = item.get("neckline")
    collar = item.get("collar")
    prints = item.get("prints") or []

    pairs = []

    if cat == "top":
        if fit: pairs.append(("fit_top", fit))
        if length: pairs.append(("length_top", length))
    elif cat == "bottom":
        if fit: pairs.append(("fit_bottom", fit))
        if length: pairs.append(("length_bottom", length))
    elif cat == "outer":
        if fit: pairs.append(("fit_top", fit)) # Outer fits uses fit_top logic in Excel? or separate?
        # Note: CSV has length_outer, but fit_top/fit_bottom.
        # Assuming outer fit uses fit_top row if available or fit_outer if exists.
        # Based on CSV snippet: fit_top exists, fit_bottom exists. No fit_outer.
        # Notebook code maps outer fit to "fit_top".
        if fit: pairs.append(("fit_top", fit)) 
        if length: pairs.append(("length_outer", length))
    elif cat == "onepiece":
        if length: pairs.append(("length_onepiece", length))

    if neckline: pairs.append(("neckline", neckline))
    if collar: pairs.append(("collar", collar))
    for p in prints: pairs.append(("prints", p))

    return pairs

def score_item_B(item: Dict[str, Any], shape: str, scoring_table: Dict) -> float:
    total = 0.0
    pairs = map_json_to_category_tags(item)
    for cat_tag in pairs:
        if cat_tag in scoring_table:
            # 해당 체형 점수가 있는지 확인
            shape_score = scoring_table[cat_tag].get(shape, 0.0)
            total += shape_score
    return total

def compute_outfit_score(
    top: Optional[Dict[str, Any]],
    bottom: Optional[Dict[str, Any]],
    outer: Optional[Dict[str, Any]],
    user_shape: str,
    scoring_table: Dict,
    color_bonus: float = 0.0
):
    base_score = 0.0
    if top: base_score += score_item_B(top, user_shape, scoring_table)
    if bottom: base_score += score_item_B(bottom, user_shape, scoring_table)
    if outer: base_score += score_item_B(outer, user_shape, scoring_table)

    confidence_bonus = 0.0
    if top: confidence_bonus += top.get("score", 0.0)
    if bottom: confidence_bonus += bottom.get("score", 0.0)
    if outer: confidence_bonus += outer.get("score", 0.0)
    confidence_bonus *= 0.1

    final_score = base_score + color_bonus + confidence_bonus
    return final_score, base_score, color_bonus, confidence_bonus

# ============================================================
# 5. Deduplication
# ============================================================

def _item_feature_tuple(item: Optional[Dict[str, Any]]) -> Tuple:
    if not item: return (None,) * 7
    prints = item.get("prints") or []
    prints_tuple = tuple(sorted(map(str, prints))) if isinstance(prints, list) else str(prints)
    return (
        item.get("minor"), item.get("color"), item.get("fit"),
        item.get("length"), item.get("sleeve"), prints_tuple, item.get("neckline")
    )

def deduplicate_outfits(outfits: List[Dict[str, Any]], sim_threshold: float = None) -> List[Dict[str, Any]]:
    filtered = []
    
    def outfit_similarity(o1, o2):
        parts = ["outer", "top", "bottom", "onepiece"]
        score, total = 0, 0
        for p in parts:
            i1, i2 = o1.get(p), o2.get(p)
            if not i1 and not i2:
                score += 1; total += 1; continue
            if (i1 and not i2) or (not i1 and i2):
                total += 1; continue
            f1, f2 = _item_feature_tuple(i1), _item_feature_tuple(i2)
            same = sum(1 for a, b in zip(f1, f2) if a == b)
            score += same
            total += len(f1)
        return score / total if total > 0 else 0

    for o in outfits:
        if sim_threshold:
            is_dup = False
            for kept in filtered:
                if outfit_similarity(o, kept) >= sim_threshold:
                    is_dup = True
                    break
            if is_dup: continue
        filtered.append(o)
    return filtered

# ============================================================
# 6. Outfit Generators
# ============================================================

def get_top_candidates(outer_color, map_data):
    rows = map_data.get(outer_color, [])
    rows = sorted(rows, key=lambda x: x["prob"], reverse=True)
    return [(r["top_color"], r["prob"]) for r in rows]

def get_bottom_candidates(top_color, map_data):
    rows = map_data.get(top_color, [])
    rows = sorted(rows, key=lambda x: x["prob"], reverse=True)
    return [(r["bottom_color"], r["prob"]) for r in rows]

def _gen_top_anchor_outfits(tops, bottoms, user_shape, scoring_table, top2bottom_map, force_outer_check=False):
    outfits = []
    for top in tops:
        if force_outer_check and top.get("sleeve") == "반팔": continue
        top_color = top.get("color")
        if not top_color: continue

        candidates = get_bottom_candidates(top_color, top2bottom_map)
        if not candidates: continue
        
        cand_colors = {c: p for c, p in candidates}
        valid_bottoms = [b for b in bottoms if b.get("color") in cand_colors]

        for bot in valid_bottoms:
            color_bonus = cand_colors.get(bot.get("color"), 0.0)
            score, base, cb, conf = compute_outfit_score(top, bot, None, user_shape, scoring_table, color_bonus)
            outfits.append({
                "outer": None, "top": top, "bottom": bot, "onepiece": None,
                "score": score, "base_score": base, "color_bonus": cb, "confidence_bonus": conf
            })
    return outfits

def _gen_outer_anchor_outfits(outers, tops, bottoms, user_shape, scoring_table, outer2top_map, top2bottom_map):
    outfits = []
    for outer in outers:
        o_color = outer.get("color")
        if not o_color: continue
        
        top_cands = get_top_candidates(o_color, outer2top_map)
        if not top_cands: continue
        
        top_probs = {c: p for c, p in top_cands}
        valid_tops = [t for t in tops if t.get("color") in top_probs]

        for top in valid_tops:
            t_color = top.get("color")
            bot_cands = get_bottom_candidates(t_color, top2bottom_map)
            if not bot_cands: continue
            
            bot_probs = {c: p for c, p in bot_cands}
            valid_bottoms = [b for b in bottoms if b.get("color") in bot_probs]

            for bot in valid_bottoms:
                prob_ot = top_probs.get(t_color, 0.0)
                prob_tb = bot_probs.get(bot.get("color"), 0.0)
                color_bonus = prob_ot + prob_tb
                
                score, base, cb, conf = compute_outfit_score(top, bot, outer, user_shape, scoring_table, color_bonus)
                outfits.append({
                    "outer": outer, "top": top, "bottom": bot, "onepiece": None,
                    "score": score, "base_score": base, "color_bonus": cb, "confidence_bonus": conf
                })
    return outfits

def _gen_onepiece_only_outfits(onepieces, user_shape, scoring_table):
    outfits = []
    for op in onepieces:
        base = score_item_B(op, user_shape, scoring_table)
        outfits.append({
            "outer": None, "top": None, "bottom": None, "onepiece": op,
            "score": base, "base_score": base, "color_bonus": 0.0, "confidence_bonus": 0.0
        })
    return outfits

def _gen_outer_onepiece_outfits(outers, onepieces, user_shape, scoring_table, outer2top_map):
    outfits = []
    for outer in outers:
        o_color = outer.get("color")
        if not o_color: continue
        
        # 원피스 색상 매칭도 outer -> top 맵을 재활용
        cands = get_top_candidates(o_color, outer2top_map)
        probs = {c: p for c, p in cands}
        
        valid_ops = [op for op in onepieces if op.get("color") in probs]
        
        for op in valid_ops:
            base = score_item_B(outer, user_shape, scoring_table) + score_item_B(op, user_shape, scoring_table)
            color_bonus = probs.get(op.get("color"), 0.0)
            score = base + color_bonus
            outfits.append({
                "outer": outer, "top": None, "bottom": None, "onepiece": op,
                "score": score, "base_score": base, "color_bonus": color_bonus, "confidence_bonus": 0.0
            })
    return outfits

# ============================================================
# 7. LLM Prompt Generation
# ============================================================

def build_outfit_prompt(user_shape: str, outer, top, bottom, onepiece) -> str:
    def _fmt(item):
        if not item: return {k: "없음" for k in ["minor", "sleeve", "length", "color", "prints", "neckline", "fit"]}
        prints = item.get("prints") or []
        p_str = ", ".join(map(str, prints)) if prints else "민무늬"
        return {
            "minor": item.get("minor", "없음"),
            "sleeve": item.get("sleeve", "없음"),
            "length": item.get("length", "없음"),
            "color": item.get("color", "없음"),
            "prints": p_str,
            "neckline": item.get("neckline", "없음"),
            "fit": item.get("fit", "없음")
        }

    o, t, b, op = _fmt(outer), _fmt(top), _fmt(bottom), _fmt(onepiece)

    if onepiece:
        return f"""
너는 전문 패션 스타일리스트다.
아래 사용자의 체형과 옷의 속성을 바탕으로, 2~4줄 이내의 간결한 코디 설명을 작성하라.

[작성 규칙]
1) 첫 문장은 “사용자님은 ‘{user_shape}형’ 입니다.”로 시작한다.
2) 체형 일반 설명을 하지 말고, 현재 원피스(또는 점프수트){'와 아우터' if outer else ''} 조합이 해당 체형을 어떻게 보완하거나 강조하는지 직접적으로 묘사한다.
3) 색상 대비, 소매/기장/핏 조합이 체형 연출에 미치는 시각적 효과를 1회 이상 자연스럽게 포함한다.
4) 마지막 문장은 반드시 “Tip:”으로 시작하며, **아래 중 ‘현재 코디와 어울리는 하나만 적절히 선택해’ 체형을 보완하는 팁을 한 줄로 제시한다.**
   - 선택지: (가방, 신발, 목걸이, 귀걸이, 스카프, 헤어악세서리, 팔찌, 시계)
5) 미사여구는 금지하며, 옷 조합이 체형에 미치는 영향만 명확히 설명한다.

[입력 정보]
체형: {user_shape}
아우터: {o["color"]} {o["minor"]} (핏:{o["fit"]}, 기장:{o["length"]})
원피스: {op["color"]} {op["minor"]} (핏:{op["fit"]}, 넥라인:{op["neckline"]})
""".strip()

    else:
        return f"""
너는 전문 패션 스타일리스트다.
아래 사용자의 체형과 옷의 속성을 바탕으로, 2~4줄 이내의 간결한 코디 설명을 작성하라.

[작성 규칙]
1) 첫 문장은 “사용자님은 ‘{user_shape}형’ 입니다.”로 시작한다.
2) 체형 일반 설명을 하지 말고, **현재 상의·하의·아우터 조합이 해당 체형을 어떻게 보완하거나 강조하는지 직접적으로 묘사한다.**
3) 색상 대비, 소매/기장/핏 조합이 체형 연출에 미치는 시각적 효과를 1회 이상 자연스럽게 포함한다.
4) 마지막 문장은 반드시 “Tip:”으로 시작하며, **아래 중 ‘현재 코디와 어울리는 하나만 적절히 선택해’ 체형을 보완하는 팁을 한 줄로 제시한다.**
   - 선택지: (가방, 신발, 목걸이, 귀걸이, 스카프, 헤어악세서리, 팔찌, 시계)
5) 미사여구는 금지하며, 옷 조합이 체형에 미치는 영향만 명확히 설명한다.

[입력 정보]
체형: {user_shape}
아우터: {o["color"]} {o["minor"]} (핏:{o["fit"]}, 기장:{o["length"]})
상의: {t["color"]} {t["minor"]} (핏:{t["fit"]}, 넥라인:{t["neckline"]})
하의: {b["color"]} {b["minor"]} (핏:{b["fit"]}, 기장:{b["length"]})
""".strip()

def generate_outfit_comment(user_shape: str, outer, top, bottom, onepiece) -> str:
    if not client:
        return "API 키가 설정되지 않아 설명을 생성할 수 없습니다."
    
    prompt = build_outfit_prompt(user_shape, outer, top, bottom, onepiece)
    try:
        completion = client.chat.completions.create(
            model="gpt-4o-mini", # or gpt-4
            messages=[
                {"role": "system", "content": "너는 사용자의 체형과 오늘의 코디를 설명해주는 전문 패션 스타일리스트다."},
                {"role": "user", "content": prompt}
            ],
            temperature=0.7,
            max_tokens=300
        )
        return completion.choices[0].message.content.strip()
    except Exception as e:
        print(f"[Error] LLM generation failed: {e}")
        return f"오류 발생: {str(e)}"  # 👈 앱 화면에서 구체적인 에러를 볼 수 있게 변경

# ============================================================
# 8. Main Entry Point (for App)
# ============================================================

def recommend_outfits(
    wardrobe: List[Dict[str, Any]],
    user_gender: str,
    user_shape: str,
    current_temp: Optional[float] = None,
    top_k: int = 5
) -> Dict[str, Any]:
    
    # 1. Load Resources
    outer2top_map, top2bottom_map = load_color_maps()
    scoring_table = load_shape_tag_weight_B(user_gender)
    
    # 2. Temperature
    if current_temp is None:
        current_temp = get_today_avg_temp_from_api()
        
    anchor_season, band_label, seasonal_items = filter_by_temperature(wardrobe, current_temp)
    
    # 3. Categorize
    outers = [i for i in seasonal_items if coarse_category(i.get("major"), i.get("minor")) == "outer"]
    tops = [i for i in seasonal_items if coarse_category(i.get("major"), i.get("minor")) == "top"]
    bottoms = [i for i in seasonal_items if coarse_category(i.get("major"), i.get("minor")) == "bottom"]
    onepieces = [i for i in seasonal_items if coarse_category(i.get("major"), i.get("minor")) == "onepiece"]
    
    outfits = []
    
    # 4. Generate Strategy
    if anchor_season == "summer":
        outfits += _gen_top_anchor_outfits(tops, bottoms, user_shape, scoring_table, top2bottom_map)
        outfits += _gen_onepiece_only_outfits(onepieces, user_shape, scoring_table)
        
    elif anchor_season == "spring_fall":
        outfits += _gen_top_anchor_outfits(tops, bottoms, user_shape, scoring_table, top2bottom_map, force_outer_check=True)
        outfits += _gen_outer_anchor_outfits(outers, tops, bottoms, user_shape, scoring_table, outer2top_map, top2bottom_map)
        outfits += _gen_onepiece_only_outfits(onepieces, user_shape, scoring_table)
        outfits += _gen_outer_onepiece_outfits(outers, onepieces, user_shape, scoring_table, outer2top_map)
        
    elif anchor_season == "late_fall_early_winter":
        outfits += _gen_top_anchor_outfits(tops, bottoms, user_shape, scoring_table, top2bottom_map, force_outer_check=True)
        outfits += _gen_outer_anchor_outfits(outers, tops, bottoms, user_shape, scoring_table, outer2top_map, top2bottom_map)
        outfits += _gen_outer_onepiece_outfits(outers, onepieces, user_shape, scoring_table, outer2top_map)
        
    elif anchor_season == "winter":
        outfits += _gen_outer_anchor_outfits(outers, tops, bottoms, user_shape, scoring_table, outer2top_map, top2bottom_map)
        outfits += _gen_outer_onepiece_outfits(outers, onepieces, user_shape, scoring_table, outer2top_map)
        
    # 5. Sort & Deduplicate
    outfits.sort(key=lambda x: x["score"], reverse=True)
    outfits = deduplicate_outfits(outfits, sim_threshold=SIM_THRESHOLD)
    
    final_results = outfits[:top_k]
    
    # 6. Generate Comments
    for item in final_results:
        item["comment"] = generate_outfit_comment(
            user_shape, item["outer"], item["top"], item["bottom"], item["onepiece"]
        )
        # Round scores for clean output
        item["score"] = round(item["score"], 3)
        
    return {
        "date": str(date.today()),
        "temperature": current_temp,
        "season_band": band_label,
        "user_shape": user_shape,
        "recommendations": final_results
    }