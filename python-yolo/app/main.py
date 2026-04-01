from __future__ import annotations

import base64
import os
import time
from contextlib import asynccontextmanager

import cv2
import numpy as np
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from ultralytics import YOLO


class InferRequest(BaseModel):
    imageBase64: str


class RawDetection(BaseModel):
    classId: int
    className: str
    score: float
    bboxXyxy: list[float]


class InferResponse(BaseModel):
    modelVersion: str
    imageWidth: int
    imageHeight: int
    inferenceMs: float
    rawDetections: list[RawDetection]


MODEL_PATH = os.getenv("YOLO_MODEL_PATH", "yolov8x.pt")
MODEL: YOLO | None = None


def load_model() -> None:
    global MODEL
    if MODEL is None:
        MODEL = YOLO(MODEL_PATH)


@asynccontextmanager
async def lifespan(_: FastAPI):
    load_model()
    yield


app = FastAPI(title="sitepulse-python-yolo", version="0.1.0", lifespan=lifespan)


@app.get("/health")
async def health():
    return {
        "status": "ok",
        "modelLoaded": MODEL is not None,
        "modelVersion": MODEL_PATH,
    }


@app.post("/infer", response_model=InferResponse)
async def infer(request: InferRequest):
    if MODEL is None:
        raise HTTPException(status_code=503, detail="Model not loaded")

    try:
        image_bytes = base64.b64decode(request.imageBase64)
    except Exception as exc:
        raise HTTPException(status_code=400, detail="Invalid base64 image payload") from exc

    arr = np.frombuffer(image_bytes, dtype=np.uint8)
    image = cv2.imdecode(arr, cv2.IMREAD_COLOR)
    if image is None:
        raise HTTPException(status_code=422, detail="Could not decode image")

    height, width = image.shape[:2]
    started = time.perf_counter()
    results = MODEL.predict(source=image, verbose=False, conf=0.10)
    inference_ms = (time.perf_counter() - started) * 1000

    detections: list[RawDetection] = []
    for result in results:
        boxes = result.boxes
        if boxes is None:
            continue
        for index in range(len(boxes)):
            bbox = [round(value, 1) for value in boxes.xyxy[index].tolist()]
            score = float(boxes.conf[index])
            class_id = int(boxes.cls[index])
            class_name = result.names.get(class_id, str(class_id))
            detections.append(
                RawDetection(
                    classId=class_id,
                    className=class_name,
                    score=score,
                    bboxXyxy=bbox,
                )
            )

    return InferResponse(
        modelVersion=MODEL_PATH,
        imageWidth=width,
        imageHeight=height,
        inferenceMs=round(inference_ms, 1),
        rawDetections=detections,
    )
