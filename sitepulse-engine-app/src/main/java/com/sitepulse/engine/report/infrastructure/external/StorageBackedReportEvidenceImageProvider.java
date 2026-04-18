package com.sitepulse.engine.report.infrastructure.external;

import com.sitepulse.engine.common.domain.port.ObjectStorage;
import com.sitepulse.engine.config.SitePulseProperties;
import com.sitepulse.engine.detection.domain.port.ProcessedImageReadModel;
import com.sitepulse.engine.detection.domain.model.StoredImage;
import com.sitepulse.engine.report.domain.model.ReportImageEvidence;
import com.sitepulse.engine.report.domain.port.ReportEvidenceImageProvider;
import java.time.temporal.ChronoUnit;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StorageBackedReportEvidenceImageProvider implements ReportEvidenceImageProvider {

    private final ProcessedImageReadModel processedImageReadModel;
    private final ObjectStorage objectStorage;
    private final SitePulseProperties properties;

    @Override
    public List<ReportImageEvidence> gather(Integer projectId, LocalDate dateFrom, LocalDate dateTo, int maxImages) {
        var rows = processedImageReadModel.findDoneInRange(
                projectId,
                dateFrom.atStartOfDay().atOffset(ZoneOffset.UTC),
                dateTo.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC)
        );
        if (rows.isEmpty()) {
            return List.of();
        }
        return selectEvidence(rows, dateFrom, dateTo, maxImages).stream()
                .map(image -> new ReportImageEvidence(
                        image.getCapturedAt() == null ? dateFrom.toString() : image.getCapturedAt().toLocalDate().toString(),
                        Base64.getEncoder().encodeToString(objectStorage.download(image.getBucket(), image.getKey())),
                        image.getCapturedAt(),
                        image.getBucket(),
                        image.getKey(),
                        objectStorage.presign(image.getBucket(), image.getKey(), properties.storagePresignTtl())
                ))
                .toList();
    }

    List<StoredImage> selectEvidence(List<StoredImage> rows, LocalDate dateFrom, LocalDate dateTo, int maxImages) {
        if (rows.isEmpty()) {
            return List.of();
        }
        int days = Math.max(1, (int) ChronoUnit.DAYS.between(dateFrom, dateTo) + 1);
        if (days <= 1) {
            return selectDaily(rows, Math.min(maxImages, 4));
        }
        if (days <= 7) {
            return selectWeekly(rows, Math.min(maxImages, 6));
        }
        return selectCustom(rows, Math.min(maxImages, 6));
    }

    private List<StoredImage> selectDaily(List<StoredImage> rows, int maxImages) {
        Map<Integer, StoredImage> selected = new LinkedHashMap<>();
        add(selected, bestOverall(rows));
        add(selected, bestBy(rows, image -> score(image.getEvidenceActivityScore()) * 1.2 + score(image.getEvidenceOverallScore())));
        add(selected, bestBy(rows, image -> score(image.getEvidenceChangeScore()) * 1.2 + score(image.getEvidenceOverallScore())));
        add(selected, latest(rows));
        return selected.values().stream().limit(maxImages).toList();
    }

    private List<StoredImage> selectWeekly(List<StoredImage> rows, int maxImages) {
        Map<Integer, StoredImage> selected = new LinkedHashMap<>();
        rows.stream()
                .filter(image -> image.getCapturedAt() != null)
                .collect(java.util.stream.Collectors.groupingBy(image -> image.getCapturedAt().toLocalDate(), LinkedHashMap::new, java.util.stream.Collectors.toList()))
                .values()
                .forEach(dayRows -> add(selected, bestOverall(dayRows)));
        rows.stream()
                .sorted(Comparator.comparingDouble((StoredImage image) -> score(image.getEvidenceChangeScore()) * 1.3 + score(image.getEvidenceOverallScore())).reversed())
                .limit(2)
                .forEach(image -> add(selected, image));
        return selected.values().stream().limit(maxImages).toList();
    }

    private List<StoredImage> selectCustom(List<StoredImage> rows, int maxImages) {
        Map<Integer, StoredImage> selected = new LinkedHashMap<>();
        rows.stream()
                .sorted(Comparator.comparingDouble(this::blendedScore).reversed())
                .limit(Math.max(1, maxImages / 2))
                .forEach(image -> add(selected, image));
        rows.stream()
                .filter(image -> image.getCapturedAt() != null)
                .collect(java.util.stream.Collectors.groupingBy(image -> image.getCapturedAt().toLocalDate(), LinkedHashMap::new, java.util.stream.Collectors.toList()))
                .values()
                .stream()
                .map(this::bestOverall)
                .forEach(image -> add(selected, image));
        add(selected, latest(rows));
        return selected.values().stream().limit(maxImages).toList();
    }

    private StoredImage bestOverall(List<StoredImage> rows) {
        return bestBy(rows, this::blendedScore);
    }

    private StoredImage latest(List<StoredImage> rows) {
        return rows.stream()
                .max(Comparator.comparing(StoredImage::getCapturedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(StoredImage::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(rows.getLast());
    }

    private StoredImage bestBy(List<StoredImage> rows, Function<StoredImage, Double> scoreFn) {
        return rows.stream()
                .max(Comparator.comparing(scoreFn)
                        .thenComparing(StoredImage::getCapturedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(StoredImage::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(rows.getFirst());
    }

    private double blendedScore(StoredImage image) {
        return score(image.getEvidenceOverallScore()) * 1.5
                + score(image.getEvidenceActivityScore()) * 1.1
                + score(image.getEvidenceChangeScore())
                + score(image.getEvidenceQualityScore()) * 0.8;
    }

    private double score(Double value) {
        return value == null ? 0.0 : value;
    }

    private void add(Map<Integer, StoredImage> selected, StoredImage image) {
        if (image != null && image.getId() != null) {
            selected.putIfAbsent(image.getId(), image);
        }
    }
}
