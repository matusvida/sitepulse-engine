package com.sitepulse.engine.detection.application;

import com.sitepulse.engine.common.util.JsonUtils;
import com.sitepulse.engine.common.web.ApiException;
import com.sitepulse.engine.config.SitePulseProperties;
import com.sitepulse.engine.detection.domain.DetectionEntity;
import com.sitepulse.engine.detection.domain.ImageEntity;
import com.sitepulse.engine.detection.domain.ImageStatus;
import com.sitepulse.engine.detection.persistence.DetectionRepository;
import com.sitepulse.engine.detection.persistence.ImageRepository;
import com.sitepulse.engine.http.detection.dto.DetectRequest;
import com.sitepulse.engine.http.detection.dto.DetectResponse;
import com.sitepulse.engine.http.detection.dto.DetectionView;
import com.sitepulse.engine.http.detection.dto.HealthResponse;
import com.sitepulse.engine.integration.storage.StorageService;
import com.sitepulse.engine.integration.yolo.YoloFeignClient;
import com.sitepulse.engine.integration.yolo.dto.YoloInferRequest;
import com.sitepulse.engine.integration.yolo.dto.YoloInferResponse;
import com.sitepulse.engine.integration.yolo.dto.YoloRawDetection;
import com.sitepulse.engine.project.application.ProjectService;
import com.sitepulse.engine.project.domain.CameraEntity;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import javax.imageio.ImageIO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DetectionService {

    private final StorageService storageService;
    private final YoloFeignClient yoloFeignClient;
    private final SitePulseProperties properties;
    private final ImageRepository imageRepository;
    private final DetectionRepository detectionRepository;
    private final ProjectService projectService;
    private final JsonUtils jsonUtils;

    public DetectResponse health() {
        var health = yoloFeignClient.health();
        return new DetectResponse(
                health.getModelVersion(),
                null,
                null,
                null,
                null,
                null,
                List.of(),
                List.of()
        );
    }

    public HealthResponse yoloHealth() {
        var health = yoloFeignClient.health();
        return new HealthResponse(
                health.getStatus(),
                health.getModelLoaded(),
                health.getModelVersion()
        );
    }

    @Transactional
    public DetectResponse detect(DetectRequest request) {
        ResolvedS3Location location = resolveLocation(request);
        log.info("Running on-demand detection for bucket={} key={}", location.bucket(), location.key());
        return runDetection(location);
    }

    @Transactional
    public void processNewImages(int limit) {
        List<ImageEntity> rows = imageRepository.claimNewImages(limit);
        log.info("Claimed {} new images for detection processing", rows.size());
        for (ImageEntity row : rows) {
            try {
                runDetection(new ResolvedS3Location(row.getBucket(), row.getKey(), row.getProjectId(), row.getId()));
                row.setStatus(ImageStatus.DONE);
                log.info("Detection completed for imageId={} key={}", row.getId(), row.getKey());
            } catch (Exception ex) {
                row.setStatus(ImageStatus.FAILED);
                log.error("Detection failed for imageId={} key={} reason={}", row.getId(), row.getKey(), ex.getMessage(), ex);
            }
            row.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
            imageRepository.save(row);
        }
    }

    private DetectResponse runDetection(ResolvedS3Location location) {
        byte[] imageBytes = storageService.download(location.bucket(), location.key());
        BufferedImage image = decode(imageBytes);

        List<String> warnings = imageQualityWarnings(image);
        if (!warnings.isEmpty() && properties.skipBadQuality()) {
            log.warn("Skipping inference for bucket={} key={} due to image quality warnings={}", location.bucket(), location.key(), warnings);
            return new DetectResponse(null, location.bucket(), location.key(), image.getWidth(), image.getHeight(), 0.0, List.of(), append(warnings, "Skipped inference due to bad image quality"));
        }

        YoloInferResponse inferResponse = yoloFeignClient.infer(new YoloInferRequest(Base64.getEncoder().encodeToString(imageBytes)));
        CameraEntity camera = location.projectId() != null ? projectService.findCameraByKey(location.projectId(), location.key()) : null;
        List<DetectionView> detections = postProcess(inferResponse.getRawDetections(), location.key(), camera, warnings);

        persistDetection(location, inferResponse, detections);
        log.info("Detection finished for bucket={} key={} detections={} warnings={}",
                location.bucket(), location.key(), detections.size(), warnings.size());
        return new DetectResponse(
                inferResponse.getModelVersion(),
                location.bucket(),
                location.key(),
                inferResponse.getImageWidth(),
                inferResponse.getImageHeight(),
                inferResponse.getInferenceMs(),
                detections,
                warnings
        );
    }

    private ResolvedS3Location resolveLocation(DetectRequest request) {
        if (request.getS3Url() != null && !request.getS3Url().isBlank()) {
            if (!request.getS3Url().startsWith("s3://")) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid S3 URL");
            }
            String raw = request.getS3Url().substring("s3://".length());
            int slash = raw.indexOf('/');
            if (slash < 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid S3 URL");
            }
            return new ResolvedS3Location(raw.substring(0, slash), raw.substring(slash + 1), null, null);
        }
        if (request.getKey() == null || request.getKey().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Either 'key' or 's3_url' must be provided");
        }
        return new ResolvedS3Location(
                request.getBucket() == null || request.getBucket().isBlank() ? properties.minioBucketDefault() : request.getBucket(),
                request.getKey(),
                null,
                null
        );
    }

    private BufferedImage decode(byte[] imageBytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Could not decode image");
            }
            return image;
        } catch (Exception ex) {
            if (ex instanceof ApiException apiException) {
                throw apiException;
            }
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Could not decode image");
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

    private List<DetectionView> postProcess(List<YoloRawDetection> rawDetections, String imageKey, CameraEntity camera, List<String> warnings) {
        Map<String, Double> perClassThresholds = properties.perClassThresholds(new com.fasterxml.jackson.databind.ObjectMapper());
        List<DetectionView> kept = new ArrayList<>();
        int filteredConf = 0;
        int filteredArea = 0;
        int filteredRoi = 0;
        List<List<Double>> roiPolygon = camera != null ? camera.getRoiPolygon() : null;
        boolean dropOutside = camera != null && Boolean.TRUE.equals(camera.getDropOutside());
        for (YoloRawDetection detection : rawDetections) {
            double threshold = perClassThresholds.getOrDefault(detection.getClassName(), properties.confThreshold());
            if (detection.getScore() < threshold) {
                filteredConf++;
                continue;
            }
            double area = Math.max(0.0, detection.getBboxXyxy().get(2) - detection.getBboxXyxy().get(0))
                    * Math.max(0.0, detection.getBboxXyxy().get(3) - detection.getBboxXyxy().get(1));
            if (area < properties.minBoxArea()) {
                filteredArea++;
                continue;
            }
            Boolean inRoi = null;
            if (roiPolygon != null && !roiPolygon.isEmpty()) {
                double cx = (detection.getBboxXyxy().get(0) + detection.getBboxXyxy().get(2)) / 2;
                double cy = (detection.getBboxXyxy().get(1) + detection.getBboxXyxy().get(3)) / 2;
                inRoi = pointInPolygon(cx, cy, roiPolygon);
                if (dropOutside && !inRoi) {
                    filteredRoi++;
                    continue;
                }
            }
            kept.add(new DetectionView(detection.getClassId(), detection.getClassName(), round(detection.getScore()), detection.getBboxXyxy().stream().map(this::round).toList(), inRoi));
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

    private Integer persistDetection(ResolvedS3Location location, YoloInferResponse inferResponse, List<DetectionView> detections) {
        ImageEntity imageEntity;
        if (location.imageId() != null) {
            imageEntity = imageRepository.findById(location.imageId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Image not found"));
            imageEntity.setStatus(ImageStatus.DONE);
            imageEntity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
            imageEntity = imageRepository.save(imageEntity);
        } else {
            imageEntity = imageRepository.save(ImageEntity.builder()
                    .bucket(location.bucket())
                    .key(location.key())
                    .status(ImageStatus.DONE)
                    .projectId(location.projectId())
                    .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                    .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                    .build());
        }
        for (DetectionView detection : detections) {
            detectionRepository.save(DetectionEntity.builder()
                    .imageId(imageEntity.getId())
                    .projectId(location.projectId())
                    .modelVersion(inferResponse.getModelVersion())
                    .classId(detection.getClassId())
                    .className(detection.getClassName())
                    .score(detection.getScore())
                    .bboxXyxy(jsonUtils.write(detection.getBboxXyxy()))
                    .inRoi(detection.getInRoi() == null ? null : detection.getInRoi().toString())
                    .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                    .build());
        }
        return imageEntity.getId();
    }

    private List<String> append(List<String> warnings, String value) {
        List<String> all = new ArrayList<>(warnings);
        all.add(value);
        return all;
    }

    private double round(double value) {
        return Math.round(value * 10_000d) / 10_000d;
    }

    private record ResolvedS3Location(String bucket, String key, Integer projectId, Integer imageId) {
    }
}
