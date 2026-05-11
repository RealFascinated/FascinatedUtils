package cc.fascinated.fascinatedutils.common;

import lombok.experimental.UtilityClass;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Iterator;

@UtilityClass
public class ImageUtils {

    /**
     * Compresses an image to WebP format.
     *
     * @param bytes the raw image bytes in any format readable by {@link ImageIO}
     * @param level lossy quality in {@code [0.0, 1.0]} where {@code 1.0} is highest quality
     * @return WebP-encoded bytes
     * @throws IllegalArgumentException if the input cannot be decoded as an image
     * @throws IllegalStateException    if no WebP writer is available on the classpath
     */
    public static byte[] compress(byte[] bytes, float level) {
        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (Exception exception) {
            throw new IllegalArgumentException("Failed to decode image", exception);
        }
        if (image == null) {
            throw new IllegalArgumentException("Unrecognized image format");
        }

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType("image/webp");
        if (!writers.hasNext()) {
            throw new IllegalStateException("No WebP ImageWriter found; ensure webp-imageio is on the classpath");
        }
        ImageWriter writer = writers.next();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (MemoryCacheImageOutputStream imageOutput = new MemoryCacheImageOutputStream(output)) {
            writer.setOutput(imageOutput);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                // WebPWriteParam requires a type to be selected before quality can be set.
                // Type 0 is lossy compression.
                String[] types = param.getCompressionTypes();
                if (types != null && types.length > 0) {
                    param.setCompressionType(types[0]);
                }
                param.setCompressionQuality(level);
            }
            writer.write(null, new IIOImage(image, null, null), param);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to encode image as WebP", exception);
        } finally {
            writer.dispose();
        }

        return output.toByteArray();
    }
}
