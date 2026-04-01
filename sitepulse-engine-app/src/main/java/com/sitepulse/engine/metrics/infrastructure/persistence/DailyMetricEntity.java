package com.sitepulse.engine.metrics.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "daily_metrics")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DailyMetricEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Integer id;

    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Column(nullable = false)
    @ToString.Include
    private LocalDate date;

    @Column(name = "people_count")
    private Integer peopleCount;

    @Column(name = "vehicle_count")
    private Integer vehicleCount;

    @Column(name = "active_hours")
    private Double activeHours;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
