package cc.fascinated.fascinatedutils.systems;

import cc.fascinated.fascinatedutils.AlumiteMod;
import cc.fascinated.fascinatedutils.Constants;
import cc.fascinated.fascinatedutils.common.AlumiteEnvironment;
import cc.fascinated.fascinatedutils.gui2.core.PixelSize;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
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
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    private static final long MAX_DOWNLOAD_BYTES = 10 * 1024 * 1024;
    // Decoded images take width * height * 4 bytes of native memory plus VRAM; 64M pixels is about 256 MB.
    private static final long MAX_IMAGE_PIXELS = 64L * 1024 * 1024;
    private static final long FAILURE_RETRY_MILLIS = TimeUnit.MINUTES.toMillis(1);

    private final Set<String> inFlight = ConcurrentHashMap.newKeySet();
    // Failed id -> time of failure; a failed texture is retried after FAILURE_RETRY_MILLIS.
    private final Map<String, Long> failedAtMillis = new ConcurrentHashMap<>();
    private final Map<String, String> hashCache = new ConcurrentHashMap<>();

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
     * Returns the registered texture {@link Identifier} for the given URL, or {@code null} if not yet loaded.
     *
     * <p>When {@code null} is returned a background download is initiated and {@code onLoad} is invoked on the
     * Minecraft main thread once the texture has been registered.
     *
     * <p>Must be called on the main thread.
     *
     * @param url       direct download URL for the image
     * @param maxHeight maximum pixel height to downscale to while preserving aspect ratio; {@code 0} disables
     * @param onLoad    callback executed on the main thread when the texture becomes available; may be {@code null}
     * @return cached identifier, or {@code null} while still loading
     */
    public LoadedTexture get(String url, int maxHeight, Runnable onLoad) {
        String id = hashOf(url) + (maxHeight > 0 ? "@" + maxHeight : "");
        return getOrSchedule(id, () -> download(id, url, maxHeight, onLoad));
    }

    /**
     * Returns the registered texture {@link Identifier} for the given URL, or {@code null} if not yet loaded.
     *
     * <p>Must be called on the main thread.
     *
     * @param url    direct download URL for the image
     * @param onLoad callback executed on the main thread when the texture becomes available; may be {@code null}
     * @return cached identifier, or {@code null} while still loading
     */
    public LoadedTexture get(String url, Runnable onLoad) {
        return get(url, 0, onLoad);
    }

    /**
     * Returns the registered texture {@link Identifier} for the given local file path, or {@code null} if not yet
     * loaded.
     *
     * <p>Uses the absolute path string as the cache key. Kicks off a background read on first call and invokes
     * {@code onLoad} on the Minecraft main thread once the texture is ready.</p>
     *
     * @param path      local file to load
     * @param maxHeight maximum pixel height to downscale to while preserving aspect ratio; {@code 0} disables
     * @param onLoad    optional callback executed on the main thread when the texture becomes available
     * @return cached identifier, or {@code null} while still loading
     */
    public LoadedTexture getLocal(Path path, int maxHeight, Runnable onLoad) {
        String id = "local:" + path.toAbsolutePath() + (maxHeight > 0 ? "@" + maxHeight : "");
        return getOrSchedule(id, () -> loadLocalFile(id, path, maxHeight, onLoad));
    }

    /**
     * Returns the registered texture {@link Identifier} for the given local file path, or {@code null} if not yet
     * loaded.
     *
     * <p>Must be called on the main thread.
     *
     * @param path   local file to load
     * @param onLoad optional callback executed on the main thread when the texture becomes available
     * @return cached identifier, or {@code null} while still loading
     */
    public LoadedTexture getLocal(Path path, Runnable onLoad) {
        return getLocal(path, 0, onLoad);
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

    private String hashOf(String url) {
        return hashCache.computeIfAbsent(url, key -> {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] bytes = digest.digest(key.getBytes(StandardCharsets.UTF_8));
                StringBuilder hex = new StringBuilder(64);
                for (byte b : bytes) {
                    hex.append(String.format("%02x", b));
                }
                return hex.toString();
            } catch (NoSuchAlgorithmException exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    /**
     * Registers a decoded {@link NativeImage} as a {@link DynamicTexture} on the main thread's texture manager.
     * Closes the image after upload, stores the size, and fires {@code onLoad}.
     *
     * <p>Must be called on the main thread.
     */
    private void registerTexture(String id, Identifier identifier, NativeImage image, long byteSize, Runnable onLoad) {
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
        textures.put(id, new LoadedTexture(id, identifier, byteSize, new PixelSize(width, height)));
        if (onLoad != null) {
            onLoad.run();
        }
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

    private void loadLocalFile(String id, Path path, int maxHeight, Runnable onLoad) {
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
                    registerTexture(id, identifier, finalImage, (long) finalImage.getWidth() * finalImage.getHeight() * finalImage.format().components(), onLoad);
                } catch (RuntimeException exception) {
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

    private void download(String id, String url, int maxHeight, Runnable onLoad) {
        NativeImage image = null;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", AlumiteEnvironment.USER_AGENT)
                    .GET()
                    .build();
            HttpResponse<InputStream> response = null;
            for (int attempt = 0; attempt <= 3; attempt++) {
                response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() != 429) {
                    break;
                }
                if (attempt == 3) {
                    break;
                }
                response.body().close();
                long retryAfterMillis = response.headers().firstValue("Retry-After")
                        .map(value -> {
                            try {
                                return Long.parseLong(value) * 1000L;
                            } catch (NumberFormatException ignored) {
                                return 1000L;
                            }
                        })
                        .orElse(1000L);
                LOGGER.warn("Attachment {} rate-limited (429), retrying in {}ms (attempt {}/3)", id, retryAfterMillis, attempt + 1);
                Thread.sleep(retryAfterMillis);
            }
            if (response.statusCode() != 200) {
                response.body().close();
                LOGGER.warn("Failed to fetch attachment {}: HTTP {}", id, response.statusCode());
                markFailed(id);
                return;
            }
            if (response.headers().firstValueAsLong("Content-Length").orElse(-1L) > MAX_DOWNLOAD_BYTES) {
                response.body().close();
                LOGGER.warn("Attachment {} exceeds size limit, skipping", id);
                markFailed(id);
                return;
            }
            // Read at most one byte past the limit so an oversized body is never fully buffered.
            byte[] bytes;
            try (InputStream body = response.body()) {
                bytes = body.readNBytes((int) MAX_DOWNLOAD_BYTES + 1);
            }
            if (bytes.length > MAX_DOWNLOAD_BYTES) {
                LOGGER.warn("Attachment {} exceeds size limit ({} bytes), skipping", id, MAX_DOWNLOAD_BYTES);
                markFailed(id);
                return;
            }
            requireWithinPixelBudget(bytes);
            bytes = toPngIfNeeded(bytes);
            image = NativeImage.read(bytes);
            image = downscaleIfNeeded(image, maxHeight);
            final NativeImage finalImage = image;
            // Weigh by decoded size (what actually occupies native memory and VRAM), not by the compressed download.
            long decodedBytes = (long) finalImage.getWidth() * finalImage.getHeight() * finalImage.format().components();
            Minecraft.getInstance().execute(() -> {
                try {
                    Identifier identifier = Identifier.fromNamespaceAndPath(AlumiteMod.MOD_ID, "attachment/" + id);
                    registerTexture(id, identifier, finalImage, decodedBytes, onLoad);
                } catch (RuntimeException exception) {
                    markFailed(id);
                    LOGGER.error("Failed to register attachment {}", id, exception);
                } finally {
                    inFlight.remove(id);
                }
            });
            image = null;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            markFailed(id);
            LOGGER.error("Interrupted while downloading attachment {}", id, exception);
        } catch (IOException | RuntimeException exception) {
            markFailed(id);
            LOGGER.error("Failed to download attachment {}", id, exception);
        } finally {
            cleanupBackgroundTask(id, image);
        }
    }

    private byte[] toPngIfNeeded(byte[] bytes) throws IOException {
        if (bytes.length >= 4 && bytes[0] == (byte) 0x89 && bytes[1] == (byte) 0x50 && bytes[2] == (byte) 0x4E && bytes[3] == (byte) 0x47) {
            return bytes;
        }
        BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(bytes));
        if (bufferedImage == null) {
            throw new IOException("Unsupported or unreadable image format");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", out);
        return out.toByteArray();
    }

    private void cleanupBackgroundTask(String id, NativeImage image) {
        if (image != null) {
            image.close();
        }
        if (failedAtMillis.containsKey(id)) {
            inFlight.remove(id);
        }
    }

    public record LoadedTexture(String cacheKey, Identifier id, long size, PixelSize pixelSize) {}
}