# Dockerfile
FROM python:3.10-slim

# 시스템 라이브러리: OpenCV/MediaPipe 실행에 필요
RUN apt-get update && apt-get install -y --no-install-recommends \
    libgl1 libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# 파이썬 의존성
# (옵션1) 네가 올린 requirements.txt 사용 + FastAPI/uvicorn/python-multipart 추가 설치
COPY app/requirements.txt /app/requirements.txt
RUN pip install --no-cache-dir -r /app/requirements.txt

# 소스 복사
COPY app/ /app/

# uvicorn 실행 (0.0.0.0:8000)
EXPOSE 8000
ENV PYTHONUNBUFFERED=1

CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8000"]