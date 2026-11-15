package cc.fascinated.fascinatedutils.systems;

import cc.fascinated.fascinatedutils.AlumiteMod;
import cc.fascinated.fascinatedutils.Constants;
import cc.fascinated.fascinatedutils.gui.core.PixelSize;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;

public class TextureManager {

    public static final TextureManager INSTANCE = new TextureManager();
    private static final Logger LOGGER = LoggerFactory.getLogger(TextureManager.class);

    // Decoded images take width * height * 4 bytes of native memory plus VRAM; 64M pixels is about 256 MB.
    private static final long MAX_IMAGE_PIXELS = 64L * 1024 * 1024;
    private static final long FAILURE_RETRY_MILLIS = TimeUnit.MINUTES.toMillis(1);

    private final Set<String> inFlight = ConcurrentHashMap.newKeySet();
    // Failed id -> time of failure; a failed texture is retried after FAILURE_RETRY_MILLIS.
    private final Map<String, Long> failedAtMillis = new ConcurrentHashMap<>();

    private final Cache<String, LoadedTexture> textures = CacheBuilder.newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .maximumWeight(512 * 1024 * 1024L) // 512 MB cap
            .weigher((String _, LoadedTexture v) -> (int) Math.min(v.size(), Integer.MAX_VALUE))
            .removalListener(notification -> {
                // Skip release for REPLACED: the new texture's registerTexture() already called
                // mc.getTextureManager().register(), which closes the old DynamicTexture. Calling
                // release() here would destroy the newly registered texture, permanently breaking it.
                if (notification.getValue() != null && notification.getCause() != RemovalCause.REPLACED) {
                    Minecraft.getInstance().getTextureManager().release(notification.getValue().id());
                    inFlight.remove(notification.getKey());
                    failedAtMillis.remove(notification.getKey());
                }
            })
            .build();

    /**
     * Returns the loaded texture for the given local file path, or {@code null} if not yet loaded.
     *
     * <p>Uses the absolute path string as the cache key. Kicks off a background read on first call; the texture
     * becomes available once it is registered on the Minecraft main thread.</p>
     *
     * @param path      local file to load
     * @param maxHeight maximum pixel height to downscale to while preserving aspect ratio; {@code 0} disables
     * @return the cached texture, or {@code null} while still loading
     */
    public LoadedTexture getLocal(Path path, int maxHeight) {
        String id = "local:" + path.toAbsolutePath() + (maxHeight > 0 ? "@" + maxHeight : "");
        return getOrSchedule(id, () -> loadLocalFile(id, path, maxHeight));
    }

    private LoadedTexture getOrSchedule(String id, Runnable task) {
        LoadedTexture cached = textures.getIfPresent(id);
        if (cached != null) {
            return cached;
        }
        Long failedAt = failedAtMillis.get(id);
        if (failedAt != null) {
            if (System.currentTimeMillis() - failedAt < FAILURE_RETRY_MILLIS) {
                return null;
            }
            failedAtMillis.remove(id);
        }
        if (inFlight.add(id)) {
            Constants.EXECUTORS.execute(task);
        }
        return null;
    }

    private void markFailed(String id) {
        failedAtMillis.put(id, System.currentTimeMillis());
    }

    /**
     * Rejects an image whose decoded size would exceed {@link #MAX_IMAGE_PIXELS}, reading only its header so a
     * small file that expands to a huge bitmap is never decoded.
     *
     * @param bytes encoded image data
     * @throws IOException if the format is unsupported or the image is too large
     */
    private void requireWithinPixelBudget(byte[] bytes) throws IOException {
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = stream == null ? Collections.emptyIterator() : ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                throw new IOException("Unsupported or unreadable image format");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream);
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels > MAX_IMAGE_PIXELS) {
                    throw new IOException("Image is too large (" + reader.getWidth(0) + "x" + reader.getHeight(0) + ")");
                }
            } finally {
                reader.dispose();
            }
        }
    }

    public long loadedTextures() {
        return textures.size();
    }

    public long loadedTextureTotalSize() {
        return textures.asMap().values().stream()
                .mapToLong(LoadedTexture::size)
                .sum();
    }

    /**
     * Registers a decoded {@link NativeImage} as a {@link DynamicTexture} on the main thread's texture manager.
     * Closes the image after upload and stores the size.
     *
     * <p>Must be called on the main thread.
     */
    private void registerTexture(String id, Identifier identifier, NativeImage image, long byteSize) {
        int width = image.getWidth();
        int height = image.getHeight();
        DynamicTexture texture;
        try {
            texture = new DynamicTexture(identifier::toString, image);
            texture.upload();
        } finally {
            image.close();
        }
        Minecraft.getInstance().getTextureManager().register(identifier, texture);
        textures.put(id, new LoadedTexture(identifier, byteSize, new PixelSize(width, height)));
    }

    private NativeImage downscaleIfNeeded(NativeImage image, int maxHeight) {
        if (maxHeight <= 0 || image.getHeight() <= maxHeight) {
            return image;
        }
        int scaledWidth = Math.max(1, (int) Math.round((double) image.getWidth() * maxHeight / image.getHeight()));
        NativeImage scaled = new NativeImage(image.format(), scaledWidth, maxHeight, false);
        try {
            image.resizeSubRectTo(0, 0, image.getWidth(), image.getHeight(), scaled);
        } catch (RuntimeException exception) {
            scaled.close();
            throw exception;
        }
        image.close();
        return scaled;
    }

    private void loadLocalFile(String id, Path path, int maxHeight) {
        NativeImage image = null;
        try {
            byte[] bytes = Files.readAllBytes(path);
            requireWithinPixelBudget(bytes);
            image = NativeImage.read(bytes);
            image = downscaleIfNeeded(image, maxHeight);
            final NativeImage finalImage = image;
            Minecraft.getInstance().execute(() -> {
                try {
                    String safePath = "local-attachment/" + Integer.toHexString(id.hashCode() & Integer.MAX_VALUE);
                    Identifier identifier = Identifier.fromNamespaceAndPath(AlumiteMod.MOD_ID, safePath);
                    registerTexture(id, identifier, finalImage, (long) finalImage.getWidth() * finalImage.getHeight() * finalImage.format().components());
                } catch (RuntimeException exception) {
                    finalImage.close();
                    markFailed(id);
                    LOGGER.error("Failed to register local attachment {}", path, exception);
                } finally {
                    inFlight.remove(id);
                }
            });
            image = null;
        } catch (IOException | RuntimeException exception) {
            markFailed(id);
            LOGGER.error("Failed to load local attachment {}", path, exception);
        } finally {
            cleanupBackgroundTask(id, image);
        }
    }

    private void cleanupBackgroundTask(String id, NativeImage image) {
        if (image != null) {
            image.close();
        }
        if (failedAtMillis.containsKey(id)) {
            inFlight.remove(id);
        }
    }

    public record LoadedTexture(Identifier id, long size, PixelSize pixelSize) {}
}
