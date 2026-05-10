package cc.fascinated.fascinatedutils.client;

import cc.fascinated.fascinatedutils.AlumiteMod;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

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
    STATUS_ONLINE("ui/user_status/online"),
    STATUS_AWAY("ui/user_status/idle"),
    STATUS_DO_NOT_DISTURB("ui/user_status/do_not_disturb"),
    STATUS_INVISIBLE("ui/user_status/invisible"),
    ;

    private final Identifier id;

    ModUiTextures(String spritePath) {
        this.id = Identifier.fromNamespaceAndPath(AlumiteMod.MOD_ID, spritePath);
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
