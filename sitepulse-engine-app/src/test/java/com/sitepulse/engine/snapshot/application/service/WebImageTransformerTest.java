package com.sitepulse.engine.snapshot.application.service;

import com.sitepulse.engine.common.domain.enums.ImageFormat;
import com.sitepulse.engine.snapshot.application.result.CameraSnapshotProfile;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebImageTransformerTest {

    @Test
    void scalesDownLargerImagesAndEncodesWebp() throws Exception {
        WebImageTransformer transformer = new WebImageTransformer();
        CameraSnapshotProfile profile = new CameraSnapshotProfile(1, 1920, 75, ImageFormat.WEBP, java.time.LocalTime.of(17, 0));
        BufferedImage resized = transformer.resize(bufferedImage(3000, 1500), profile);

        WebImageTransformer.TransformedImage transformed = transformer.transform(imageBytes(3000, 1500), profile);

        assertEquals(1920, resized.getWidth());
        assertEquals(960, resized.getHeight());
        assertEquals(ImageFormat.WEBP.getMediaType(), transformed.mediaType());
        assertTrue(transformed.bytes().length > 0);
        assertEquals("RIFF", new String(Arrays.copyOfRange(transformed.bytes(), 0, 4), StandardCharsets.US_ASCII));
        assertEquals("WEBP", new String(Arrays.copyOfRange(transformed.bytes(), 8, 12), StandardCharsets.US_ASCII));
    }

    @Test
    void leavesSmallerImagesAtOriginalWidth() throws Exception {
        WebImageTransformer transformer = new WebImageTransformer();
        CameraSnapshotProfile profile = new CameraSnapshotProfile(1, 1920, 75, ImageFormat.WEBP, java.time.LocalTime.of(17, 0));
        BufferedImage resized = transformer.resize(bufferedImage(1200, 800), profile);

        WebImageTransformer.TransformedImage transformed = transformer.transform(imageBytes(1200, 800), profile);

        assertEquals(1200, resized.getWidth());
        assertEquals(800, resized.getHeight());
        assertTrue(transformed.bytes().length > 0);
    }

    private byte[] imageBytes(int width, int height) throws Exception {
        BufferedImage image = bufferedImage(width, height);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, ImageFormat.JPEG.getCanonicalExtension(), outputStream);
        return outputStream.toByteArray();
    }

    private BufferedImage bufferedImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, 0x335577);
            }
        }
        return image;
    }
}
