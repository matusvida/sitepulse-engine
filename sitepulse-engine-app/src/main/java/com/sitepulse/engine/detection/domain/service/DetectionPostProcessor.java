package com.sitepulse.engine.detection.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitepulse.engine.common.exception.ProcessingException;
import com.sitepulse.engine.config.SitePulseProperties;
import com.sitepulse.engine.detection.domain.model.CameraRoiSettings;
import com.sitepulse.engine.detection.domain.model.DetectedObject;
import com.sitepulse.engine.detection.domain.model.DetectionInference;
import com.sitepulse.engine.detection.domain.model.DetectionOutcome;
import com.sitepulse.engine.detection.domain.model.RawDetection;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DetectionPostProcessor {

    private final SitePulseProperties properties;
    private final ObjectMapper objectMapper;

    public DetectionOutcome process(String bucket, String key, byte[] imageBytes, DetectionInference inference, CameraRoiSettings cameraRoiSettings) {
        BufferedImage image = decode(imageBytes);
        List<String> warnings = imageQualityWarnings(image);
        if (!warnings.isEmpty() && properties.skipBadQuality()) {
            return new DetectionOutcome(null, bucket, key, image.getWidth(), image.getHeight(), 0.0, List.of(), append(warnings, "Skipped inference due to bad image quality"), true);
        }
        List<DetectedObject> detections = filterDetections(inference.rawDetections(), cameraRoiSettings, warnings);
        return new DetectionOutcome(
                inference.modelVersion(),
                bucket,
                key,
                inference.imageWidth(),
                inference.imageHeight(),
                inference.inferenceMs(),
                detections,
                warnings,
                false
        );
    }

    public BufferedImage decode(byte[] imageBytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                throw new ProcessingException("Could not decode image");
            }
            return image;
        } catch (IOException ex) {
            throw new ProcessingException("Could not decode image", ex);
        }
    }

    private List<String> imageQualityWarnings(BufferedImage image) {
        List<String> warnings = new ArrayList<>();
        double brightness = 0;
        int pixels = image.getWidth() * image.getHeight();
        long diff = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int gray = (((rgb >> 16) & 0xff) + ((rgb >> 8) & 0xff) + (rgb & 0xff)) / 3;
                brightness += gray;
                if (x > 0) {
                    int prev = image.getRGB(x - 1, y);
                    int prevGray = (((prev >> 16) & 0xff) + ((prev >> 8) & 0xff) + (prev & 0xff)) / 3;
                    diff += Math.abs(gray - prevGray);
                }
            }
        }
        double meanBrightness = brightness / pixels;
        double blurEstimate = (double) diff / Math.max(1, pixels - image.getHeight());
        if (blurEstimate < properties.blurThreshold()) {
            warnings.add("Image appears blurry");
        }
        if (meanBrightness < properties.brightnessLow()) {
            warnings.add("Image is very dark");
        } else if (meanBrightness > properties.brightnessHigh()) {
            warnings.add("Image is overexposed");
        }
        return warnings;
    }

    private List<DetectedObject> filterDetections(List<RawDetection> rawDetections, CameraRoiSettings cameraRoiSettings, List<String> warnings) {
        Map<String, Double> perClassThresholds = properties.perClassThresholds(objectMapper);
        List<DetectedObject> kept = new ArrayList<>();
        int filteredConf = 0;
        int filteredArea = 0;
        int filteredRoi = 0;
        List<List<Double>> roiPolygon = cameraRoiSettings == null ? null : cameraRoiSettings.roiPolygon();
        boolean dropOutside = cameraRoiSettings != null && cameraRoiSettings.dropOutside();
        for (RawDetection detection : rawDetections) {
            double threshold = perClassThresholds.getOrDefault(detection.className(), properties.confThreshold());
            if (detection.score() < threshold) {
                filteredConf++;
                continue;
            }
            double area = Math.max(0.0, detection.bboxXyxy().get(2) - detection.bboxXyxy().get(0))
                    * Math.max(0.0, detection.bboxXyxy().get(3) - detection.bboxXyxy().get(1));
            if (area < properties.minBoxArea()) {
                filteredArea++;
                continue;
            }
            Boolean inRoi = null;
            if (roiPolygon != null && !roiPolygon.isEmpty()) {
                double cx = (detection.bboxXyxy().get(0) + detection.bboxXyxy().get(2)) / 2;
                double cy = (detection.bboxXyxy().get(1) + detection.bboxXyxy().get(3)) / 2;
                inRoi = pointInPolygon(cx, cy, roiPolygon);
                if (dropOutside && !inRoi) {
                    filteredRoi++;
                    continue;
                }
            }
            kept.add(new DetectedObject(
                    detection.classId(),
                    detection.className(),
                    round(detection.score()),
                    detection.bboxXyxy().stream().map(this::round).toList(),
                    inRoi,
                    detection.trackId(),
                    detection.colorHint(),
                    detection.notes()
            ));
        }
        if (filteredConf > 0) {
            warnings.add(filteredConf + " detections below confidence threshold");
        }
        if (filteredArea > 0) {
            warnings.add(filteredArea + " detections below minimum box area");
        }
        if (filteredRoi > 0) {
            warnings.add(filteredRoi + " detections outside ROI");
        }
        return kept;
    }

    private boolean pointInPolygon(double x, double y, List<List<Double>> polygon) {
        boolean inside = false;
        for (int i = 0, j = polygon.size() - 1; i < polygon.size(); j = i++) {
            double xi = polygon.get(i).get(0);
            double yi = polygon.get(i).get(1);
            double xj = polygon.get(j).get(0);
            double yj = polygon.get(j).get(1);
            boolean intersect = ((yi > y) != (yj > y)) && (x < (xj - xi) * (y - yi) / ((yj - yi) + 1e-9) + xi);
            if (intersect) {
                inside = !inside;
            }
        }
        return inside;
    }

    private List<String> append(List<String> warnings, String value) {
        List<String> all = new ArrayList<>(warnings);
        all.add(value);
        return all;
    }

    private double round(double value) {
        return Math.round(value * 10_000d) / 10_000d;
    }
}
