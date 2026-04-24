package com.sitepulse.engine.metrics.domain.service;

import com.sitepulse.engine.detection.application.enums.ImageEvidenceCandidateTag;
import com.sitepulse.engine.detection.domain.model.DetectionTaxonomy;
import com.sitepulse.engine.detection.domain.model.ImageEvidenceSummary;
import java.util.Locale;
import java.util.Set;

public class DailyActiveHoursCalculator {

    private static final Set<String> ACTIVE_CANDIDATE_TAGS = Set.of(
            ImageEvidenceCandidateTag.LOADING_ACTIVITY.value(),
            ImageEvidenceCandidateTag.EQUIPMENT_CHANGE.value(),
            ImageEvidenceCandidateTag.NEW_EQUIPMENT.value()
    );

    public boolean isActiveImage(ImageEvidenceSummary summary) {
        return hasWorkerPresence(summary)
                || !summary.changeFlags().isEmpty()
                || summary.candidateTags().stream()
                .anyMatch(ACTIVE_CANDIDATE_TAGS::contains);
    }

    private boolean hasWorkerPresence(ImageEvidenceSummary summary) {
        for (var entry : summary.classCounts().entrySet()) {
            String className = entry.getKey();
            Integer count = entry.getValue();
            if (count == null || count <= 0) {
                continue;
            }
            String normalized = className.trim().toLowerCase(Locale.ROOT);
            String canonical = DetectionTaxonomy.ALIAS_TO_CANONICAL.getOrDefault(normalized, normalized);
            if (DetectionTaxonomy.PERSON.equals(canonical)) {
                return true;
            }
        }
        return false;
    }
}
