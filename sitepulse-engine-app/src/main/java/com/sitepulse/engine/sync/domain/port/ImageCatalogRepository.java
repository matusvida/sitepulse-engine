package com.sitepulse.engine.sync.domain.port;

import com.sitepulse.engine.sync.domain.model.ImageImport;

public interface ImageCatalogRepository {

    boolean exists(String bucket, String key);

    boolean saveImportedImage(ImageImport imageImport);

    Integer resolveCameraId(Integer projectId, String key);
}
