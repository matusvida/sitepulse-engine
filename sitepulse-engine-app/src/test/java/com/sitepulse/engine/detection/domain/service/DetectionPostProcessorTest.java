package com.sitepulse.engine.detection.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitepulse.engine.common.domain.enums.ImageFormat;
import com.sitepulse.engine.config.SitePulseProperties;
import com.sitepulse.engine.detection.domain.model.CameraRoiSettings;
import com.sitepulse.engine.detection.domain.model.DetectionInference;
import com.sitepulse.engine.detection.domain.model.DetectionOutcome;
import com.sitepulse.engine.detection.domain.model.RawDetection;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetectionPostProcessorTest {

    @Test
    void processDropsDetectionsOutsideRoiWhenConfigured() {
        DetectionPostProcessor processor = new DetectionPostProcessor(testProperties(), new ObjectMapper());
        CameraRoiSettings cameraSettings = new CameraRoiSettings(
                List.of(
                        List.of(0.0, 0.0),
                        List.of(50.0, 0.0),
                        List.of(50.0, 50.0),
                        List.of(0.0, 50.0)
                ),
                true,
                100,
                100
        );
        DetectionInference inference = new DetectionInference(
                "gpt-4.1",
                100,
                100,
                15.0,
                "overcast",
                List.of(
                        new RawDetection(1, "worker", 0.95, List.of(10.0, 10.0, 20.0, 20.0), null, "yellow", "inside roi"),
                        new RawDetection(1, "worker", 0.95, List.of(70.0, 70.0, 80.0, 80.0), null, "orange", "outside roi")
                )
        );

        DetectionOutcome outcome = processor.process("bucket", "key", imageBytes(100, 100), inference, cameraSettings);

        assertEquals(1, outcome.detections().size());
        assertEquals(List.of(10.0, 10.0, 20.0, 20.0), outcome.detections().getFirst().bboxXyxy());
        assertEquals(Boolean.TRUE, outcome.detections().getFirst().inRoi());
        assertEquals("overcast", outcome.weatherNote());
        assertTrue(outcome.warnings().contains("1 detections outside ROI"));
    }

    private static byte[] imageBytes(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, 0x808080);
            }
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, ImageFormat.PNG.getCanonicalExtension(), out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to build test image", ex);
        }
    }

    private static SitePulseProperties testProperties() {
        return new SitePulseProperties(
                "postgresql://sitepulse:sitepulse@localhost:5432/sitepulse",
                "http://localhost:3000",
                "minio",
                "tower-tl",
                60,
                new SitePulseProperties.StorageProperties(
                        "http://minio:9000",
                        "http://localhost:9001",
                        "admin",
                        "password123",
                        "us-east-1"
                ),
                "yolov8x.pt",
                0.35,
                "{}",
                25.0,
                false,
                "roi_config.json",
                0.0,
                0,
                255,
                false,
                "0 0/10 * * * *",
                "0 5/10 * * * *",
                "openai",
                "0 0 2 * * *",
                3,
                "",
                "",
                "",
                "",
                "test-key",
                "gpt-4.1",
                52_428_800L,
                "http://python-yolo:8000",
                new SitePulseProperties.ImageWebSnapshotsProperties(false, 1920, 75, ImageFormat.WEBP, java.time.LocalTime.of(17, 0)),
                new SitePulseProperties.AuthProperties(
                        "http://localhost:3000",
                        "sitepulse_session",
                        false,
                        "Lax",
                        168,
                        72,
                        1,
                        null,
                        null,
                        new SitePulseProperties.MailProperties(
                                true,
                                "SitePulse",
                                "SitePulse <noreply@example.com>",
                                null,
                                new SitePulseProperties.ResendProperties(
                                        true,
                                        "https://api.resend.com",
                                        "re_test"
                                )
                        )
                )
        );
    }
}
