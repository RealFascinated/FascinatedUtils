package cc.fascinated.fascinatedutils.renderer.text;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * GUI string and rich-text measurement and drawing (TextRenderer surface, clean-room).
 */
public interface TextRenderer {

    /**
     * Horizontal span for a substring in logical pixels.
     *
     * @param text   source string
     * @param length maximum code units to measure (clamped to {@code text.length()})
     * @param shadow whether shadow width padding matches draw mode
     * @return width in logical pixels, at least zero
     */
    double getWidth(String text, int length, boolean shadow);

    /**
     * Default width for the full string without shadow padding.
     *
     * @param text source string
     * @return width in logical pixels
     */
    default double getWidth(String text) {
        return getWidth(text, text.length(), false);
    }

    /**
     * Rich-text width in logical pixels.
     *
     * @param text rich text to measure
     * @return width in logical pixels, at least one when non-empty
     */
    int getWidth(Component text);

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
     * Draw a plain string using the active font.
     *
     * @param drawContext draw context for matrix and scissor state
     * @param text        string content
     * @param originX     left origin in logical pixels
     * @param originY     top origin in logical pixels
     * @param colorArgb   packed ARGB color
     * @param shadow      whether vanilla shadow is drawn
     */
    void drawString(GuiGraphicsExtractor drawContext, String text, int originX, int originY, int colorArgb, boolean shadow);

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
