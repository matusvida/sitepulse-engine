package com.sitepulse.engine.sync.infrastructure.persistence;

import com.sitepulse.engine.detection.domain.model.DetectionImage;
import com.sitepulse.engine.detection.domain.port.CameraLookup;
import com.sitepulse.engine.detection.domain.port.DetectionImageRepository;
import com.sitepulse.engine.sync.domain.model.ImageImport;
import com.sitepulse.engine.sync.domain.port.ImageCatalogRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Slf4j
@RequiredArgsConstructor
public class ImageCatalogRepositoryAdapter implements ImageCatalogRepository {

    private final DetectionImageRepository detectionImageRepository;
    private final CameraLookup cameraLookup;

    @Override
    public boolean exists(String bucket, String key) {
        return detectionImageRepository.existsByBucketAndKey(bucket, key);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean saveImportedImage(ImageImport imageImport) {
        try {
            Integer cameraId = resolveCameraId(imageImport.projectId(), imageImport.key());
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            detectionImageRepository.save(DetectionImage.createNew(
                    imageImport.bucket(),
                    imageImport.key(),
                    imageImport.projectId(),
                    cameraId,
                    imageImport.capturedAt(),
                    now
            ));
            return true;
        } catch (DataIntegrityViolationException ex) {
            if (detectionImageRepository.existsByBucketAndKey(imageImport.bucket(), imageImport.key())) {
                log.info("Ignoring duplicate image row for bucket={} key={} after concurrent sync", imageImport.bucket(), imageImport.key());
                return false;
            }
            throw ex;
        }
    }

    @Override
    public Integer resolveCameraId(Integer projectId, String key) {
        return cameraLookup.findCameraIdByProjectAndKey(projectId, key);
    }
}
