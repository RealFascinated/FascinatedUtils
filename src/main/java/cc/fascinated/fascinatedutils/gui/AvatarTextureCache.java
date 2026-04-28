package cc.fascinated.fascinatedutils.gui;

import cc.fascinated.fascinatedutils.FascinatedUtils;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AvatarTextureCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(AvatarTextureCache.class);

    public static final AvatarTextureCache INSTANCE = new AvatarTextureCache();

    private static final String AVATAR_URL = "https://mc-heads.net/avatar/%s/32";
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ConcurrentHashMap<String, Identifier> loaded = new ConcurrentHashMap<>();
    private final Set<String> inFlight = ConcurrentHashMap.newKeySet();
    private final Set<String> failed = ConcurrentHashMap.newKeySet();

    /**
     * Returns the registered texture {@link Identifier} for the given player UUID, or {@code null} if not yet loaded.
     *
     * <p>When {@code null} is returned a background download is initiated and {@code onLoad} is invoked on the
     * Minecraft main thread once the texture has been registered.
     *
     * @param uuid   Minecraft player UUID string
     * @param onLoad callback executed on the main thread when the texture becomes available; may be {@code null}
     * @return cached identifier, or {@code null} while still loading
     */
    public Identifier get(String uuid, Runnable onLoad) {
        Identifier cached = loaded.get(uuid);
        if (cached != null) {
            return cached;
        }
        if (failed.contains(uuid)) {
            return null;
        }
        if (inFlight.add(uuid)) {
            FascinatedUtils.SCHEDULED_POOL.execute(() -> download(uuid, onLoad));
        }
        return null;
    }

    private void download(String uuid, Runnable onLoad) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(String.format(AVATAR_URL, uuid)))
                    .timeout(Duration.ofSeconds(5))
                    .header("User-Agent", AlumiteEnvironment.USER_AGENT)
                    .GET()
                    .build();
            HttpResponse<byte[]> response = HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                LOGGER.warn("Failed to fetch avatar for {}: HTTP {}", uuid, response.statusCode());
                failed.add(uuid);
                inFlight.remove(uuid);
                return;
            }
            NativeImage image = NativeImage.read(response.body());
            Identifier identifier = Identifier.fromNamespaceAndPath(FascinatedUtils.MOD_ID, "avatar/" + uuid);
            NativeImage finalImage = image;
            Minecraft.getInstance().execute(() -> {
                DynamicTexture texture = new DynamicTexture(identifier::toString, finalImage);
                texture.upload();
                Minecraft.getInstance().getTextureManager().register(identifier, texture);
                loaded.put(uuid, identifier);
                inFlight.remove(uuid);
                if (onLoad != null) {
                    onLoad.run();
                }
            });
        } catch (IOException | InterruptedException exception) {
            failed.add(uuid);
            inFlight.remove(uuid);
            LOGGER.error("Failed to download avatar for {}", uuid, exception);
        }
    }
}
