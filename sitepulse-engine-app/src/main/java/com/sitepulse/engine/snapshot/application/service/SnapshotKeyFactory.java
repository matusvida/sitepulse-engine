package com.sitepulse.engine.snapshot.application.service;

import com.sitepulse.engine.common.domain.model.ImageFormat;
import com.sitepulse.engine.project.domain.model.Camera;
import com.sitepulse.engine.project.domain.model.Project;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class SnapshotKeyFactory {

    public String create(Project project, Camera camera, LocalDate date, ImageFormat format) {
        String normalizedFormat = (format == null ? ImageFormat.WEBP : format).getCanonicalExtension();
        StringBuilder key = new StringBuilder();
        appendSegment(key, project.getStorageKeyPrefix());
        appendSegment(key, camera.getKeyPrefix());
        appendSegment(key, "snapshots");
        appendSegment(key, date.toString());
        appendSegment(key, "latest." + normalizedFormat);
        return key.toString();
    }

    private void appendSegment(StringBuilder key, String segment) {
        if (segment == null || segment.isBlank()) {
            return;
        }
        String normalized = segment.replaceAll("^/+", "").replaceAll("/+$", "");
        if (normalized.isBlank()) {
            return;
        }
        if (key.length() > 0) {
            key.append('/');
        }
        key.append(normalized);
    }
}
