package com.sitepulse.engine.common.domain.enums;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import lombok.Getter;
import org.springframework.http.MediaType;

@Getter
public enum ImageFormat {
    JPEG("jpeg", MediaType.IMAGE_JPEG_VALUE, "jpg", "jpeg"),
    PNG("png", MediaType.IMAGE_PNG_VALUE, "png"),
    WEBP("webp", "image/webp", "webp");

    private final String canonicalExtension;
    private final String mediaType;
    private final String[] extensions;

    ImageFormat(String canonicalExtension, String mediaType, String... extensions) {
        this.canonicalExtension = canonicalExtension;
        this.mediaType = mediaType;
        this.extensions = extensions;
    }

    public String dataUriPrefix() {
        return "data:" + mediaType + ";base64,";
    }

    public boolean matchesFileName(String fileName) {
        String normalized = normalizeFileName(fileName);
        return Arrays.stream(extensions).anyMatch(extension -> normalized.endsWith("." + extension));
    }

    public static Optional<ImageFormat> fromFileName(String fileName) {
        return Arrays.stream(values())
                .filter(format -> format.matchesFileName(fileName))
                .findFirst();
    }

    public static Optional<ImageFormat> fromExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            return Optional.empty();
        }
        String normalized = extension.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(format -> Arrays.stream(format.extensions).anyMatch(normalized::equals))
                .findFirst();
    }

    public static ImageFormat fromConfiguredFormat(String extension, ImageFormat fallback) {
        return fromExtension(extension).orElse(fallback);
    }

    private static String normalizeFileName(String fileName) {
        return fileName == null ? "" : fileName.trim().toLowerCase(Locale.ROOT);
    }
}
