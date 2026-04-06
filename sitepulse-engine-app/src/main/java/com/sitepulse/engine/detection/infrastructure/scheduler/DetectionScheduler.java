package com.sitepulse.engine.detection.infrastructure.scheduler;

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

    private final ProcessPendingImagesUseCase processPendingImagesUseCase;

    @Scheduled(cron = "${sitepulse.detection-sweep-cron}", zone = "UTC")
    @SchedulerLock(name = "detectionSweepJob")
    public void runDetectionSweep() {
        log.info("Running scheduled detection sweep");
        processPendingImagesUseCase.process(20);
    }
}
