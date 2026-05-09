package cc.fascinated.fascinatedutils.gui2.render;

import cc.fascinated.fascinatedutils.gui2.theme.UiTheme;
import net.minecraft.resources.Identifier;

/**
 * Per-frame renderer contract used by the new gui2 scene graph.
 *
 * <p>Implementations are responsible for clip stack safety, batching strategy, and text flush ordering.
 */
public interface RenderFrame {

    UiTheme theme();

    /**
     * Starts a new UI frame.
     *
     * @param logicalWidth full logical frame width in UI coordinates
     * @param logicalHeight full logical frame height in UI coordinates
     */
    void beginFrame(float logicalWidth, float logicalHeight);

    /**
     * Ends the current UI frame.
     */
    void endFrame();

    /**
     * Pushes a child clip region intersected with the active clip.
     *
     * @param clipRegion child clip region in logical coordinates
     */
    void pushClip(ClipRegion clipRegion);

    /**
     * Pops the active clip region.
     */
    void popClip();

    void pushAlpha(float alphaFactor);

    void popAlpha();

    void pushTransform(float translateX, float translateY, float scaleX, float scaleY);

    void popTransform();

    void drawRect(int positionX, int positionY, int width, int height, int argbColor);

    void drawLine(int startX, int startY, int endX, int endY, float thickness, int argbColor);

    void drawBorder(int positionX, int positionY, int width, int height, int thickness, int argbColor);

    void drawRoundedRect(int positionX, int positionY, int width, int height, int cornerRadius, int argbColor);

    void drawRoundedRectFrame(int positionX, int positionY, int width, int height, int cornerRadius, int borderArgbColor, int fillArgbColor, int borderThickness);

    void drawVerticalGradient(int positionX, int positionY, int width, int height, int topArgbColor, int bottomArgbColor);

    void drawText(String text, int positionX, int positionY, int argbColor, boolean shadow, boolean bold);

    /**
     * Flushes any queued text draws so they appear below subsequently drawn geometry.
     *
     * <p>Call this before rendering a sibling or overlay node that should appear above previously queued text.
     */
    void flushText();

    int measureTextWidth(String text, boolean bold);

    int fontHeight();

    void drawTexture(Identifier textureId, int positionX, int positionY, int width, int height, int tintArgb);

    void drawRoundedTexture(Identifier textureId, int positionX, int positionY, int width, int height, int cornerRadius, int tintArgb);
}
