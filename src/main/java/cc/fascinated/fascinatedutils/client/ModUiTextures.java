package cc.fascinated.fascinatedutils.client;

import cc.fascinated.fascinatedutils.AlumiteMod;
import com.mojang.blaze3d.platform.NativeImage;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

@Getter
public enum ModUiTextures {
    CLOSE("ui/close"),
    BACK("ui/back"),
    RESET("ui/reset"),
    TRASH("ui/trash"),
    CHECK("ui/check"),
    SETTINGS("ui/settings"),
    CHEVRON_DOWN("ui/keyboard_arrow_down"),
    CHEVRON_RIGHT("ui/keyboard_arrow_right"),
    CHEVRON_LEFT("ui/keyboard_arrow_left"),
    EDIT("ui/edit"),
    VISIBILITY("ui/visibility"),
    VISIBILITY_OFF("ui/visibility_off"),
    SEARCH("ui/search"),
    GROUP("ui/group"),
    SUNNY("ui/sunny"),
    ADD("ui/add"),
    WARNING("ui/warning"),
    MORE_VERT("ui/more_vert"),
    COPY("ui/copy"),
    IMAGE("ui/image"),
    STATUS_ONLINE("ui/user_status/online"),
    STATUS_AWAY("ui/user_status/idle"),
    STATUS_DO_NOT_DISTURB("ui/user_status/do_not_disturb"),
    STATUS_INVISIBLE("ui/user_status/invisible"),
    ;

    private static final Logger LOGGER = LoggerFactory.getLogger(ModUiTextures.class);

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
     * routes it through the linear-sampled mesh path instead of the nearest-filtered atlas path.
     *
     * @param mc the active Minecraft instance
     */
    public static void loadTextures(Minecraft mc) {
        for (ModUiTextures sprite : values()) {
            Identifier resourceId = Identifier.fromNamespaceAndPath(
                    AlumiteMod.MOD_ID, "textures/gui/sprites/" + sprite.id.getPath() + ".png");
            try {
                Resource resource = mc.getResourceManager().getResourceOrThrow(resourceId);
                try (InputStream stream = resource.open()) {
                    NativeImage image = NativeImage.read(stream);
                    DynamicTexture texture = new DynamicTexture(sprite.id::toString, image);
                    texture.upload();
                    image.close();
                    mc.getTextureManager().register(sprite.id, texture);
                }
            } catch (IOException exception) {
                LOGGER.error("Failed to load UI sprite {}", sprite.id, exception);
            }
        }
    }

    /**
     * Draw a GUI sprite with a packed ARGB tint.
     *
     * @param graphics  draw context for the current GUI frame
     * @param textureId sprite identifier registered in the GUI sprite atlas
     * @param x         destination left in screen pixels
     * @param y         destination top in screen pixels
     * @param width     destination width in screen pixels
     * @param height    destination height in screen pixels
     * @param tintArgb  packed ARGB tint (including alpha)
     */
    public static void drawTinted(GuiGraphicsExtractor graphics, Identifier textureId, int x, int y, int width, int height, int tintArgb) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, textureId, x, y, width, height, tintArgb);
    }
}
