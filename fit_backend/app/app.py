# app/app.py
from fastapi import FastAPI, UploadFile, File
from fastapi.responses import JSONResponse, Response
import base64

# ⬇️ 실제 파일명에 맞게 임포트 (중요!)
from m_body_shape_pipeline import classify_image as classify_male
from w_body_shape_pipeline import classify_image as classify_female

from clothes_pipeline import process_image as process_clothes_image
from uuid import uuid4
from pathlib import Path
import os

app = FastAPI(title="FitForYou", version="1.1.0")


def _pack_json(result: dict) -> dict:
    """overlay PNG bytes -> base64 문자열로 변환해서 JSON에 포함"""
    if result.get("overlay_png_bytes"):
        b64_img = base64.b64encode(result["overlay_png_bytes"]).decode("utf-8")
        result["overlay_png_b64"] = b64_img
        result.pop("overlay_png_bytes", None)
    return result

# ================================
# 1) 기존 체형 진단 엔드포인트
# ================================

@app.post("/classify/male")
async def classify_m(file: UploadFile = File(...)):
    image_bytes = await file.read()
    result = classify_male(image_bytes, return_overlay=True)
    if not result.get("ok"):
        return JSONResponse(status_code=400, content=result)
    return JSONResponse(status_code=200, content=_pack_json(result))


@app.post("/classify/female")
async def classify_w(file: UploadFile = File(...)):
    image_bytes = await file.read()
    result = classify_female(image_bytes, return_overlay=True)
    if not result.get("ok"):
        return JSONResponse(status_code=400, content=result)
    return JSONResponse(status_code=200, content=_pack_json(result))


@app.post("/classify-overlay/male")
async def classify_overlay_m(file: UploadFile = File(...)):
    image_bytes = await file.read()
    result = classify_male(image_bytes, return_overlay=True)
    if not result.get("ok"):
        return JSONResponse(status_code=400, content=result)
    if result.get("overlay_png_bytes"):
        return Response(content=result["overlay_png_bytes"], media_type="image/png")
    return JSONResponse(status_code=500, content={"ok": False, "error": "Overlay generation failed"})


@app.post("/classify-overlay/female")
async def classify_overlay_w(file: UploadFile = File(...)):
    image_bytes = await file.read()
    result = classify_female(image_bytes, return_overlay=True)
    if not result.get("ok"):
        return JSONResponse(status_code=400, content=result)
    if result.get("overlay_png_bytes"):
        return Response(content=result["overlay_png_bytes"], media_type="image/png")
    return JSONResponse(status_code=500, content={"ok": False, "error": "Overlay generation failed"})

# ================================
# 2) 의류 분류 엔드포인트 (새로 추가)
# ================================
BASE_DIR = Path(__file__).resolve().parent
CLOTHES_UPLOAD_DIR = BASE_DIR / "uploads_clothes"
os.makedirs(CLOTHES_UPLOAD_DIR, exist_ok=True)


@app.post("/clothes/classify")
async def classify_clothes(file: UploadFile = File(...)):
    """
    코디 전체 사진 1장을 받아서
    - YOLO로 의류 bbox 검출
    - 각 bbox별 속성 분류
    - JSON으로 반환
    """
    if not file.content_type.startswith("image/"):
        return JSONResponse(
            status_code=400,
            content={"ok": False, "error": "이미지 파일만 업로드 가능합니다."}
        )

    suffix = Path(file.filename).suffix or ".jpg"
    tmp_name = f"{uuid4().hex}{suffix}"
    tmp_path = CLOTHES_UPLOAD_DIR / tmp_name

    # 파일 저장
    content = await file.read()
    with open(tmp_path, "wb") as f:
        f.write(content)

    try:
        results = process_clothes_image(str(tmp_path))
    except Exception as e:
        # 디버깅 편하게 에러 그대로 리턴
        return JSONResponse(
            status_code=500,
            content={"ok": False, "error": f"의류 분류 중 오류: {e}"}
        )
    finally:
        # 원본 이미지는 굳이 안 남길 거면 삭제
        if tmp_path.exists():
            tmp_path.unlink()

    return JSONResponse(
        status_code=200,
        content={
            "ok": True,
            "filename": file.filename,
            "num_items": len(results),
            "items": results,
        }
    )