import os
import io
import json
import uuid
from typing import List, Dict, Any, Optional
from pathlib import Path

from fastapi import FastAPI, UploadFile, File, HTTPException, Header, Depends, Security
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from supabase import create_client, Client

# 파이프라인 임포트
from m_body_shape_pipeline import classify_image as classify_male
from w_body_shape_pipeline import classify_image as classify_female
from clothes_pipeline import process_image as process_clothes_image
from recommendation_pipeline import recommend_outfits

app = FastAPI(title="FitForYou with Supabase", version="2.0.0")

# CORS 설정 (유지)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ================================
# 1. Supabase 클라이언트 설정
# ================================
# 실제 운영 시 환경 변수로 관리하세요.
SUPABASE_URL = os.getenv("SUPABASE_URL")
SUPABASE_KEY = os.getenv("SUPABASE_KEY")

if not SUPABASE_URL or not SUPABASE_KEY:
    raise RuntimeError("SUPABASE_URL과 SUPABASE_KEY 환경 변수가 설정되지 않았습니다.")

# 서비스 롤 키를 쓰면 RLS 우회 가능, Anon 키를 쓰면 토큰 필요. 
# 여기서는 편의상 JWT 검증 로직을 약식으로 처리하기 위해 클라이언트 초기화.
supabase: Client = create_client(SUPABASE_URL, SUPABASE_KEY)

# ================================
# 2. 유틸리티 함수
# ================================

# ✅ 1. 보안 스키마 정의 (Swagger UI용)
security = HTTPBearer()

# ✅ 2. 토큰 추출 및 검증 함수
async def get_current_user_id(credentials: HTTPAuthorizationCredentials = Security(security)) -> str:
    """
    Swagger UI의 Authorize 버튼이나 헤더의 Authorization: Bearer <token> 에서
    토큰을 자동으로 추출하고 검증합니다.
    """
    token = credentials.credentials  # Bearer 부분을 뺀 순수 토큰
    
    try:
        # Supabase를 통해 토큰 유효성 검증 및 유저 정보 조회
        user = supabase.auth.get_user(token)
        return user.user.id
    except Exception as e:
        raise HTTPException(status_code=401, detail=f"Invalid Token: {str(e)}")

# ================================
# [추가] 데이터 모델 정의
# ================================
class UserProfileCreate(BaseModel):
    gender: str  # 'male' or 'female'

class RecommendRequest(BaseModel):
    temperature: Optional[float] = None # 없으면 자동 조회
    top_k: int = 3

# ================================
# 3. API 엔드포인트
# ================================

# [추가] 3-0. 초기 프로필(성별) 저장
@app.post("/users/profile")
async def create_user_profile(
    profile: UserProfileCreate,
    user_id: str = Depends(get_current_user_id)
):
    """
    프론트엔드에서 회원가입/로그인 직후 성별을 선택했을 때 호출합니다.
    Supabase profiles 테이블에 user_id와 gender를 저장(Upsert)합니다.
    """
    try:
        # DB에 저장할 데이터 준비
        data = {
            "id": user_id,
            "gender": profile.gender,
            # 초기 생성 시 다른 필드는 None 또는 기본값으로 설정
            "body_shape": None, 
            "body_metrics": None
        }
        
        # profiles 테이블에 upsert (이미 있으면 업데이트, 없으면 생성)
        res = supabase.table("profiles").upsert(data).execute()
        
        return JSONResponse(status_code=200, content={
            "ok": True,
            "message": "Profile created successfully",
            "data": res.data[0] if res.data else {}
        })

    except Exception as e:
        return JSONResponse(status_code=500, content={"ok": False, "error": str(e)})


# 3-1. 체형 분석 및 프로필 저장
@app.post("/user/analyze-shape")
async def analyze_shape(
    gender: str, 
    file: UploadFile = File(...), 
    user_id: str = Depends(get_current_user_id)
):
    """
    이미지 분석 -> 결과(체형, 수치) -> Supabase 'profiles' 테이블에 저장 (UPSERT)
    """
    image_bytes = await file.read()
    
    # 1. 분석 수행
    if gender.lower() == 'male':
        result = classify_male(image_bytes, return_overlay=False)
    else:
        result = classify_female(image_bytes, return_overlay=False)
        
    if not result.get("ok"):
        return JSONResponse(status_code=400, content=result)

    # 2. 데이터 준비
    shape_label = result.get("label")  # 예: "역삼각형"
    metrics = result.get("measurements") # 수치 데이터

    # 3. Supabase DB 저장 (profiles 테이블)
    data = {
        "id": user_id,
        "gender": gender.lower(),
        "body_shape": shape_label,
        "body_metrics": metrics
    }
    
    try:
        # upsert: 있으면 업데이트, 없으면 생성
        supabase.table("profiles").upsert(data).execute()
    except Exception as e:
        return JSONResponse(status_code=500, content={"ok": False, "error": f"DB Save Error: {str(e)}"})

    return JSONResponse(status_code=200, content={
        "ok": True, 
        "shape": shape_label, 
        "message": "Body shape analyzed and saved to profile."
    })


# 3-2. 의류 등록 (분석 -> 누끼 저장 -> DB 저장)
@app.post("/clothes/upload")
async def upload_clothes(
    file: UploadFile = File(...), 
    user_id: str = Depends(get_current_user_id)
):
    """
    1. YOLO + Attr 분석
    2. 누끼 이미지(PNG)를 Supabase Storage에 업로드
    3. 메타데이터를 Supabase DB 'wardrobe' 테이블에 저장
    """
    # 1. 임시 파일 저장 (process_clothes_image가 경로를 받으므로)
    temp_filename = f"{uuid.uuid4()}_{file.filename}"
    temp_path = f"/tmp/{temp_filename}"
    with open(temp_path, "wb") as f:
        f.write(await file.read())

    try:
        # 2. 파이프라인 실행 (로컬에 crop/nobg 생성됨)
        # process_image가 리스트를 반환함 (한 사진에 여러 옷이 있을 수 있음)
        analyzed_items = process_clothes_image(temp_path) 
        
        saved_items = []

        for item in analyzed_items:
            # 3. 누끼 이미지(nobg)를 Supabase Storage에 업로드
            nobg_local_path = item["crop_nobg_path"] # 로컬 경로
            file_name = os.path.basename(nobg_local_path)
            storage_path = f"{user_id}/{file_name}" # user_id/파일명 구조로 저장

            with open(nobg_local_path, "rb") as f:
                file_bytes = f.read()
            
            # 스토리지 업로드
            supabase.storage.from_("wardrobe_images").upload(
                path=storage_path,
                file=file_bytes,
                file_options={"content-type": "image/png"}
            )
            
            # 4. Public URL 생성
            public_url = supabase.storage.from_("wardrobe_images").get_public_url(storage_path)

            # 5. DB에 메타데이터 저장
            db_data = {
                "user_id": user_id,
                "image_url": public_url,
                "major_category": item["major"],
                "minor_category": item["minor"],
                "color": item["color"],
                "attributes": {
                    "fit": item.get("fit"),
                    "length": item.get("length"),
                    "sleeve": item.get("sleeve"),
                    "neckline": item.get("neckline"),
                    "prints": item.get("prints")
                }
            }
            res = supabase.table("wardrobe").insert(db_data).execute()
            saved_items.append(res.data[0])

            # 로컬 임시 크롭 파일 삭제 (선택)
            if os.path.exists(nobg_local_path): os.remove(nobg_local_path)
            if os.path.exists(item["crop_path"]): os.remove(item["crop_path"])

        return JSONResponse(status_code=200, content={
            "ok": True,
            "saved_count": len(saved_items),
            "items": saved_items
        })

    except Exception as e:
        import traceback
        traceback.print_exc()
        return JSONResponse(status_code=500, content={"ok": False, "error": str(e)})
    finally:
        if os.path.exists(temp_path):
            os.remove(temp_path)


# 3-3. 코디 추천 (DB 조회 -> 추천)
@app.post("/recommend/outfit")
async def recommend_endpoint(
    req: RecommendRequest, 
    user_id: str = Depends(get_current_user_id)
):
    """
    1. Supabase DB에서 user profile(체형) 조회
    2. Supabase DB에서 user wardrobe(옷) 조회
    3. 추천 알고리즘 실행
    """
    try:
        # 1. 프로필 조회
        profile_res = supabase.table("profiles").select("*").eq("id", user_id).execute()
        if not profile_res.data:
            return JSONResponse(status_code=400, content={"ok": False, "error": "Profile not found. Analyze body shape first."})
        
        profile = profile_res.data[0]
        user_gender = profile["gender"] # 'male' or 'female'
        user_shape = profile["body_shape"] # '역삼각' 등

        # 2. 옷장 조회
        wardrobe_res = supabase.table("wardrobe").select("*").eq("user_id", user_id).execute()
        db_wardrobe = wardrobe_res.data
        
        if not db_wardrobe:
            return JSONResponse(status_code=400, content={"ok": False, "error": "Wardrobe is empty. Upload clothes first."})

        # 3. 파이프라인 포맷으로 변환 (DB 컬럼 -> 파이프라인 Dict 키)
        formatted_wardrobe = []
        for item in db_wardrobe:
            attrs = item.get("attributes", {}) or {}
            formatted_wardrobe.append({
                "major": item["major_category"],
                "minor": item["minor_category"],
                "color": item["color"],
                "crop_path": item["image_url"], # 누끼 이미지 URL 사용
                # 속성들 풀기
                "fit": attrs.get("fit"),
                "length": attrs.get("length"),
                "sleeve": attrs.get("sleeve"),
                "neckline": attrs.get("neckline"),
                "prints": attrs.get("prints", [])
            })

        # 4. 추천 실행
        result = recommend_outfits(
            wardrobe=formatted_wardrobe,
            user_gender=user_gender,
            user_shape=user_shape,
            current_temp=req.temperature,
            top_k=req.top_k
        )

        return JSONResponse(status_code=200, content={"ok": True, "data": result})

    except Exception as e:
        return JSONResponse(status_code=500, content={"ok": False, "error": str(e)})

# ================================
# 4. 사용자 관리 엔드포인트 (추가됨)
# ================================

# 4-1. 내 정보 조회 (체형 정보 확인용)
@app.get("/users/me")
async def get_my_profile(user_id: str = Depends(get_current_user_id)):
    """
    내 프로필 정보(성별, 체형, 측정치)와 옷장 아이템 개수를 조회합니다.
    """
    try:
        # 1. 프로필 조회
        profile_res = supabase.table("profiles").select("*").eq("id", user_id).execute()
        
        # 2. 옷장 아이템 개수 조회 (count만)
        wardrobe_res = supabase.table("wardrobe").select("*", count="exact").eq("user_id", user_id).execute()
        
        profile_data = profile_res.data[0] if profile_res.data else None
        
        if not profile_data:
            # ✅ 핵심 로직: 프로필이 없으면 404 리턴 -> 프론트엔드가 이를 감지하여 성별 입력 화면으로 이동
            return JSONResponse(status_code=404, content={"ok": False, "error": "프로필이 존재하지 않습니다."})

        return JSONResponse(status_code=200, content={
            "ok": True,
            "user_id": user_id,
            "gender": profile_data.get("gender"),
            "shape": profile_data.get("body_shape"),
            "metrics": profile_data.get("body_metrics"),
            "wardrobe_count": wardrobe_res.count,
            "joined_at": profile_data.get("updated_at")
        })

    except Exception as e:
        return JSONResponse(status_code=500, content={"ok": False, "error": str(e)})


# 4-2. 회원 탈퇴 (데이터 삭제)
@app.delete("/users/me")
async def delete_my_account(user_id: str = Depends(get_current_user_id)):
    """
    [주의] 사용자의 모든 데이터(프로필, 옷장 DB, 스토리지 파일)를 삭제합니다.
    * 실제 Auth 계정 삭제는 Supabase Service Role Key가 필요하므로, 
      여기서는 '데이터'만 삭제하고 로그아웃 처리는 프론트에서 해야 합니다.
    """
    try:
        # 1. 스토리지 파일 삭제
        files_list = supabase.storage.from_("wardrobe_images").list(path=user_id)
        
        if files_list:
            files_to_remove = [f"{user_id}/{x['name']}" for x in files_list]
            supabase.storage.from_("wardrobe_images").remove(files_to_remove)

        # 2. DB 데이터 삭제
        supabase.table("profiles").delete().eq("id", user_id).execute()

        return JSONResponse(status_code=200, content={
            "ok": True, 
            "message": "모든 사용자 데이터가 삭제되었습니다."
        })

    except Exception as e:
        return JSONResponse(status_code=500, content={"ok": False, "error": str(e)})