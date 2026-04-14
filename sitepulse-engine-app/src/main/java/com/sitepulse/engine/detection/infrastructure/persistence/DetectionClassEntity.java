package com.sitepulse.engine.detection.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "detection_classes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DetectionClassEntity {

    @Id
    private Integer id;

    @Column(name = "class_name", nullable = false, length = 128)
    private String className;

    @Column(name = "class_group", nullable = false, length = 64)
    private String classGroup;
}
