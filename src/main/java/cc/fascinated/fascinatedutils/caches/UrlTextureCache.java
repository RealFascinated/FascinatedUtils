package cc.fascinated.fascinatedutils.caches;

import cc.fascinated.fascinatedutils.AlumiteMod;
import cc.fascinated.fascinatedutils.common.AlumiteEnvironment;
import cc.fascinated.fascinatedutils.common.LRUCache;
import cc.fascinated.fascinatedutils.gui2.core.PixelSize;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class UrlTextureCache {

    public static final UrlTextureCache INSTANCE = new UrlTextureCache();
    private static final Logger LOGGER = LoggerFactory.getLogger(UrlTextureCache.class);
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    private static final int MAX_TEXTURES = 200;
    private static final long MAX_DOWNLOAD_BYTES = 8 * 1024 * 1024; // 8 MB

    private final Set<String> inFlight = ConcurrentHashMap.newKeySet();
    private final Set<String> failed = ConcurrentHashMap.newKeySet();
    private final Map<String, String> hashCache = new ConcurrentHashMap<>();
    private final Map<String, PixelSize> textureSizes = new ConcurrentHashMap<>();

    // LRU map — must only be accessed on the main thread
    private final LRUCache<String, Identifier> loaded = new LRUCache<>(MAX_TEXTURES, (id, identifier) -> {
        textureSizes.remove(id);
        Minecraft.getInstance().getTextureManager().release(identifier);
    });

    /**
     * Returns the registered texture {@link Identifier} for the given URL, or {@code null} if not yet loaded.
     *
     * <p>When {@code null} is returned a background download is initiated and {@code onLoad} is invoked on the
     * Minecraft main thread once the texture has been registered.
     *
     * <p>Must be called on the main thread.
     *
     * @param url    direct download URL for the image
     * @param onLoad callback executed on the main thread when the texture becomes available; may be {@code null}
     * @return cached identifier, or {@code null} while still loading
     */
    public Identifier get(String url, Runnable onLoad) {
        String id = hashOf(url);
        Identifier cached = loaded.get(id);
        if (cached != null) {
            return cached;
        }
        if (failed.contains(id)) {
            return null;
        }
        if (inFlight.add(id)) {
            AlumiteMod.SCHEDULED_POOL.execute(() -> download(id, url, onLoad));
        }
        return null;
    }

    /**
     * Returns the registered texture {@link Identifier} for the given local file path, or {@code null} if not yet
     * loaded.
     *
     * <p>Uses the absolute path string as the cache key. Kicks off a background read on first call and invokes
     * {@code onLoad} on the Minecraft main thread once the texture is ready.</p>
     *
     * @param path   local file to load
     * @param onLoad optional callback executed on the main thread when the texture becomes available
     * @return cached identifier, or {@code null} while still loading
     */
    public Identifier getLocal(Path path, Runnable onLoad) {
        String id = "local:" + path.toAbsolutePath();
        Identifier cached = loaded.get(id);
        if (cached != null) {
            return cached;
        }
        if (failed.contains(id)) {
            return null;
        }
        if (inFlight.add(id)) {
            AlumiteMod.SCHEDULED_POOL.execute(() -> loadLocalFile(id, path, onLoad));
        }
        return null;
    }

    /**
     * Returns the pixel dimensions {@code [width, height]} of a previously loaded local file, or
     * {@code null} if the texture has not been loaded yet.
     *
     * <p>Must be called on the main thread.
     *
     * @param path local file path
     * @return pixel dimensions, or {@code null} if not yet available
     */
    public PixelSize getLocalSizePixels(Path path) {
        return textureSizes.get("local:" + path.toAbsolutePath());
    }

    /**
     * Returns the pixel dimensions {@code [width, height]} of a previously downloaded URL texture, or
     * {@code null} if not yet loaded.
     *
     * <p>Must be called on the main thread.
     *
     * @param url texture URL
     * @return pixel dimensions, or {@code null} if not yet available
     */
    public PixelSize getSizePixels(String url) {
        return textureSizes.get(hashOf(url));
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

    private void loadLocalFile(String id, Path path, Runnable onLoad) {
        NativeImage image = null;
        try {
            byte[] bytes = Files.readAllBytes(path);
            if (bytes.length > MAX_DOWNLOAD_BYTES) {
                LOGGER.warn("Local attachment {} exceeds size limit ({} bytes), skipping", path, bytes.length);
                failed.add(id);
                return;
            }
            image = NativeImage.read(bytes);
            final NativeImage finalImage = image;
            Minecraft.getInstance().execute(() -> {
                try {
                    String safePath = "local-attachment/" + Integer.toHexString(id.hashCode() & Integer.MAX_VALUE);
                    Identifier identifier = Identifier.fromNamespaceAndPath(AlumiteMod.MOD_ID, safePath);
                    DynamicTexture texture = new DynamicTexture(identifier::toString, finalImage);
                    texture.upload();
                    textureSizes.put(id, new PixelSize(finalImage.getWidth(), finalImage.getHeight()));
                    finalImage.close();
                    Minecraft.getInstance().getTextureManager().register(identifier, texture);
                    loaded.put(id, identifier);
                    if (onLoad != null) {
                        onLoad.run();
                    }
                } finally {
                    inFlight.remove(id);
                }
            });
            image = null;
        } catch (IOException exception) {
            failed.add(id);
            LOGGER.error("Failed to load local attachment {}", path, exception);
        } finally {
            if (image != null) {
                image.close();
            }
            if (failed.contains(id)) {
                inFlight.remove(id);
            }
        }
    }

    private void download(String id, String url, Runnable onLoad) {
        NativeImage image = null;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", AlumiteEnvironment.USER_AGENT)
                    .GET()
                    .build();
            HttpResponse<byte[]> response = HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                LOGGER.warn("Failed to fetch attachment {}: HTTP {}", id, response.statusCode());
                failed.add(id);
                return;
            }
            byte[] bytes = response.body();
            if (bytes.length > MAX_DOWNLOAD_BYTES) {
                LOGGER.warn("Attachment {} exceeds size limit ({} bytes), skipping", id, bytes.length);
                failed.add(id);
                return;
            }
            image = NativeImage.read(bytes);
            final NativeImage finalImage = image;
            Minecraft.getInstance().execute(() -> {
                try {
                    Identifier identifier = Identifier.fromNamespaceAndPath(AlumiteMod.MOD_ID, "attachment/" + id);
                    DynamicTexture texture = new DynamicTexture(identifier::toString, finalImage);
                    texture.upload();
                    textureSizes.put(id, new PixelSize(finalImage.getWidth(), finalImage.getHeight()));
                    finalImage.close(); // drop RAM copy now that it's in VRAM
                    Minecraft.getInstance().getTextureManager().register(identifier, texture);
                    loaded.put(id, identifier);
                    if (onLoad != null) {
                        onLoad.run();
                    }
                } finally {
                    inFlight.remove(id);
                }
            });
            image = null; // ownership transferred to main thread block above
        } catch (IOException | InterruptedException exception) {
            failed.add(id);
            LOGGER.error("Failed to download attachment {}", id, exception);
        } finally {
            if (image != null) {
                image.close(); // decode succeeded but execute() was never reached — free native memory
            }
            // Only remove inFlight here on the failure path; success path removes it on the main thread
            if (failed.contains(id)) {
                inFlight.remove(id);
            }
        }
    }
}