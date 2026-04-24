package com.sitepulse.engine.detection.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ImageEvidenceSummary(
        @JsonProperty("class_counts") Map<String, Integer> classCounts,
        @JsonProperty("dominant_classes") List<String> dominantClasses,
        @JsonProperty("first_appearance_flags") List<String> firstAppearanceFlags,
        @JsonProperty("change_flags") List<String> changeFlags,
        @JsonProperty("quality_flags") List<String> qualityFlags,
        @JsonProperty("candidate_tags") List<String> candidateTags
) {
    public ImageEvidenceSummary {
        classCounts = classCounts == null ? Map.of() : Map.copyOf(classCounts);
        dominantClasses = dominantClasses == null ? List.of() : List.copyOf(dominantClasses);
        firstAppearanceFlags = firstAppearanceFlags == null ? List.of() : List.copyOf(firstAppearanceFlags);
        changeFlags = changeFlags == null ? List.of() : List.copyOf(changeFlags);
        qualityFlags = qualityFlags == null ? List.of() : List.copyOf(qualityFlags);
        candidateTags = candidateTags == null ? List.of() : List.copyOf(candidateTags);
    }

    public static ImageEvidenceSummary empty() {
        return new ImageEvidenceSummary(Map.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }
}
