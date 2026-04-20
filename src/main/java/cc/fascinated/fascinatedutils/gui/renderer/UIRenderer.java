package cc.fascinated.fascinatedutils.gui.renderer;

import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.gui.GuiTheme;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import net.minecraft.resources.Identifier;

public interface UIRenderer {

    /**
     * Active GUI theme for this renderer (defaults to the Fascinated shell theme when not overridden).
     *
     * @return non-null theme
     */
    default GuiTheme theme() {
        return FascinatedGuiTheme.INSTANCE;
    }

    void drawRect(float positionX, float positionY, float width, float height, int color);

    /**
     * Draw a straight line segment with the given stroke thickness (logical pixels). Prefer a bounded number of fills,
     * not per-pixel stepping.
     */
    void drawLine(float x1, float y1, float x2, float y2, float thickness, int color);

    void fillGradientVertical(float positionX, float positionY, float width, float height, int colorTop, int colorBottom);

    void fillRoundedGradientVertical(float positionX, float positionY, float width, float height, float cornerRadius, int colorTop, int colorBottom, int cornerRoundMask);

    void drawBorder(float positionX, float positionY, float width, float height, float thickness, int color);

    void fillRoundedRect(float positionX, float positionY, float width, float height, float cornerRadius, int fillArgb, int cornerRoundMask);

    void fillRoundedRectFrame(float positionX, float positionY, float width, float height, float cornerRadius, int borderArgb, int fillArgb, float borderThicknessX, float borderThicknessY, int cornerRoundMask);

    /**
     * Draws a plain string in logical pixel coordinates.
     *
     * @param positionY layout line top for this line in logical pixels (converted to a vanilla text baseline before
     *                  rasterizing)
     */
    void drawText(String text, float positionX, float positionY, int color, boolean shadow, boolean bold);

    /**
     * Draws a plain string centered horizontally on {@code centerX}.
     *
     * @param positionY layout line top for the first line in logical pixels (converted to a vanilla text baseline
     *                  before rasterizing)
     */
    void drawCenteredText(String text, float centerX, float positionY, int color, boolean shadow, boolean bold);

    /**
     * Draws MiniMessage-backed rich text in logical pixel coordinates.
     *
     * @param positionY layout line top for this line in logical pixels (converted to a vanilla text baseline before
     *                  rasterizing)
     */
    void drawMiniMessageText(String miniMessageText, float positionX, float positionY, boolean shadow);

    int measureMiniMessageTextWidth(String miniMessageText);

    /**
     * Draw a GUI texture with the given tint. Textures must be registered via {@link
     * ModUiTextures#reloadAll} after resource load (see {@link
     * ModUiTextures#registerResourceReloadListener}).
     */
    void drawTexture(Identifier texture, float positionX, float positionY, float width, float height, int tintArgb);

    void pushClip(float positionX, float positionY, float width, float height);

    /**
     * Push a clip rectangle expanded by logical outsets (for example horizontal scroll gutters), intersected with the
     * active clip when nested.
     *
     * @param positionX        left of the inner content rectangle before outset expansion
     * @param positionY        top of the inner content rectangle before outset expansion
     * @param width            inner width before outset expansion
     * @param height           inner height before outset expansion
     * @param horizontalOutset logical pixels to expand left and right
     * @param verticalOutset   logical pixels to expand top and bottom
     */
    void pushClipWithLogicalOutset(float positionX, float positionY, float width, float height, float horizontalOutset, float verticalOutset);

    void popClip();

    void pushTranslate(float offsetX, float offsetY);

    void popTranslate();

    void pushScale(float scale);

    void popScale();

    int measureTextWidth(String text, boolean bold);

    /**
     * Logical height of one text line for layout and intrinsic text hosts (matches the active {@code TextRenderer}).
     */
    int getFontHeight();

    void setMultiplyAlpha(float factor);

    void resetMultiplyAlpha();
}
