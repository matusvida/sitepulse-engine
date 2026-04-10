package com.sitepulse.engine.scheduler.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "job_feature_flags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JobFeatureFlagEntity {

    @Id
    @Column(name = "job_name", nullable = false, length = 128)
    private String jobName;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
