FROM python:3.12-slim

# System deps for opencv-python-headless
RUN apt-get update && \
    apt-get install -y --no-install-recommends libgl1 libglib2.0-0 && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /opt/sitepulse-engine

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

# Pre-download the default model weights at build time so first request is fast
RUN python -c "from ultralytics import YOLO; YOLO('yolov8x.pt')"

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "1"]
