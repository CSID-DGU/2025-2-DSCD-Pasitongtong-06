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
# m_body_shape_pipeline.py와 w_body_shape_pipeline.py 파일이 
# app 폴더 안에 있어야 하며, 각 파일 내부에 해당 함수가 정의되어 있어야 합니다.
try:
    from app.m_body_shape_pipeline import predict_male_body_shape
    from app.w_body_shape_pipeline import predict_female_body_shape
    # 옷 관련 파이프라인 (기존 유지)
    from app.clothes_pipeline import process_image as process_clothes_image
    from app.recommendation_pipeline import recommend_outfits
except ImportError:
    # 로컬 테스트용 (app 패키지 없이 실행 시)
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
    # 배포 환경이 아닐 때 에러 방지를 위한 더미 처리 (필요시 삭제)
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
        # supabase가 연결되어 있다면 검증
        if supabase:
            user = supabase.auth.get_user(token)
            return user.user.id
        else:
            # 테스트용: 토큰을 그대로 ID로 사용 (배포 시 제거)
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


# =========================================================
# [핵심 수정] 4-1. 체형 분석 API (성별 분기 처리)
# =========================================================
@app.post("/user/analyze-shape")
async def analyze_shape(
    gender: str = Form(...),       # 프론트에서 multipart/form-data로 보낸 문자열
    file: UploadFile = File(...),  # 프론트에서 보낸 이미지 파일
    user_id: str = Depends(get_current_user_id)
):
    """
    1. 이미지를 임시 파일로 저장
    2. gender 값(male/female)에 따라 적절한 모델 호출
    3. 결과를 DB에 저장하고 반환
    """
    # 1. 임시 파일 저장 경로 생성
    temp_filename = f"temp_{user_id}_{uuid.uuid4()}.png"
    temp_path = os.path.join("/tmp", temp_filename) if os.path.exists("/tmp") else temp_filename
    
    try:
        # 파일 저장
        with open(temp_path, "wb") as buffer:
            shutil.copyfileobj(file.file, buffer)
            
        result_shape = ""
        
        print(f"Analyzing for gender: {gender}, file: {temp_path}")

        # 2. 성별에 따른 모델 분기 실행
        if gender.lower() == "male":
            # [남자 모델 실행] m_body_shape_pipeline.py의 함수 호출
            # 함수 이름이 다르면 여기를 수정하세요 (예: classify_male 등)
            result_shape = predict_male_body_shape(temp_path)
            
        elif gender.lower() == "female":
            # [여자 모델 실행] w_body_shape_pipeline.py의 함수 호출
            result_shape = predict_female_body_shape(temp_path)
            
        else:
            raise HTTPException(status_code=400, detail="Gender must be 'male' or 'female'")

        print(f"Analysis Result: {result_shape}")

        # 3. Supabase DB 업데이트
        if supabase:
            data = {
                "id": user_id,
                "gender": gender.lower(),
                "body_shape": result_shape,
                # "body_metrics": ... # 필요시 추가
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
        return JSONResponse(status_code=500, content={
            "ok": False, 
            "error": str(e)
        })
        
    finally:
        # 4. 임시 파일 삭제 (서버 용량 확보)
        if os.path.exists(temp_path):
            os.remove(temp_path)


# 4-2. 의류 등록
@app.post("/clothes/upload")
async def upload_clothes(
    file: UploadFile = File(...), 
    user_id: str = Depends(get_current_user_id)
):
    # 1. 원본 이미지 임시 저장
    temp_filename = f"{uuid.uuid4()}_{file.filename}"
    temp_path = temp_filename 

    with open(temp_path, "wb") as f:
        f.write(await file.read())

    try:
        # 2. 옷 분석 파이프라인 실행
        analyzed_items = process_clothes_image(temp_path) 
        saved_items = []

        if supabase:
            for item in analyzed_items:
                # -----------------------------------------------------------
                # [추가] 1. 처리된 이미지(배경 제거됨)를 Supabase Storage에 업로드
                # -----------------------------------------------------------
                # clothes_pipeline.py에서 배경 제거된 파일 경로를 'crop_nobg_path'로 반환합니다.
                local_file_path = item.get("crop_nobg_path")
                
                # 만약 배경 제거된 파일이 없다면 원본('crop_path')을 사용하거나 에러 처리
                if not local_file_path or not os.path.exists(local_file_path):
                    print(f"[Warning] 파일이 존재하지 않음: {local_file_path}")
                    continue

                # 스토리지에 저장할 파일명 생성 (중복 방지 UUID 사용)
                # 예: user_id/random_uuid.png 구조로 저장
                file_ext = os.path.splitext(local_file_path)[1] # .png 등
                storage_path = f"{user_id}/{uuid.uuid4()}{file_ext}"

                with open(local_file_path, "rb") as f:
                    file_bytes = f.read()

                # 'wardrobe_images'라는 버킷이 Supabase에 미리 생성되어 있어야 합니다.
                # 버킷 설정이 'Public'이어야 get_public_url이 작동합니다.
                try:
                    supabase.storage.from_("wardrobe_images").upload(
                        path=storage_path,
                        file=file_bytes,
                        file_options={"content-type": "image/png"}
                    )
                    
                    # 2. 업로드된 파일의 Public URL 가져오기
                    public_url = supabase.storage.from_("wardrobe_images").get_public_url(storage_path)
                    
                except Exception as upload_err:
                    print(f"[Error] Storage upload failed: {upload_err}")
                    continue

                # ==========================================================
                
                # DB에 저장할 데이터 구성
                db_data = {
                    "user_id": user_id,
                    "image_url": public_url,  # 이제 public_url이 정의되어 정상 작동합니다
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
                saved_items.append(res.data[0] if res.data else {})

                # 개별 크롭 이미지 삭제 (선택 사항)
                if crop_path and os.path.exists(crop_path):
                    os.remove(crop_path)

        return JSONResponse(status_code=200, content={
            "ok": True,
            "saved_count": len(saved_items),
            "items": saved_items
        })

    except Exception as e:
        # 에러 로그 출력
        print(f"Error in upload_clothes: {e}")
        return JSONResponse(status_code=500, content={"ok": False, "error": str(e)})
        
    finally:
        # 원본 임시 파일 삭제
        if os.path.exists(temp_path):
            os.remove(temp_path)

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
        
        # 2. 옷장 조회 (상세 속성 포함)
        wardrobe_res = supabase.table("wardrobe").select("*").eq("user_id", user_id).execute()
        db_wardrobe = wardrobe_res.data
        
        # 3. 데이터 포맷팅 (DB -> Pipeline 입력)
        # [수정 포인트] 프론트엔드에서 사용하기 편하도록 'crop_path' 대신 'image_url'로 키 이름 통일
        formatted_wardrobe = []
        for item in db_wardrobe:
            formatted_wardrobe.append({
                "id": item.get("id"),       # (선택) 옷 ID도 같이 주면 프론트에서 관리하기 편함
                "major": item["major_category"],
                "minor": item["minor_category"],
                "color": item["color"],
                "image_url": item["image_url"], # ★ 수정됨: 배경 제거된 이미지 URL (기존 crop_path)
                
                # 상세 속성 (추천 알고리즘용)
                "fit": item.get("fit"),
                "length": item.get("length"),
                "neckline": item.get("neckline"),
                "collar": item.get("collar"),
                "prints": item.get("prints")
            })

        # 4. 추천 로직 실행
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
            # 파일 삭제
            files_list = supabase.storage.from_("wardrobe_images").list(path=user_id)
            if files_list:
                files_to_remove = [f"{user_id}/{x['name']}" for x in files_list]
                supabase.storage.from_("wardrobe_images").remove(files_to_remove)
            
            # DB 삭제
            supabase.table("profiles").delete().eq("id", user_id).execute()

        return JSONResponse(status_code=200, content={"ok": True, "message": "Account deleted"})

    except Exception as e:
        return JSONResponse(status_code=500, content={"ok": False, "error": str(e)})