package com.sitepulse.engine.sync.infrastructure.scheduler;

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

    private final RunScheduledSyncUseCase runScheduledSyncUseCase;

    @Scheduled(cron = "${sitepulse.sync-cron}", zone = "UTC")
    @SchedulerLock(name = "dropboxSyncJob")
    public void runSync() {
        log.info("Running scheduled Dropbox sync");
        runScheduledSyncUseCase.run();
    }
}
