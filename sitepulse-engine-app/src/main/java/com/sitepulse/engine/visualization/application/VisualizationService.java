package com.sitepulse.engine.visualization.application;

import com.sitepulse.engine.common.util.JsonUtils;
import com.sitepulse.engine.detection.domain.DetectionEntity;
import com.sitepulse.engine.detection.domain.ImageEntity;
import com.sitepulse.engine.detection.persistence.DetectionRepository;
import com.sitepulse.engine.detection.persistence.ImageRepository;
import com.sitepulse.engine.integration.storage.StorageService;
import com.sitepulse.engine.project.application.ProjectService;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;

@Service
public class VisualizationService {

    private static final Map<String, Color> COLORS = Map.of(
            "person", new Color(0, 120, 255),
            "car", new Color(255, 80, 0),
            "truck", new Color(255, 0, 80),
            "bus", new Color(200, 0, 200)
    );

    private final ProjectService projectService;
    private final ImageRepository imageRepository;
    private final DetectionRepository detectionRepository;
    private final StorageService storageService;
    private final JsonUtils jsonUtils;

    public VisualizationService(
            ProjectService projectService,
            ImageRepository imageRepository,
            DetectionRepository detectionRepository,
            StorageService storageService,
            JsonUtils jsonUtils
    ) {
        this.projectService = projectService;
        this.imageRepository = imageRepository;
        this.detectionRepository = detectionRepository;
        this.storageService = storageService;
        this.jsonUtils = jsonUtils;
    }

    public Map<String, Object> visualize(Integer projectId, LocalDate dateFrom, LocalDate dateTo) {
        projectService.requireProject(projectId);
        List<ImageEntity> images = imageRepository.findDoneInRange(
                projectId,
                dateFrom.atStartOfDay().atOffset(ZoneOffset.UTC),
                dateTo.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC)
        ).stream().filter(image -> !detectionRepository.findByImageId(image.getId()).isEmpty()).toList();

        int processed = 0;
        List<String> errors = new ArrayList<>();
        for (ImageEntity image : images) {
            try {
                byte[] original = storageService.download(image.getBucket(), image.getKey());
                byte[] annotated = drawDetections(original, detectionRepository.findByImageId(image.getId()));
                storageService.upload(image.getBucket(), "detection/" + image.getKey(), annotated, "image/jpeg");
                processed++;
            } catch (Exception ex) {
                errors.add(image.getKey() + ": " + ex.getMessage());
            }
        }
        return Map.of("imagesFound", images.size(), "imagesProcessed", processed, "errors", errors);
    }

    private byte[] drawDetections(byte[] bytes, List<DetectionEntity> detections) throws Exception {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        Graphics2D graphics = image.createGraphics();
        graphics.setStroke(new BasicStroke(2));
        graphics.setFont(new Font("SansSerif", Font.BOLD, 14));
        for (DetectionEntity detection : detections) {
            List<Double> bbox = jsonUtils.readDoubleList(detection.getBboxXyxy());
            int x1 = bbox.get(0).intValue();
            int y1 = bbox.get(1).intValue();
            int x2 = bbox.get(2).intValue();
            int y2 = bbox.get(3).intValue();
            Color color = COLORS.getOrDefault(detection.getClassName(), Color.GREEN);
            graphics.setColor(color);
            graphics.drawRect(x1, y1, x2 - x1, y2 - y1);
            graphics.fillRect(x1, Math.max(0, y1 - 18), 140, 18);
            graphics.setColor(Color.WHITE);
            graphics.drawString(detection.getClassName() + " " + Math.round((detection.getScore() == null ? 0 : detection.getScore()) * 100) + "%", x1 + 4, Math.max(14, y1 - 4));
        }
        graphics.dispose();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", outputStream);
        return outputStream.toByteArray();
    }
}
