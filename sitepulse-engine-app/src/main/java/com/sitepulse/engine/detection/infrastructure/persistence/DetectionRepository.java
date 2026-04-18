package com.sitepulse.engine.detection.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetectionRepository extends JpaRepository<DetectionEntity, Integer> {

    List<DetectionEntity> findByImageId(Integer imageId);

    void deleteByImageId(Integer imageId);

    @Query(value = "select class_name from detection_classes where id = :classId", nativeQuery = true)
    Optional<String> findClassNameByClassId(@Param("classId") Integer classId);
}
