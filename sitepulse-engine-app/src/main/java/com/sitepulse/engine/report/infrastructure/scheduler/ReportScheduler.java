package com.sitepulse.engine.report.infrastructure.scheduler;

import com.sitepulse.engine.report.application.usecase.RunScheduledDailyReportUseCase;
import com.sitepulse.engine.report.application.usecase.RunScheduledWeeklyReportUseCase;
import com.sitepulse.engine.scheduler.infrastructure.JobExecutionGate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportScheduler {

    private static final String DAILY_JOB_NAME = "automaticDailyReportJob";
    private static final String WEEKLY_JOB_NAME = "automaticWeeklyReportJob";

    private final RunScheduledDailyReportUseCase runScheduledDailyReportUseCase;
    private final RunScheduledWeeklyReportUseCase runScheduledWeeklyReportUseCase;
    private final JobExecutionGate jobExecutionGate;

    @Scheduled(cron = "${sitepulse.daily-report-cron:0 20 2 * * *}", zone = "UTC")
    @SchedulerLock(name = DAILY_JOB_NAME)
    public void runDailyReports() {
        if (!jobExecutionGate.shouldRun(DAILY_JOB_NAME)) {
            return;
        }
        log.info("Running scheduled daily report generation");
        runScheduledDailyReportUseCase.run();
    }

    @Scheduled(cron = "${sitepulse.weekly-report-cron:0 30 2 * * MON}", zone = "UTC")
    @SchedulerLock(name = WEEKLY_JOB_NAME)
    public void runWeeklyReports() {
        if (!jobExecutionGate.shouldRun(WEEKLY_JOB_NAME)) {
            return;
        }
        log.info("Running scheduled weekly report generation");
        runScheduledWeeklyReportUseCase.run();
    }
}
