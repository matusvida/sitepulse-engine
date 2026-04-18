package com.sitepulse.engine.sync.infrastructure.scheduler;

import com.sitepulse.engine.scheduler.infrastructure.JobExecutionGate;
import com.sitepulse.engine.sync.application.usecase.RunScheduledSyncUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class SyncScheduler {

    private static final String JOB_NAME = "dropboxSyncJob";
    private final RunScheduledSyncUseCase runScheduledSyncUseCase;
    private final JobExecutionGate jobExecutionGate;

    @Scheduled(cron = "${sitepulse.sync-cron}", zone = "UTC")
    @SchedulerLock(name = JOB_NAME)
    public void runSync() {
        if (!jobExecutionGate.shouldRun(JOB_NAME)) {
            return;
        }
        log.info("Running scheduled Dropbox sync");
        runScheduledSyncUseCase.run();
    }
}
