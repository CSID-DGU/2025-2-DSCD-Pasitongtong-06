# app/app.py
from fastapi import FastAPI, UploadFile, File
from fastapi.responses import JSONResponse, Response
import base64

# ⬇️ 실제 파일명에 맞게 임포트 (중요!)
from m_body_shape_pipeline import classify_image as classify_male
from w_body_shape_pipeline import classify_image as classify_female

app = FastAPI(title="FitForYou Body Shape API", version="1.0.0")


def _pack_json(result: dict) -> dict:
    """overlay PNG bytes -> base64 문자열로 변환해서 JSON에 포함"""
    if result.get("overlay_png_bytes"):
        b64_img = base64.b64encode(result["overlay_png_bytes"]).decode("utf-8")
        result["overlay_png_b64"] = b64_img
        result.pop("overlay_png_bytes", None)
    return result


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