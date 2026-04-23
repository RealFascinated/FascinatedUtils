package cc.fascinated.fascinatedutils.client;

import cc.fascinated.fascinatedutils.FascinatedUtils;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

@Getter
public enum ModUiTextures {
    CLOSE("ui/mod_settings/close"), BACK("ui/mod_settings/back"), RESET("ui/mod_settings/reset"),
    TRASH("ui/mod_settings/trash"), CHECK("ui/mod_settings/check"), SETTINGS("ui/mod_settings/settings"),
    CHEVRON_DOWN("ui/mod_settings/keyboard_arrow_down"), CHEVRON_RIGHT("ui/mod_settings/keyboard_arrow_right"),
    EDIT("ui/mod_settings/edit"), VISIBILITY("ui/mod_settings/visibility"),
    VISIBILITY_OFF("ui/mod_settings/visibility_off");

    private final Identifier id;

    ModUiTextures(String spritePath) {
        this.id = Identifier.fromNamespaceAndPath(FascinatedUtils.MOD_ID, spritePath);
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
