package com.sitepulse.engine.visualization.application.usecase;

import com.sitepulse.engine.common.domain.enums.ImageFormat;
import com.sitepulse.engine.common.domain.port.ObjectStorage;
import com.sitepulse.engine.common.exception.SitePulseException;
import com.sitepulse.engine.detection.domain.model.DetectedObject;
import com.sitepulse.engine.detection.domain.model.StoredImage;
import com.sitepulse.engine.detection.domain.port.ProcessedImageReadModel;
import com.sitepulse.engine.project.application.ProjectLookupService;
import com.sitepulse.engine.visualization.application.result.VisualizationBatchResult;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;

@Service
@RequiredArgsConstructor
public class GenerateDetectionVisualizationUseCase {

    private static final ImageFormat OUTPUT_FORMAT = ImageFormat.JPEG;
    private static final Map<String, Color> COLORS = Map.of(
            "person", new Color(0, 120, 255),
            "car", new Color(255, 80, 0),
            "truck", new Color(255, 0, 80),
            "bus", new Color(200, 0, 200)
    );

    private final ProjectLookupService projectLookupService;
    private final ProcessedImageReadModel processedImageReadModel;
    private final ObjectStorage objectStorage;

    public VisualizationBatchResult generate(Integer projectId, LocalDate dateFrom, LocalDate dateTo) {
        projectLookupService.requireProject(projectId);
        List<StoredImage> images = processedImageReadModel.findDoneInRange(
                projectId,
                dateFrom.atStartOfDay().atOffset(ZoneOffset.UTC),
                dateTo.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC)
        ).stream()
                .filter(image -> !processedImageReadModel.findDetections(image.getId()).isEmpty())
                .toList();

        int processed = 0;
        List<String> errors = new ArrayList<>();
        for (StoredImage image : images) {
            try {
                byte[] original = objectStorage.download(image.getBucket(), image.getKey());
                byte[] annotated = drawDetections(original, processedImageReadModel.findDetections(image.getId()));
                objectStorage.upload(image.getBucket(), "detection/" + image.getKey(), annotated, OUTPUT_FORMAT.getMediaType());
                processed++;
            } catch (SitePulseException | IOException ex) {
                errors.add(image.getKey() + ": " + ex.getMessage());
            }
        }
        return new VisualizationBatchResult(images.size(), processed, errors);
    }

    private byte[] drawDetections(byte[] bytes, List<DetectedObject> detections) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        Graphics2D graphics = image.createGraphics();
        graphics.setStroke(new BasicStroke(2));
        graphics.setFont(new Font("SansSerif", Font.BOLD, 14));
        for (DetectedObject detection : detections) {
            List<Double> bbox = detection.bboxXyxy();
            int x1 = bbox.get(0).intValue();
            int y1 = bbox.get(1).intValue();
            int x2 = bbox.get(2).intValue();
            int y2 = bbox.get(3).intValue();
            Color color = COLORS.getOrDefault(detection.className(), Color.GREEN);
            graphics.setColor(color);
            graphics.drawRect(x1, y1, x2 - x1, y2 - y1);
            graphics.fillRect(x1, Math.max(0, y1 - 18), 140, 18);
            graphics.setColor(Color.WHITE);
            graphics.drawString(detection.className() + " " + Math.round((detection.score() == null ? 0 : detection.score()) * 100) + "%", x1 + 4, Math.max(14, y1 - 4));
        }
        graphics.dispose();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, OUTPUT_FORMAT.getCanonicalExtension(), outputStream);
        return outputStream.toByteArray();
    }
}
