package cc.fascinated.fascinatedutils.renderer.text;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * GUI string and rich-text measurement and drawing (TextRenderer surface, clean-room).
 */
public interface TextRenderer {

    /**
     * Rich-text width in logical pixels.
     *
     * @param text rich text to measure
     * @return width in logical pixels, at least one when non-empty
     */
    int getWidth(Component text);

    /**
     * Precomposed literal/styled glyph run width in logical pixels.
     *
     * @param text visual-order character sequence
     * @return width in logical pixels, at least one when non-empty
     */
    int getWidth(FormattedCharSequence text);

    /**
     * Nominal line height for body text in logical pixels.
     *
     * @param shadow whether shadow extends the line box
     * @return height in logical pixels, at least one
     */
    double getHeight(boolean shadow);

    /**
     * Line height without shadow extension.
     *
     * @return height in logical pixels
     */
    default double getHeight() {
        return getHeight(false);
    }

    /**
     * Draw a precomposed literal/styled glyph run using the active font.
     *
     * @param drawContext draw context for matrix and scissor state
     * @param text        visual-order character sequence
     * @param originX     left origin in logical pixels
     * @param originY     top origin in logical pixels
     * @param colorArgb   packed ARGB color
     * @param shadow      whether vanilla shadow is drawn
     */
    void drawText(GuiGraphicsExtractor drawContext, FormattedCharSequence text, int originX, int originY, int colorArgb, boolean shadow);

    /**
     * Draw rich text using the active font.
     *
     * @param drawContext draw context for matrix and scissor state
     * @param text        styled text
     * @param originX     left origin in logical pixels
     * @param originY     top origin in logical pixels
     * @param colorArgb   packed ARGB color
     * @param shadow      whether vanilla shadow is drawn
     */
    void drawText(GuiGraphicsExtractor drawContext, Component text, int originX, int originY, int colorArgb, boolean shadow);
}
