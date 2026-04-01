package com.sitepulse.engine.report.domain.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class ProgressReport {

    @EqualsAndHashCode.Include
    @ToString.Include
    private final Integer id;

    private final Integer projectId;
    private final String reportType;
    private final String contentMd;
    private final String summary;
    private final LocalDate dateRangeStart;
    private final LocalDate dateRangeEnd;
    private final Integer imageCount;
    private final String modelUsed;
    private final OffsetDateTime createdAt;

    public static ProgressReport create(
            Integer projectId,
            String reportType,
            String contentMd,
            String summary,
            LocalDate dateRangeStart,
            LocalDate dateRangeEnd,
            Integer imageCount,
            String modelUsed,
            OffsetDateTime createdAt
    ) {
        return new ProgressReport(null, projectId, reportType, contentMd, summary, dateRangeStart, dateRangeEnd, imageCount, modelUsed, createdAt);
    }

    public static ProgressReport restore(
            Integer id,
            Integer projectId,
            String reportType,
            String contentMd,
            String summary,
            LocalDate dateRangeStart,
            LocalDate dateRangeEnd,
            Integer imageCount,
            String modelUsed,
            OffsetDateTime createdAt
    ) {
        return new ProgressReport(id, projectId, reportType, contentMd, summary, dateRangeStart, dateRangeEnd, imageCount, modelUsed, createdAt);
    }
}
