package com.sitepulse.engine.scheduler;

import com.sitepulse.engine.config.SitePulseProperties;
import com.sitepulse.engine.detection.application.DetectionService;
import com.sitepulse.engine.metrics.application.AnalysisService;
import com.sitepulse.engine.plan.application.PlanService;
import com.sitepulse.engine.project.persistence.ProjectRepository;
import com.sitepulse.engine.sync.application.SyncService;
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
    private final SyncService syncService;
    private final DetectionService detectionService;
    private final AnalysisService analysisService;
    private final ProjectRepository projectRepository;

    @Scheduled(cron = "${sitepulse.sync-cron}", zone = "UTC")
    @SchedulerLock(name = "dropboxSyncJob")
    public void runSync() {
        log.info("Running scheduled Dropbox sync");
        syncService.syncAllProjects(projectRepository.findAll());
    }

    @Scheduled(cron = "${sitepulse.detection-sweep-cron}", zone = "UTC")
    @SchedulerLock(name = "detectionSweepJob")
    public void runDetectionSweep() {
        log.info("Running scheduled detection sweep");
        detectionService.processNewImages(10);
    }

    @Scheduled(cron = "0 0 ${sitepulse.analysis-hour} * * *", zone = "UTC")
    @SchedulerLock(name = "nightlyAnalysisJob")
    public void runAnalysis() {
        log.info("Running scheduled nightly analysis");
        analysisService.runAnalysis();
    }
}
