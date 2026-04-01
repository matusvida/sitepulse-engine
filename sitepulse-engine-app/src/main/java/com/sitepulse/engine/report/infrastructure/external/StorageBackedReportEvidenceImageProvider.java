package com.sitepulse.engine.report.infrastructure.external;

import com.sitepulse.engine.common.domain.port.ObjectStorage;
import com.sitepulse.engine.detection.domain.port.ProcessedImageReadModel;
import com.sitepulse.engine.report.domain.model.ReportImageEvidence;
import com.sitepulse.engine.report.domain.port.ReportEvidenceImageProvider;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StorageBackedReportEvidenceImageProvider implements ReportEvidenceImageProvider {

    private final ProcessedImageReadModel processedImageReadModel;
    private final ObjectStorage objectStorage;

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
        int step = Math.max(1, rows.size() / maxImages);
        return IntStream.range(0, rows.size())
                .filter(index -> index % step == 0)
                .limit(maxImages)
                .mapToObj(rows::get)
                .map(image -> new ReportImageEvidence(
                        image.getCapturedAt() == null ? dateFrom.toString() : image.getCapturedAt().toLocalDate().toString(),
                        Base64.getEncoder().encodeToString(objectStorage.download(image.getBucket(), image.getKey()))
                ))
                .toList();
    }
}
