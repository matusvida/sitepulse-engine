package com.sitepulse.engine.report.application.service;

import com.sitepulse.engine.common.domain.enums.ImageFormat;
import com.sitepulse.engine.common.domain.port.ObjectStorage;
import com.sitepulse.engine.common.exception.ProcessingException;
import com.sitepulse.engine.config.SitePulseProperties;
import com.sitepulse.engine.report.domain.model.ProgressReport;
import com.sitepulse.engine.report.domain.model.ReportImageEvidence;
import com.sitepulse.engine.snapshot.application.result.CameraSnapshotProfile;
import com.sitepulse.engine.snapshot.application.service.WebImageTransformer;
import java.time.LocalTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StoredReportImageAssetService {

    private final ObjectStorage objectStorage;
    private final SitePulseProperties properties;
    private final WebImageTransformer webImageTransformer;

    public List<PreparedReportImageAsset> prepare(ProgressReport report, List<ReportImageEvidence> evidenceImages) {
        if (report == null || evidenceImages == null || evidenceImages.isEmpty()) {
            return List.of();
        }

        List<PreparedReportImageAsset> preparedAssets = new java.util.ArrayList<>();
        Map<String, Integer> usedNames = new LinkedHashMap<>();
        for (ReportImageEvidence evidenceImage : evidenceImages) {
            if (evidenceImage.imageId() == null) {
                throw new ProcessingException("Report evidence image is missing imageId");
            }
            if (evidenceImage.bucket() == null || evidenceImage.bucket().isBlank()) {
                throw new ProcessingException("Report evidence image is missing bucket");
            }
            if (evidenceImage.key() == null || evidenceImage.key().isBlank()) {
                throw new ProcessingException("Report evidence image is missing source key");
            }
            byte[] sourceBytes = decodeBase64(evidenceImage.base64Content(), evidenceImage.key());
            ImageFormat targetFormat = resolveTargetFormat(evidenceImage.key());
            CameraSnapshotProfile profile = compressionProfile(targetFormat);
            WebImageTransformer.TransformedImage transformedImage = webImageTransformer.transform(sourceBytes, profile);
            String outputExtension = resolveOutputExtension(evidenceImage.key(), targetFormat);
            String imagePath = buildImagePath(report, evidenceImage, outputExtension, usedNames);
            objectStorage.upload(evidenceImage.bucket(), imagePath, transformedImage.bytes(), transformedImage.mediaType());
            preparedAssets.add(new PreparedReportImageAsset(
                    evidenceImage.imageId(),
                    evidenceImage.bucket(),
                    imagePath
            ));
        }
        return preparedAssets;
    }

    private byte[] decodeBase64(String content, String key) {
        try {
            return Base64.getDecoder().decode(content);
        } catch (IllegalArgumentException ex) {
            throw new ProcessingException("Failed to decode report evidence image for " + key, ex);
        }
    }

    private ImageFormat resolveTargetFormat(String sourceKey) {
        String fileName = fileName(sourceKey);
        return ImageFormat.fromFileName(fileName).orElse(properties.imageWebSnapshots().targetFormat());
    }

    private CameraSnapshotProfile compressionProfile(ImageFormat targetFormat) {
        SitePulseProperties.ImageWebSnapshotsProperties defaults = properties.imageWebSnapshots();
        return new CameraSnapshotProfile(
                null,
                defaults.targetWidth(),
                defaults.targetQuality(),
                targetFormat,
                defaults.freezeTime() == null ? LocalTime.NOON : defaults.freezeTime()
        );
    }

    private String buildImagePath(
            ProgressReport report,
            ReportImageEvidence evidenceImage,
            String outputExtension,
            Map<String, Integer> usedNames
    ) {
        String sourceRoot = sourceRoot(evidenceImage.key());
        String reportType = normalizeSegment(report.getReportType(), "custom");
        String reportDate = report.getDateRangeStart() == null ? "undated" : report.getDateRangeStart().toString();
        String fileName = uniqueFileName(baseName(evidenceImage), outputExtension, usedNames);
        if (sourceRoot.isBlank()) {
            return "reports/" + reportType + "/" + reportDate + "/" + fileName;
        }
        return sourceRoot + "/reports/" + reportType + "/" + reportDate + "/" + fileName;
    }

    private String baseName(ReportImageEvidence evidenceImage) {
        String originalName = fileName(evidenceImage.key());
        int extensionIndex = originalName.lastIndexOf('.');
        String base = extensionIndex > 0 ? originalName.substring(0, extensionIndex) : originalName;
        String normalized = base == null ? "" : base.trim();
        if (!normalized.isBlank()) {
            return normalized;
        }
        return "image-" + evidenceImage.imageId();
    }

    private String uniqueFileName(String baseName, String outputExtension, Map<String, Integer> usedNames) {
        String extension = "." + outputExtension;
        String normalizedBase = baseName.endsWith(extension) ? baseName.substring(0, baseName.length() - extension.length()) : baseName;
        int count = usedNames.getOrDefault(normalizedBase, 0);
        usedNames.put(normalizedBase, count + 1);
        if (count == 0) {
            return normalizedBase + extension;
        }
        return normalizedBase + "-" + count + extension;
    }

    private String resolveOutputExtension(String sourceKey, ImageFormat targetFormat) {
        String fileName = fileName(sourceKey);
        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex > 0 && extensionIndex + 1 < fileName.length()) {
            String sourceExtension = fileName.substring(extensionIndex + 1).toLowerCase(Locale.ROOT);
            if (ImageFormat.fromExtension(sourceExtension).orElse(null) == targetFormat) {
                return sourceExtension;
            }
        }
        return targetFormat.getCanonicalExtension();
    }

    private String sourceRoot(String sourceKey) {
        String normalized = normalizeKey(sourceKey);
        String[] parts = normalized.split("/");
        if (parts.length >= 3) {
            return String.join("/", java.util.Arrays.copyOf(parts, parts.length - 2));
        }
        if (parts.length == 2) {
            return parts[0];
        }
        return "";
    }

    private String fileName(String sourceKey) {
        String normalized = normalizeKey(sourceKey);
        int lastSlash = normalized.lastIndexOf('/');
        return lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
    }

    private String normalizeKey(String key) {
        return key == null ? "" : key.replace('\\', '/').replaceAll("^/+", "").replaceAll("/+$", "");
    }

    private String normalizeSegment(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public record PreparedReportImageAsset(
            Integer imageId,
            String bucket,
            String imagePath
    ) {
    }
}
