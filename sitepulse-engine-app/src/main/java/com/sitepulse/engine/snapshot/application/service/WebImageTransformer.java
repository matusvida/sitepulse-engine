package com.sitepulse.engine.snapshot.application.service;

import com.sitepulse.engine.common.domain.model.ImageFormat;
import com.sitepulse.engine.common.exception.ProcessingException;
import com.sitepulse.engine.snapshot.application.result.CameraSnapshotProfile;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.springframework.stereotype.Service;

@Service
public class WebImageTransformer {

    public TransformedImage transform(byte[] sourceBytes, CameraSnapshotProfile profile) {
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(sourceBytes));
            if (source == null) {
                throw new ProcessingException("Unsupported image payload for snapshot transformation");
            }
            BufferedImage resized = resize(source, profile);
            ImageFormat format = profile.targetFormat() == null ? ImageFormat.WEBP : profile.targetFormat();
            byte[] data = encode(resized, format.getCanonicalExtension(), profile.targetQuality());
            return new TransformedImage(data, format.getMediaType());
        } catch (IOException ex) {
            throw new ProcessingException("Failed to transform image into web snapshot", ex);
        }
    }

    BufferedImage resize(BufferedImage source, CameraSnapshotProfile profile) {
        if (source.getWidth() <= profile.targetWidth()) {
            return source;
        }
        int targetWidth = profile.targetWidth();
        int targetHeight = Math.max(1, (int) Math.round((double) source.getHeight() * targetWidth / source.getWidth()));
        int imageType = source.getType() == 0 ? BufferedImage.TYPE_INT_RGB : source.getType();
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, imageType);
        Graphics2D graphics = resized.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
            return resized;
        } finally {
            graphics.dispose();
        }
    }

    private byte[] encode(BufferedImage image, String format, int quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(format);
        if (!writers.hasNext()) {
            throw new ProcessingException("No ImageIO writer available for format: " + format);
        }
        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream)) {
            writer.setOutput(imageOutputStream);
            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            if (writeParam.canWriteCompressed()) {
                writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                String[] compressionTypes = writeParam.getCompressionTypes();
                if (compressionTypes != null && compressionTypes.length > 0) {
                    writeParam.setCompressionType(compressionTypes[0]);
                }
                writeParam.setCompressionQuality(normalizeQuality(quality));
            }
            writer.write(null, new IIOImage(image, null, null), writeParam);
            imageOutputStream.flush();
            return outputStream.toByteArray();
        } finally {
            writer.dispose();
        }
    }

    private float normalizeQuality(int quality) {
        return Math.max(0.0f, Math.min(1.0f, quality / 100.0f));
    }

    public record TransformedImage(byte[] bytes, String mediaType) {
    }
}
