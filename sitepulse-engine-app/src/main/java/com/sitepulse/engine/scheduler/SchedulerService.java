package com.sitepulse.engine.scheduler;

import com.sitepulse.engine.config.SitePulseProperties;
import com.sitepulse.engine.detection.application.usecase.ProcessPendingImagesUseCase;
import com.sitepulse.engine.metrics.application.usecase.RunScheduledAnalysisUseCase;
import com.sitepulse.engine.sync.application.usecase.RunScheduledSyncUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class SchedulerService {

    private final SitePulseProperties properties;
    private final RunScheduledSyncUseCase runScheduledSyncUseCase;
    private final ProcessPendingImagesUseCase processPendingImagesUseCase;
    private final RunScheduledAnalysisUseCase runScheduledAnalysisUseCase;

    @Scheduled(cron = "${sitepulse.sync-cron}", zone = "UTC")
    @SchedulerLock(name = "dropboxSyncJob")
    public void runSync() {
        log.info("Running scheduled Dropbox sync");
        runScheduledSyncUseCase.run();
    }

    @Scheduled(cron = "${sitepulse.detection-sweep-cron}", zone = "UTC")
    @SchedulerLock(name = "detectionSweepJob")
    public void runDetectionSweep() {
        log.info("Running scheduled detection sweep");
        processPendingImagesUseCase.process(10);
    }

    @Scheduled(cron = "0 0 ${sitepulse.analysis-hour} * * *", zone = "UTC")
    @SchedulerLock(name = "nightlyAnalysisJob")
    public void runAnalysis() {
        log.info("Running scheduled nightly analysis");
        runScheduledAnalysisUseCase.run();
    }
}
