package cc.fascinated.fascinatedutils.gui.widgets.settings;

import cc.fascinated.fascinatedutils.gui.GuiDesignSpace;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.theme.Icons;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.themes.fascinated.FascinatedGuiTheme;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SettingRowResetLayout {

    public static float glyphBoxPx() {
        return GuiDesignSpace.pxUniform(FascinatedGuiTheme.INSTANCE.resetGlyphSize());
    }

    public static float resetGapBesideControlPx() {
        return GuiDesignSpace.pxX(UITheme.GAP_LG);
    }

    public static float trailingResetReservePx() {
        return glyphBoxPx() + resetGapBesideControlPx();
    }

    public static float trailingResetLeftX(float contentRightX) {
        return contentRightX - glyphBoxPx();
    }

    public static float verticallyCenteredTop(float rowTop, float rowHeight) {
        float box = glyphBoxPx();
        return rowTop + Math.max(0f, (rowHeight - box) * 0.5f);
    }

    public static void paintGlyph(GuiRenderer graphics, float originX, float originY, float rowHeight, boolean hovered, boolean atDefault) {
        float box = glyphBoxPx();
        int tint = atDefault ? graphics.theme().textMuted() : graphics.theme().textPrimary();
        Icons.paintSettingResetCharacter(graphics, originX, originY, box, box, tint);
    }

    public static boolean rectContains(float left, float top, float size, float pointerX, float pointerY) {
        return pointerX >= left && pointerY >= top && pointerX < left + size && pointerY < top + size;
    }
}
