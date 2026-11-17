package cc.fascinated.fascinatedutils.renderer.text;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * Measures and draws GUI text with the vanilla {@link Font}. Text is positioned by its layout line top: the top of a
 * line box whose baseline sits eight units below, which is also where {@link GlyphAtlas} puts its baseline.
 */
public class VanillaTextRenderer {
    /**
     * Vanilla draws a glyph row starting at its {@code y}, with the baseline seven units below. One extra unit moves
     * that baseline to eight below the layout line top.
     */
    private static final int LINE_TOP_TO_VANILLA_ORIGIN = 1;

    /**
     * Rich-text width.
     *
     * @param text rich text to measure
     * @return width in local units, at least one
     */
    public int width(Component text) {
        return Math.max(1, font().width(text));
    }

    /**
     * Precomposed glyph run width.
     *
     * @param text visual-order character sequence
     * @return width in local units, at least one
     */
    public int width(FormattedCharSequence text) {
        return Math.max(1, font().width(text));
    }

    /**
     * Nominal line height of body text.
     *
     * @return height in local units
     */
    public int lineHeight() {
        return font().lineHeight;
    }

    /**
     * Draws a precomposed glyph run. The origin is rounded to whole units on both axes, like rectangles.
     *
     * @param graphics  draw context supplying pose and scissor
     * @param text      visual-order character sequence
     * @param positionX left origin in local units
     * @param lineTopY  layout line top in local units
     * @param colorArgb packed ARGB color
     * @param shadow    whether vanilla shadow is drawn
     */
    public void draw(GuiGraphicsExtractor graphics, FormattedCharSequence text, float positionX, float lineTopY, int colorArgb, boolean shadow) {
        graphics.text(font(), text, Math.round(positionX), Math.round(lineTopY) + LINE_TOP_TO_VANILLA_ORIGIN, colorArgb, shadow);
    }

    /**
     * Draws rich text. The origin is rounded to whole units on both axes, like rectangles.
     *
     * @param graphics  draw context supplying pose and scissor
     * @param text      styled text
     * @param positionX left origin in local units
     * @param lineTopY  layout line top in local units
     * @param colorArgb packed ARGB color
     * @param shadow    whether vanilla shadow is drawn
     */
    public void draw(GuiGraphicsExtractor graphics, Component text, float positionX, float lineTopY, int colorArgb, boolean shadow) {
        graphics.text(font(), text, Math.round(positionX), Math.round(lineTopY) + LINE_TOP_TO_VANILLA_ORIGIN, colorArgb, shadow);
    }

    private Font font() {
        return Minecraft.getInstance().font;
    }
}
