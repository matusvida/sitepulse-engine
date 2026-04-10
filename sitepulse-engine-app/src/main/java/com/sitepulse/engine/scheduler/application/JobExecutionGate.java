package com.sitepulse.engine.scheduler.application;

import com.sitepulse.engine.scheduler.infrastructure.persistence.JobFeatureFlagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobExecutionGate {

    private final JobFeatureFlagRepository jobFeatureFlagRepository;

    public boolean isEnabled(String jobName) {
        return jobFeatureFlagRepository.findById(jobName)
                .map(flag -> flag.isEnabled())
                .orElse(false);
    }

    public boolean shouldRun(String jobName) {
        boolean enabled = isEnabled(jobName);
        if (!enabled) {
            log.info("Skipping scheduled job jobName={} because it is disabled in job_feature_flags", jobName);
        }
        return enabled;
    }
}
