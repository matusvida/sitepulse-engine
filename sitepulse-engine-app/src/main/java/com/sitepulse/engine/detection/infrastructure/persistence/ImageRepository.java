package com.sitepulse.engine.detection.infrastructure.persistence;

import com.sitepulse.engine.detection.domain.ImageStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImageRepository extends JpaRepository<ImageEntity, Integer> {

    boolean existsByBucketAndKey(String bucket, String key);

    @Query(value = """
            UPDATE images
            SET status = :processingStatus, updated_at = NOW()
            WHERE id IN (
                SELECT id FROM images
                WHERE status = 'NEW'
                ORDER BY id
                LIMIT :limit
                FOR UPDATE SKIP LOCKED
            )
            RETURNING *
            """, nativeQuery = true)
    List<ImageEntity> claimNewImages(@Param("limit") int limit, @Param("processingStatus") String processingStatus);

    default List<ImageEntity> claimNewImages(int limit) {
        return claimNewImages(limit, ImageStatus.PROCESSING.name());
    }

    @Query("""
            select i from ImageEntity i
            where i.projectId = :projectId and i.status in :statuses and i.capturedAt is not null
            order by i.capturedAt desc
            """)
    List<ImageEntity> findByProjectIdAndStatusInWithCapturedAtOrderByCapturedAtDesc(@Param("projectId") Integer projectId, @Param("statuses") List<ImageStatus> status);

    default List<ImageEntity> findProcessedByProject(Integer projectId) {
        return findByProjectIdAndStatusInWithCapturedAtOrderByCapturedAtDesc(projectId, List.of(ImageStatus.NEW, ImageStatus.DONE));
    }

    @Query("""
            select i from ImageEntity i
            where i.projectId = :projectId and i.status = :status and i.capturedAt >= :from and i.capturedAt < :to
            order by i.capturedAt asc
            """)
    List<ImageEntity> findByProjectIdAndStatusAndCapturedAtBetweenOrderByCapturedAtAsc(
            @Param("projectId") Integer projectId,
            @Param("status") ImageStatus status,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
    );

    default List<ImageEntity> findDoneInRange(Integer projectId, OffsetDateTime from, OffsetDateTime to) {
        return findByProjectIdAndStatusAndCapturedAtBetweenOrderByCapturedAtAsc(projectId, ImageStatus.DONE, from, to);
    }

    @Query("""
            select i.capturedAt from ImageEntity i
            where i.projectId = :projectId and i.status = :status and i.capturedAt is not null
            order by i.capturedAt desc
            """)
    List<OffsetDateTime> findCapturedAtValuesByProjectIdAndStatus(@Param("projectId") Integer projectId, @Param("status") ImageStatus status);

    default List<OffsetDateTime> findSnapshotCapturedAtValues(Integer projectId) {
        return findCapturedAtValuesByProjectIdAndStatus(projectId, ImageStatus.DONE);
    }

    @Query(value = """
            SELECT * FROM images
            WHERE project_id = :projectId
              AND status = :status
              AND captured_at >= :dayStart
              AND captured_at < :dayEnd
            ORDER BY ABS(EXTRACT(EPOCH FROM (captured_at - :midday))) ASC
            LIMIT 1
            """, nativeQuery = true)
    Optional<ImageEntity> findClosestSnapshot(
            @Param("projectId") Integer projectId,
            @Param("status") String status,
            @Param("dayStart") OffsetDateTime dayStart,
            @Param("dayEnd") OffsetDateTime dayEnd,
            @Param("midday") OffsetDateTime midday
    );

    default Optional<ImageEntity> findClosestSnapshot(Integer projectId, OffsetDateTime dayStart, OffsetDateTime dayEnd, OffsetDateTime midday) {
        return findClosestSnapshot(projectId, ImageStatus.DONE.name(), dayStart, dayEnd, midday);
    }
}
