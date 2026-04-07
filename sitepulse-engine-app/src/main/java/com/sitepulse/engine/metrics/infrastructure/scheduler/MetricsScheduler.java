package com.sitepulse.engine.metrics.infrastructure.scheduler;

import com.sitepulse.engine.metrics.application.usecase.RunScheduledAnalysisUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class MetricsScheduler {

    private final RunScheduledAnalysisUseCase runScheduledAnalysisUseCase;

    @Scheduled(cron = "${sitepulse.analysis-cron}", zone = "UTC")
    @SchedulerLock(name = "nightlyAnalysisJob")
    public void runAnalysis() {
        log.info("Running scheduled nightly analysis");
        runScheduledAnalysisUseCase.run();
    }
}
