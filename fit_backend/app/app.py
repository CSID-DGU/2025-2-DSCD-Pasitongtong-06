import os
import io
import json
import uuid
import shutil
from typing import List, Dict, Any, Optional
from pathlib import Path

from fastapi import FastAPI, UploadFile, File, HTTPException, Header, Depends, Security, Form
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from supabase import create_client, Client

# ================================
# [중요] 파이프라인 파일 임포트
# ================================
try:
    from app.m_body_shape_pipeline import predict_male_body_shape
    from app.w_body_shape_pipeline import predict_female_body_shape
    from app.clothes_pipeline import process_image as process_clothes_image
    from app.recommendation_pipeline import recommend_outfits
except ImportError:
    # 로컬 테스트용
    from m_body_shape_pipeline import predict_male_body_shape
    from w_body_shape_pipeline import predict_female_body_shape
    from clothes_pipeline import process_image as process_clothes_image
    from recommendation_pipeline import recommend_outfits

app = FastAPI(title="FitForYou with Supabase", version="2.0.0")

# CORS 설정
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
SUPABASE_URL = os.getenv("SUPABASE_URL")
SUPABASE_KEY = os.getenv("SUPABASE_KEY")

if not SUPABASE_URL or not SUPABASE_KEY:
    print("Warning: SUPABASE env vars not found.")
    supabase = None
else:
    supabase: Client = create_client(SUPABASE_URL, SUPABASE_KEY)

# ================================
# 2. 인증 유틸리티
# ================================
security = HTTPBearer()

async def get_current_user_id(credentials: HTTPAuthorizationCredentials = Security(security)) -> str:
    token = credentials.credentials
    try:
        if supabase:
            user = supabase.auth.get_user(token)
            return user.user.id
        else:
            return "test_user_id"
    except Exception as e:
        raise HTTPException(status_code=401, detail=f"Invalid Token: {str(e)}")

# ================================
# 3. 데이터 모델
# ================================
class UserProfileCreate(BaseModel):
    gender: str 

class RecommendRequest(BaseModel):
    temperature: Optional[float] = None
    top_k: int = 3

# ================================
# 4. API 엔드포인트
# ================================

# 4-0. 초기 프로필(성별) 저장
@app.post("/users/profile")
async def create_user_profile(
    profile: UserProfileCreate,
    user_id: str = Depends(get_current_user_id)
):
    try:
        data = {
            "id": user_id,
            "gender": profile.gender,
            "body_shape": None, 
            "body_metrics": None
        }
        if supabase:
            res = supabase.table("profiles").upsert(data).execute()
            return JSONResponse(status_code=200, content={"ok": True, "data": res.data})
        return JSONResponse(status_code=200, content={"ok": True, "message": "Supabase unconnected"})
    except Exception as e:
        return JSONResponse(status_code=500, content={"ok": False, "error": str(e)})


# 4-1. 체형 분석 API
@app.post("/user/analyze-shape")
async def analyze_shape(
    gender: str = Form(...),
    file: UploadFile = File(...),
    user_id: str = Depends(get_current_user_id)
):
    temp_filename = f"temp_{user_id}_{uuid.uuid4()}.png"
    temp_path = os.path.join("/tmp", temp_filename) if os.path.exists("/tmp") else temp_filename
    
    try:
        with open(temp_path, "wb") as buffer:
            shutil.copyfileobj(file.file, buffer)
            
        result_shape = ""
        print(f"Analyzing for gender: {gender}, file: {temp_path}")

        if gender.lower() == "male":
            result_shape = predict_male_body_shape(temp_path)
        elif gender.lower() == "female":
            result_shape = predict_female_body_shape(temp_path)
        else:
            raise HTTPException(status_code=400, detail="Gender must be 'male' or 'female'")

        print(f"Analysis Result: {result_shape}")

        if supabase:
            data = {
                "id": user_id,
                "gender": gender.lower(),
                "body_shape": result_shape,
            }
            supabase.table("profiles").upsert(data).execute()

        return JSONResponse(status_code=200, content={
            "ok": True,
            "gender": gender,
            "body_shape": result_shape,
            "message": "Analysis successful"
        })

    except Exception as e:
        print(f"Error during analysis: {e}")
        return JSONResponse(status_code=500, content={"ok": False, "error": str(e)})
        
    finally:
        if os.path.exists(temp_path):
            os.remove(temp_path)


# =========================================================
# [핵심 수정] 4-2. 의류 등록 (다중 이미지 지원)
# =========================================================
@app.post("/clothes/upload")
async def upload_clothes(
    files: List[UploadFile] = File(...),  # [변경] List[UploadFile]로 여러 장 받음
    user_id: str = Depends(get_current_user_id)
):
    saved_items_total = []
    temp_paths = []  # 삭제할 임시 파일 경로들 보관

    try:
        # 업로드된 각 파일에 대해 반복 처리
        for file in files:
            # 1. 원본 이미지 임시 저장
            temp_filename = f"{uuid.uuid4()}_{file.filename}"
            temp_path = temp_filename
            
            # 파일 쓰기
            content = await file.read()
            with open(temp_path, "wb") as f:
                f.write(content)
            
            temp_paths.append(temp_path)  # 나중에 삭제하기 위해 목록에 추가

            # 2. 옷 분석 파이프라인 실행
            # clothes_pipeline.py는 단일 경로를 받아 처리하므로 루프 안에서 호출
            analyzed_items = process_clothes_image(temp_path) 

            if supabase:
                for item in analyzed_items:
                    # -----------------------------------------------------------
                    # 3. 처리된 이미지(배경 제거됨)를 Supabase Storage에 업로드
                    # -----------------------------------------------------------
                    local_file_path = item.get("crop_nobg_path")
                    
                    if not local_file_path or not os.path.exists(local_file_path):
                        print(f"[Warning] 파일이 존재하지 않음: {local_file_path}")
                        continue

                    file_ext = os.path.splitext(local_file_path)[1]
                    storage_path = f"{user_id}/{uuid.uuid4()}{file_ext}"

                    with open(local_file_path, "rb") as f_img:
                        file_bytes = f_img.read()

                    public_url = ""
                    try:
                        supabase.storage.from_("wardrobe_images").upload(
                            path=storage_path,
                            file=file_bytes,
                            file_options={"content-type": "image/png"}
                        )
                        public_url = supabase.storage.from_("wardrobe_images").get_public_url(storage_path)
                        
                    except Exception as upload_err:
                        print(f"[Error] Storage upload failed: {upload_err}")
                        continue

                    # -----------------------------------------------------------
                    # 4. DB에 메타데이터 저장
                    # -----------------------------------------------------------
                    db_data = {
                        "user_id": user_id,
                        "image_url": public_url,
                        "major_category": item.get("major"),
                        "minor_category": item.get("minor"),
                        "color": item.get("color"),
                        "fit": item.get("fit"),
                        "length": item.get("length"),
                        "neckline": item.get("neckline"),
                        "collar": item.get("collar"),
                        "prints": item.get("prints") 
                    }
                    
                    res = supabase.table("wardrobe").insert(db_data).execute()
                    if res.data:
                        saved_items_total.append(res.data[0])

                    # 파이프라인에서 생성한 개별 크롭 파일 삭제
                    crop_path = item.get("crop_path")
                    if crop_path and os.path.exists(crop_path):
                        os.remove(crop_path)
                    
                    # 배경제거 파일도 업로드 후 로컬에선 삭제 (용량 절약)
                    if local_file_path and os.path.exists(local_file_path):
                        os.remove(local_file_path)

        return JSONResponse(status_code=200, content={
            "ok": True,
            "saved_count": len(saved_items_total),
            "items": saved_items_total
        })

    except Exception as e:
        print(f"Error in upload_clothes: {e}")
        return JSONResponse(status_code=500, content={"ok": False, "error": str(e)})
        
    finally:
        # 5. 모든 원본 임시 파일 일괄 삭제
        for path in temp_paths:
            if os.path.exists(path):
                try:
                    os.remove(path)
                except Exception:
                    pass


# 4-3. 코디 추천
@app.post("/recommend/outfit")
async def recommend_endpoint(
    req: RecommendRequest, 
    user_id: str = Depends(get_current_user_id)
):
    try:
        if not supabase:
            return JSONResponse(status_code=503, content={"ok": False, "error": "DB unconnected"})

        # 1. 프로필 조회
        profile_res = supabase.table("profiles").select("*").eq("id", user_id).execute()
        if not profile_res.data:
            return JSONResponse(status_code=400, content={"ok": False, "error": "Profile not found"})
        
        profile = profile_res.data[0]
        
        # 2. 옷장 조회
        wardrobe_res = supabase.table("wardrobe").select("*").eq("user_id", user_id).execute()
        db_wardrobe = wardrobe_res.data
        
        # 3. 데이터 포맷팅
        formatted_wardrobe = []
        for item in db_wardrobe:
            formatted_wardrobe.append({
                "id": item.get("id"),
                "major": item["major_category"],
                "minor": item["minor_category"],
                "color": item["color"],
                "image_url": item["image_url"],
                "fit": item.get("fit"),
                "length": item.get("length"),
                "neckline": item.get("neckline"),
                "collar": item.get("collar"),
                "prints": item.get("prints")
            })

        # 4. 추천 로직
        result = recommend_outfits(
            wardrobe=formatted_wardrobe,
            user_gender=profile["gender"],
            user_shape=profile["body_shape"],
            current_temp=req.temperature,
            top_k=req.top_k
        )

        return JSONResponse(status_code=200, content={"ok": True, "data": result})

    except Exception as e:
        return JSONResponse(status_code=500, content={"ok": False, "error": str(e)})

# 4-4. 내 정보 조회
@app.get("/users/me")
async def get_my_profile(user_id: str = Depends(get_current_user_id)):
    try:
        if not supabase:
             return JSONResponse(status_code=200, content={"ok": True, "user_id": user_id, "mode": "offline"})

        profile_res = supabase.table("profiles").select("*").eq("id", user_id).execute()
        wardrobe_res = supabase.table("wardrobe").select("*", count="exact").eq("user_id", user_id).execute()
        
        if not profile_res.data:
            return JSONResponse(status_code=404, content={"ok": False, "error": "Profile not found"})

        profile_data = profile_res.data[0]

        return JSONResponse(status_code=200, content={
            "ok": True,
            "user_id": user_id,
            "gender": profile_data.get("gender"),
            "shape": profile_data.get("body_shape"),
            "wardrobe_count": wardrobe_res.count
        })

    except Exception as e:
        return JSONResponse(status_code=500, content={"ok": False, "error": str(e)})

# 4-5. 회원 탈퇴
@app.delete("/users/me")
async def delete_my_account(user_id: str = Depends(get_current_user_id)):
    try:
        if supabase:
            files_list = supabase.storage.from_("wardrobe_images").list(path=user_id)
            if files_list:
                files_to_remove = [f"{user_id}/{x['name']}" for x in files_list]
                supabase.storage.from_("wardrobe_images").remove(files_to_remove)
            
            supabase.table("profiles").delete().eq("id", user_id).execute()

        return JSONResponse(status_code=200, content={"ok": True, "message": "Account deleted"})

    except Exception as e:
        return JSONResponse(status_code=500, content={"ok": False, "error": str(e)})