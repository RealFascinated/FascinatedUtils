package cc.fascinated.fascinatedutils.caches;

import cc.fascinated.fascinatedutils.AlumiteMod;
import cc.fascinated.fascinatedutils.common.AlumiteEnvironment;
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
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class UrlTextureCache {

    public static final UrlTextureCache INSTANCE = new UrlTextureCache();
    private static final Logger LOGGER = LoggerFactory.getLogger(UrlTextureCache.class);
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    private static final int MAX_TEXTURES = 200;
    private static final long MAX_DOWNLOAD_BYTES = 8 * 1024 * 1024; // 8 MB

    // LRU map — must only be accessed on the main thread
    private final Map<String, Identifier> loaded = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Identifier> eldest) {
            if (size() > MAX_TEXTURES) {
                Minecraft.getInstance().getTextureManager().release(eldest.getValue());
                return true;
            }
            return false;
        }
    };

    private final Set<String> inFlight = ConcurrentHashMap.newKeySet();
    private final Set<String> failed = ConcurrentHashMap.newKeySet();

    /**
     * Returns the registered texture {@link Identifier} for the given URL, or {@code null} if not yet loaded.
     *
     * <p>When {@code null} is returned a background download is initiated and {@code onLoad} is invoked on the
     * Minecraft main thread once the texture has been registered.
     *
     * <p>Must be called on the main thread.
     *
     * @param id     cache key and texture path (e.g. a content-addressed hash)
     * @param url    direct download URL for the image
     * @param onLoad callback executed on the main thread when the texture becomes available; may be {@code null}
     * @return cached identifier, or {@code null} while still loading
     */
    public Identifier get(String id, String url, Runnable onLoad) {
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