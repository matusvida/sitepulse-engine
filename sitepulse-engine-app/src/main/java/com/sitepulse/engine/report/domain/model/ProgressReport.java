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
    private final String generationOrigin;
    private final String periodKey;
    private final String confidenceLevel;
    private final String language;
    private final String contentMd;
    private final String headline;
    private final String summary;
    private final LocalDate dateRangeStart;
    private final LocalDate dateRangeEnd;
    private final Integer imageCount;
    private final Integer evidenceImageCount;
    private final String modelUsed;
    private final OffsetDateTime createdAt;

    public static ProgressReport create(
            Integer projectId,
            String reportType,
            String generationOrigin,
            String periodKey,
            String confidenceLevel,
            String language,
            String contentMd,
            String headline,
            String summary,
            LocalDate dateRangeStart,
            LocalDate dateRangeEnd,
            Integer imageCount,
            Integer evidenceImageCount,
            String modelUsed,
            OffsetDateTime createdAt
    ) {
        if (dateRangeStart != null && dateRangeEnd != null && dateRangeStart.isAfter(dateRangeEnd)) {
            throw new IllegalArgumentException("dateRangeStart must not be after dateRangeEnd");
        }
        if (contentMd == null || contentMd.isBlank()) {
            throw new IllegalArgumentException("Report content must not be blank");
        }
        return new ProgressReport(
                null,
                projectId,
                reportType,
                generationOrigin,
                periodKey,
                confidenceLevel,
                language,
                contentMd,
                headline,
                summary,
                dateRangeStart,
                dateRangeEnd,
                imageCount,
                evidenceImageCount,
                modelUsed,
                createdAt
        );
    }

    public static ProgressReport restore(
            Integer id,
            Integer projectId,
            String reportType,
            String generationOrigin,
            String periodKey,
            String confidenceLevel,
            String language,
            String contentMd,
            String headline,
            String summary,
            LocalDate dateRangeStart,
            LocalDate dateRangeEnd,
            Integer imageCount,
            Integer evidenceImageCount,
            String modelUsed,
            OffsetDateTime createdAt
    ) {
        return new ProgressReport(
                id,
                projectId,
                reportType,
                generationOrigin,
                periodKey,
                confidenceLevel,
                language,
                contentMd,
                headline,
                summary,
                dateRangeStart,
                dateRangeEnd,
                imageCount,
                evidenceImageCount,
                modelUsed,
                createdAt
        );
    }
}
