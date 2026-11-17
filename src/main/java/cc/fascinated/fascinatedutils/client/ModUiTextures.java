package cc.fascinated.fascinatedutils.client;

import cc.fascinated.fascinatedutils.AlumiteMod;
import com.mojang.blaze3d.platform.NativeImage;
import lombok.Getter;
import cc.fascinated.fascinatedutils.systems.modules.ModuleIcons;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStream;

@Slf4j
@Getter
public enum ModUiTextures {
    CLOSE("ui/close"),
    BACK("ui/back"),
    RESET("ui/reset"),
    TRASH("ui/trash"),
    CHECK("ui/check"),
    SETTINGS("ui/settings"),
    CHEVRON_RIGHT("ui/keyboard_arrow_right"),
    CHEVRON_LEFT("ui/keyboard_arrow_left"),
    EDIT("ui/edit"),
    SUNNY("ui/sunny"),
    ADD("ui/add"),
    WARNING("ui/warning"),
    COPY("ui/copy"),
    IMAGE("ui/image"),
    SAVE("ui/save"),
    ;

    private final Identifier id;

    ModUiTextures(String spritePath) {
        this.id = Identifier.fromNamespaceAndPath(AlumiteMod.MOD_ID, spritePath);
    }

    /**
     * Pre-load every UI sprite as a standalone {@link DynamicTexture} registered in the
     * {@link net.minecraft.client.renderer.texture.TextureManager} under its sprite identifier.
     *
     * <p>Must be called on the Minecraft main thread after the render device is initialised
     * (e.g. from the {@code CLIENT_STARTED} lifecycle event). Once registered,
     * {@code Renderer2D.drawTexture} finds the texture via {@code TextureManager.getTexture} and
     * draws it through the linear-sampled mesh path.
     *
     * @param mc the active Minecraft instance
     */
    public static void loadTextures(Minecraft mc) {
        for (ModUiTextures sprite : values()) {
            registerSprite(mc, sprite.id);
        }
        ModuleIcons.INSTANCE.loadTextures(mc);
    }

    /**
     * Loads one sprite PNG as a standalone texture registered under its own identifier.
     *
     * @param mc       the active Minecraft instance
     * @param spriteId sprite identifier whose PNG lives under {@code textures/gui/sprites/}
     */
    public static void registerSprite(Minecraft mc, Identifier spriteId) {
        Identifier resourceId = Identifier.fromNamespaceAndPath(
                AlumiteMod.MOD_ID, "textures/gui/sprites/" + spriteId.getPath() + ".png");
        try {
            Resource resource = mc.getResourceManager().getResourceOrThrow(resourceId);
            try (InputStream stream = resource.open()) {
                NativeImage image = NativeImage.read(stream);
                try {
                    DynamicTexture texture = new DynamicTexture(spriteId::toString, image);
                    texture.upload();
                    image.close();
                    mc.getTextureManager().register(spriteId, texture);
                } catch (Exception exception) {
                    image.close();
                    throw exception;
                }
            }
        } catch (Exception exception) {
            log.error("Failed to load UI sprite {}", spriteId, exception);
        }
    }
}
