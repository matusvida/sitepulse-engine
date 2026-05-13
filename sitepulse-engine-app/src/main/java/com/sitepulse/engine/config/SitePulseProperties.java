package com.sitepulse.engine.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitepulse.engine.common.domain.enums.ImageFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sitepulse")
public record SitePulseProperties(
        @NotBlank String postgresDsn,
        @NotBlank String corsOrigins,
        @NotBlank String storageProvider,
        @NotBlank String storageDefaultBucket,
        @NotNull Integer storagePresignTtlMinutes,
        @Valid
        StorageProperties storage,
        @NotBlank String yoloModelPath,
        @NotNull Double confThreshold,
        @NotBlank String perClassThresholdsJson,
        @NotNull Double minBoxArea,
        boolean enableRoi,
        @NotBlank String roiConfigPath,
        @NotNull Double blurThreshold,
        @NotNull Integer brightnessLow,
        @NotNull Integer brightnessHigh,
        boolean skipBadQuality,
        @NotBlank String syncCron,
        @NotBlank String detectionSweepCron,
        @NotBlank String detectionProvider,
        @NotBlank String analysisCron,
        @NotNull Integer minDetectionsActiveHour,
        String dropboxToken,
        String dropboxAppKey,
        String dropboxAppSecret,
        String dropboxRefreshToken,
        String openaiApiKey,
        @NotBlank String openaiModel,
        @NotNull Long maxImageBytes,
        @NotBlank String pythonYoloBaseUrl,
        @Valid ImageWebSnapshotsProperties imageWebSnapshots,
        @Valid AuthProperties auth
) {

    public String[] corsOriginArray() {
        return corsOrigins.split("\\s*,\\s*");
    }

    public Duration storagePresignTtl() {
        return Duration.ofMinutes(storagePresignTtlMinutes);
    }

    public boolean usesLocalStorageProvisioning() {
        return "minio".equalsIgnoreCase(storageProvider) || "local".equalsIgnoreCase(storageProvider);
    }

    public record StorageProperties(
            @NotBlank String endpoint,
            @NotBlank String publicEndpoint,
            @NotBlank String accessKey,
            @NotBlank String secretKey,
            String region
    ) {
    }

    public record ImageWebSnapshotsProperties(
            boolean enabled,
            @NotNull Integer targetWidth,
            @NotNull Integer targetQuality,
            @NotNull ImageFormat targetFormat,
            @NotNull LocalTime freezeTime
    ) {
    }

    public record AuthProperties(
            @NotBlank String frontendBaseUrl,
            @NotBlank String sessionCookieName,
            boolean sessionCookieSecure,
            @NotBlank String sessionCookieSameSite,
            @NotNull Integer sessionTtlHours,
            @NotNull Integer invitationTtlHours,
            @NotNull Integer passwordResetTtlHours,
            String initialAdminEmail,
            String initialAdminPassword,
            @Valid MailProperties mail
    ) {
        public Duration sessionTtl() {
            return Duration.ofHours(sessionTtlHours);
        }

        public Duration invitationTtl() {
            return Duration.ofHours(invitationTtlHours);
        }

        public Duration passwordResetTtl() {
            return Duration.ofHours(passwordResetTtlHours);
        }

        public boolean hasSeededAdmin() {
            return initialAdminEmail != null
                    && !initialAdminEmail.isBlank()
                    && initialAdminPassword != null
                    && !initialAdminPassword.isBlank();
        }
    }

    public record MailProperties(
            boolean enabled,
            @NotBlank String appName,
            String from,
            String replyTo,
            @Valid ResendProperties resend
    ) {
        public boolean canUseResend() {
            return enabled
                    && resend != null
                    && resend.enabled()
                    && resend.apiKey() != null
                    && !resend.apiKey().isBlank()
                    && from != null
                    && !from.isBlank();
        }

        public String normalizedFrom() {
            return from == null ? null : from.trim();
        }

        public String normalizedReplyTo() {
            return replyTo == null || replyTo.isBlank() ? null : replyTo.trim();
        }
    }

    public record ResendProperties(
            boolean enabled,
            @NotBlank String baseUrl,
            String apiKey
    ) {
    }

    public Map<String, Double> perClassThresholds(ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(perClassThresholdsJson, new TypeReference<>() {});
        } catch (IOException ex) {
            throw new IllegalStateException("PER_CLASS_THRESHOLDS_JSON is not valid JSON", ex);
        }
    }

    public Map<String, RoiCameraConfig> loadRoiConfig(ObjectMapper objectMapper) {
        if (!enableRoi) {
            return Map.of();
        }
        Path path = Path.of(roiConfigPath);
        if (!Files.exists(path)) {
            return Map.of();
        }
        try {
            RoiConfig roiConfig = objectMapper.readValue(Files.readString(path), RoiConfig.class);
            return roiConfig.cameras();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read ROI config from " + roiConfigPath, ex);
        }
    }

    public record RoiConfig(Map<String, RoiCameraConfig> cameras) {
    }

    public record RoiCameraConfig(List<List<Double>> roiPolygon, Boolean dropOutside) {
    }
}
