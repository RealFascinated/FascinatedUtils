package cc.fascinated.fascinatedutils.client;

import cc.fascinated.fascinatedutils.FascinatedUtils;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.ClientStartedEvent;
import com.mojang.blaze3d.platform.NativeImage;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public enum ModUiTextures {
    CLOSE("textures/ui/mod_settings/close.png", 64, 64), BACK("textures/ui/mod_settings/back.png", 64, 64), RESET("textures/ui/mod_settings/reset.png", 64, 64), TRASH("textures/ui/mod_settings/trash.png", 64, 64), CHECK("textures/ui/mod_settings/check.png", 64, 64);

    private static final Identifier ATLAS_TEXTURE_ID = Identifier.fromNamespaceAndPath(FascinatedUtils.MOD_ID, "ui/mod_settings/chrome-atlas");
    private static final ConcurrentHashMap<Identifier, AtlasRegion> ICON_REGIONS = new ConcurrentHashMap<>();
    private final Identifier id;
    private final int sourceWidthPx;
    private final int sourceHeightPx;

    ModUiTextures(String resourcePath, int sourceWidthPx, int sourceHeightPx) {
        this.id = Identifier.fromNamespaceAndPath(FascinatedUtils.MOD_ID, resourcePath);
        this.sourceWidthPx = sourceWidthPx;
        this.sourceHeightPx = sourceHeightPx;
    }

    /**
     * Registers a resource reload listener so every {@link ModUiTextures} entry is re-registered after pack changes.
     */
    @SuppressWarnings("deprecation")
    public static void registerResourceReloadListener() {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public @NonNull Identifier getFabricId() {
                return Identifier.fromNamespaceAndPath(FascinatedUtils.MOD_ID, "mod_ui_textures");
            }

            @Override
            public void onResourceManagerReload(ResourceManager resourceManager) {
                ModUiTextures.reloadAll(resourceManager);
            }
        });
    }

    /**
     * Register or replace GPU textures for all chrome entries from the resource pack.
     *
     * @param resourceManager active client resource manager during reload
     */
    public static void reloadAll(ResourceManager resourceManager) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }
        TextureManager textureManager = client.getTextureManager();
        try {
            registerAtlas(textureManager, resourceManager);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to register mod UI texture atlas", exception);
        }
    }

    /**
     * Draw a registered mod UI texture with a packed ARGB tint, using the matching constant's source dimensions.
     *
     * @param graphics  draw context for the current GUI frame
     * @param textureId texture previously registered by {@link #reloadAll}
     * @param x         destination left in screen pixels
     * @param y         destination top in screen pixels
     * @param width     destination width in screen pixels
     * @param height    destination height in screen pixels
     * @param tintArgb  packed ARGB tint (including alpha)
     */
    public static void drawTinted(GuiGraphicsExtractor graphics, Identifier textureId, int x, int y, int width, int height, int tintArgb) {
        AtlasRegion region = ICON_REGIONS.get(textureId);
        if (region == null) {
            return;
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, ATLAS_TEXTURE_ID, x, y, region.uPx(), region.vPx(), width, height, region.regionWidthPx(), region.regionHeightPx(), region.atlasWidthPx(), region.atlasHeightPx(), tintArgb);
    }

    @EventHandler
    private static void fascinatedutils$reloadOnClientStarted(ClientStartedEvent event) {
        reloadAll(event.minecraftClient().getResourceManager());
    }

    private static void registerAtlas(TextureManager textureManager, ResourceManager resourceManager) throws IOException {
        List<LoadedSprite> loadedSprites = new ArrayList<>();
        int atlasWidthPx = 0;
        int atlasHeightPx = 1;
        for (ModUiTextures texture : values()) {
            LoadedSprite sprite = loadSprite(resourceManager, texture);
            loadedSprites.add(sprite);
            atlasWidthPx += sprite.widthPx();
            atlasHeightPx = Math.max(atlasHeightPx, sprite.heightPx());
        }
        NativeImage atlasImage = new NativeImage(atlasWidthPx, atlasHeightPx, true);
        int penX = 0;
        ICON_REGIONS.clear();
        for (LoadedSprite sprite : loadedSprites) {
            blitSpriteIntoAtlas(sprite.image(), atlasImage, penX);
            ICON_REGIONS.put(sprite.texture().id, new AtlasRegion(penX, 0, sprite.widthPx(), sprite.heightPx(), atlasWidthPx, atlasHeightPx));
            penX += sprite.widthPx();
            sprite.image().close();
        }
        textureManager.register(ATLAS_TEXTURE_ID, new DynamicTexture(ATLAS_TEXTURE_ID::toString, atlasImage));
    }

    private static LoadedSprite loadSprite(ResourceManager resourceManager, ModUiTextures texture) throws IOException {
        try (InputStream stream = resourceManager.getResourceOrThrow(texture.id).open()) {
            NativeImage image = NativeImage.read(stream);
            int widthPx = Math.max(1, image.getWidth());
            int heightPx = Math.max(1, image.getHeight());
            return new LoadedSprite(texture, image, widthPx, heightPx);
        }
    }

    private static void blitSpriteIntoAtlas(NativeImage sprite, NativeImage atlas, int destinationX) {
        for (int y = 0; y < sprite.getHeight(); y++) {
            for (int x = 0; x < sprite.getWidth(); x++) {
                atlas.setPixel(destinationX + x, y, sprite.getPixel(x, y));
            }
        }
    }

    private record LoadedSprite(ModUiTextures texture, NativeImage image, int widthPx, int heightPx) {}

    private record AtlasRegion(int uPx, int vPx, int regionWidthPx, int regionHeightPx, int atlasWidthPx,
                               int atlasHeightPx) {}
}
