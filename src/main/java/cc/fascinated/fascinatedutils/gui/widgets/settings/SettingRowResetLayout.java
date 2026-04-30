package cc.fascinated.fascinatedutils.gui.widgets.settings;

import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.theme.Icons;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SettingRowResetLayout {

    public static float glyphBoxPx() {
        return FascinatedGuiTheme.INSTANCE.resetGlyphSize();
    }

    public static float resetGapBesideControlPx() {
        return UITheme.GAP_LG;
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

    /**
     * Draws the trailing reset glyph when the value is not already default.
     *
     * <p>Trailing width from {@link #trailingResetReservePx()} is unchanged when {@code atDefault}; the glyph is simply
     * not drawn.
     *
     * @param graphics  active GUI renderer
     * @param originX   glyph left
     * @param originY   glyph top
     * @param rowHeight row height used for vertical alignment context elsewhere on the row
     * @param hovered   whether the pointer is over the interactable reset hit (unused; reserved for future styling)
     * @param atDefault when true, skips painting
     */
    public static void paintGlyph(GuiRenderer graphics, float originX, float originY, float rowHeight, boolean hovered, boolean atDefault) {
        if (atDefault) {
            return;
        }
        float box = glyphBoxPx();
        Icons.paintSettingResetCharacter(graphics, originX, originY, box, box, graphics.theme().textPrimary());
    }

    /**
     * Whether the pointer should hit the reset control for interaction.
     *
     * <p>When {@code atDefault} is true, this returns false so the reserved trailing slot does not steal hovers,
     * clicks, or tooltips even though layout space is unchanged.
     *
     * @param left      glyph square left
     * @param top       glyph square top
     * @param size      glyph square edge length
     * @param pointerX  logical pointer X
     * @param pointerY  logical pointer Y
     * @param atDefault whether the setting value already matches its default
     * @return true when the reset glyph is interactable and the pointer lies inside its square
     */
    public static boolean resetGlyphHitActive(float[] square, float pointerX, float pointerY, boolean atDefault) {
        return resetGlyphHitActive(square[0], square[1], square[2], pointerX, pointerY, atDefault);
    }

    public static boolean resetGlyphHitActive(float left, float top, float size, float pointerX, float pointerY, boolean atDefault) {
        if (atDefault) {
            return false;
        }
        return rectContains(left, top, size, pointerX, pointerY);
    }

    public static boolean rectContains(float[] square, float pointerX, float pointerY) {
        return rectContains(square[0], square[1], square[2], pointerX, pointerY);
    }

    public static boolean rectContains(float left, float top, float size, float pointerX, float pointerY) {
        return pointerX >= left && pointerY >= top && pointerX < left + size && pointerY < top + size;
    }

    public static boolean rectContains(float left, float top, float width, float height, float pointerX, float pointerY) {
        return pointerX >= left && pointerY >= top && pointerX < left + width && pointerY < top + height;
    }
}
