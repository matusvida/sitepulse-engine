package com.sitepulse.engine.sync.domain.port;

import com.sitepulse.engine.detection.domain.model.DetectionImage;
import com.sitepulse.engine.sync.domain.model.ImageImport;
import java.util.Optional;

public interface ImageCatalogRepository {

    boolean exists(String bucket, String key);

    SaveImportedImageResult saveImportedImage(ImageImport imageImport);

    Integer resolveCameraId(Integer projectId, String key);

    record SaveImportedImageResult(boolean imported, Optional<DetectionImage> image) {
    }
}
