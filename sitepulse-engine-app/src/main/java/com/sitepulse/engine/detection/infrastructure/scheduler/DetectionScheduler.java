package com.sitepulse.engine.detection.infrastructure.scheduler;

import com.sitepulse.engine.scheduler.application.JobExecutionGate;
import com.sitepulse.engine.detection.application.usecase.ProcessPendingImagesUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class DetectionScheduler {

    private static final String JOB_NAME = "detectionSweepJob";
    private final ProcessPendingImagesUseCase processPendingImagesUseCase;
    private final JobExecutionGate jobExecutionGate;

    @Scheduled(cron = "${sitepulse.detection-sweep-cron}", zone = "UTC")
    @SchedulerLock(name = JOB_NAME)
    public void runDetectionSweep() {
        if (!jobExecutionGate.shouldRun(JOB_NAME)) {
            return;
        }
        log.info("Running scheduled detection sweep");
        processPendingImagesUseCase.process(91);
    }
}
