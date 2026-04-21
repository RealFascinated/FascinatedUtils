package cc.fascinated.fascinatedutils.gui.hudeditor;

import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UiColor;
import cc.fascinated.fascinatedutils.systems.hud.HUDManager;
import cc.fascinated.fascinatedutils.systems.hud.HudLayoutCanvas;
import cc.fascinated.fascinatedutils.systems.hud.HudModule;

import java.util.ArrayList;
import java.util.List;

public class HudEditorChrome {

    public static final float SCALE_HANDLE_SIZE = 6f;
    public static final float MIN_WIDGET_SCALE = 0.5f;
    public static final float SCALE_DRAG_MIN_REFERENCE_DISTANCE = 4f;
    /**
     * Half-width of the band around 1.0 where scale snaps to exactly default.
     */
    public static final float SCALE_SNAP_TO_UNITY_BAND = 0.07f;
    public static final float CLOSE_BUTTON_SIZE = 13f;
    public static final float CLOSE_BUTTON_PAD = 3f;
    public static final float SETTINGS_BUTTON_SIZE = 13f;
    public static final float SETTINGS_BUTTON_GAP = 3f;
    private static final int SELECTED_COLOR = UiColor.argb("#913de2");
    private static final int SCALE_HANDLE_FILL = UiColor.argb("#ede4fa");
    private static final int BUTTON_FILL = UiColor.argb("#CC000000");
    private static final int BUTTON_FILL_HOVER = UiColor.argb("#EE000000");
    private static final int BUTTON_BORDER = UiColor.argb("#66FFFFFF");
    private static final int BUTTON_BORDER_HOVER = UiColor.argb("#BBFFFFFF");
    private static final int CLOSE_FILL_HOVER = UiColor.argb("#EE3d0000");
    private static final int CLOSE_BORDER_HOVER = UiColor.argb("#BBFF4444");
    private static final float BUTTON_ICON_INSET = 2f;

    /**
     * Widgets eligible for layout editing: matches {@link HUDManager#renderHUD}
     * visibility so disabled HUD widgets do not appear in the editor.
     *
     * @param registeredWidgets widgets from the HUD manager
     * @return visible widgets in registration order
     */
    public static List<HudModule> visibleLayoutWidgets(List<HudModule> registeredWidgets) {
        List<HudModule> visible = new ArrayList<>(registeredWidgets.size());
        for (HudModule widget : registeredWidgets) {
            if (widget.isEnabled()) {
                visible.add(widget);
            }
        }
        return visible;
    }

    /**
     * Clamps canvas extent to a safe finite range for layout math.
     *
     * @param extent logical width or height
     * @return clamped extent in logical pixels
     */
    public static float clampCanvasExtent(float extent) {
        return HudLayoutCanvas.clampExtent(extent);
    }

    /**
     * Snaps scale to exactly 1.0 when it lies near unity so the default size is obvious in the editor.
     *
     * @param scale current widget scale
     * @return snapped scale
     */
    public static float snapScaleNearUnity(float scale) {
        if (Math.abs(scale - 1f) <= SCALE_SNAP_TO_UNITY_BAND) {
            return 1f;
        }
        return scale;
    }

    /**
     * Computes the Y coordinate of the button row for a widget, preferring placement above the widget.
     * Falls back to below the widget when there is not enough space above.
     *
     * @param widget      widget whose buttons are being placed
     * @param canvasHeight logical canvas height
     * @return top-edge Y of the button row
     */
    public static float computeButtonRowY(HudModule widget, float canvasHeight) {
        float widgetY = widget.getHudState().getPositionY();
        float scaledHeight = widget.getScaledHeight();
        float buttonSize = CLOSE_BUTTON_SIZE;
        if (widgetY - buttonSize >= 0f) {
            return widgetY - buttonSize;
        } else if (widgetY + scaledHeight + buttonSize <= canvasHeight) {
            return widgetY + scaledHeight;
        } else {
            return widgetY + CLOSE_BUTTON_PAD;
        }
    }

    /**
     * Whether the pointer lies over the settings button for the given widget.
     *
     * @param widget       widget to test
     * @param pointerX     pointer X in logical space
     * @param pointerY     pointer Y in logical space
     * @param canvasHeight logical canvas height for button placement
     * @return true when the point is inside the settings button rectangle
     */
    public static boolean settingsButtonContainsPoint(HudModule widget, float pointerX, float pointerY, float canvasHeight) {
        float widgetX = widget.getHudState().getPositionX();
        float scaledWidth = widget.getScaledWidth();
        float closeLeft = widgetX + scaledWidth - CLOSE_BUTTON_SIZE - CLOSE_BUTTON_PAD;
        float buttonX = closeLeft - SETTINGS_BUTTON_GAP - SETTINGS_BUTTON_SIZE;
        float buttonY = computeButtonRowY(widget, canvasHeight);
        return pointerX >= buttonX && pointerX <= buttonX + SETTINGS_BUTTON_SIZE && pointerY >= buttonY && pointerY <= buttonY + SETTINGS_BUTTON_SIZE;
    }

    public static boolean closeButtonContainsPoint(HudModule widget, float pointerX, float pointerY, float canvasHeight) {
        float widgetX = widget.getHudState().getPositionX();
        float scaledWidth = widget.getScaledWidth();
        float buttonX = widgetX + scaledWidth - CLOSE_BUTTON_SIZE - CLOSE_BUTTON_PAD;
        float buttonY = computeButtonRowY(widget, canvasHeight);
        return pointerX >= buttonX && pointerX <= buttonX + CLOSE_BUTTON_SIZE && pointerY >= buttonY && pointerY <= buttonY + CLOSE_BUTTON_SIZE;
    }

    public static boolean scaleHandleContainsPoint(HudModule widget, float pointerX, float pointerY) {
        float widgetX = widget.getHudState().getPositionX();
        float widgetY = widget.getHudState().getPositionY();
        float handleLeft = widgetX + widget.getScaledWidth() - SCALE_HANDLE_SIZE;
        float handleTop = widgetY + widget.getScaledHeight() - SCALE_HANDLE_SIZE;
        return pointerX >= handleLeft && pointerX <= widgetX + widget.getScaledWidth() && pointerY >= handleTop && pointerY <= widgetY + widget.getScaledHeight();
    }

    public static void drawWidgetEditorChrome(GuiRenderer glRenderer, HudModule widget, HudModule selectedWidget, float deltaSeconds, float canvasWidth, float canvasHeight, boolean repositionFromAnchor) {
        drawWidgetEditorChrome(glRenderer, widget, selectedWidget, deltaSeconds, canvasWidth, canvasHeight, repositionFromAnchor, Float.NaN, Float.NaN);
    }

    /**
     * Draws clipped HUD preview, post-stabilization, and widget action buttons.
     * Buttons are drawn only when the pointer is over the widget or the widget is selected.
     *
     * @param glRenderer          renderer for this pass
     * @param widget              widget being painted
     * @param selectedWidget      currently selected widget, or null
     * @param deltaSeconds        animation delta in seconds
     * @param canvasWidth         logical canvas width
     * @param canvasHeight        logical canvas height
     * @param repositionFromAnchor whether to reposition from anchor before drawing
     * @param pointerX            logical pointer X for hover detection
     * @param pointerY            logical pointer Y for hover detection
     */
    public static void drawWidgetEditorChrome(GuiRenderer glRenderer, HudModule widget, HudModule selectedWidget, float deltaSeconds, float canvasWidth, float canvasHeight, boolean repositionFromAnchor, float pointerX, float pointerY) {
        Runnable draw = widget.prepareAndDraw(glRenderer, deltaSeconds, true);
        if (draw == null) {
            widget.recordHudContentSkipped();
        }
        if (repositionFromAnchor) {
            widget.applyHudAnchorToPosition(canvasWidth, canvasHeight);
        }
        if (draw != null) {
            glRenderer.pushTranslate(widget.getHudState().getPositionX(), widget.getHudState().getPositionY());
            glRenderer.pushScale(widget.getHudState().getScale());
            draw.run();
            glRenderer.endRenderSegment();
            glRenderer.popScale();
            glRenderer.popTranslate();
        }

        boolean pointerOverWidget = !Float.isNaN(pointerX) && widget.containsPoint(pointerX, pointerY);
        if (pointerOverWidget || widget == selectedWidget) {
            float buttonRowY = computeButtonRowY(widget, canvasHeight);
            boolean closeHovered = closeButtonContainsPoint(widget, pointerX, pointerY, canvasHeight);
            boolean settingsHovered = settingsButtonContainsPoint(widget, pointerX, pointerY, canvasHeight);
            drawSettingsButton(glRenderer, widget.getHudState().getPositionX(), buttonRowY, widget.getScaledWidth(), settingsHovered);
            drawCloseButton(glRenderer, widget.getHudState().getPositionX(), buttonRowY, widget.getScaledWidth(), closeHovered);
        }

        if (widget == selectedWidget) {
            drawScaleHandle(glRenderer, widget.getHudState().getPositionX(), widget.getHudState().getPositionY(), widget.getScaledWidth(), widget.getScaledHeight());
        }

        glRenderer.endRenderSegment();
    }

    /**
     * Draws a 1px axis-aligned rectangle outline.
     *
     * @param renderer    target renderer
     * @param rectX       left edge
     * @param rectY       top edge
     * @param rectWidth   width
     * @param rectHeight  height
     * @param strokeColor stroke color
     */
    public static void drawAxisAlignedBorder1px(UIRenderer renderer, float rectX, float rectY, float rectWidth, float rectHeight, int strokeColor) {
        rectX = (float) Math.floor(rectX);
        rectY = (float) Math.floor(rectY);
        rectWidth = Math.max(1f, (float) Math.round(rectWidth));
        rectHeight = Math.max(1f, (float) Math.round(rectHeight));
        if (rectWidth <= 0f || rectHeight <= 0f) {
            return;
        }
        renderer.drawRect(rectX, rectY, rectWidth, 1f, strokeColor);
        if (rectHeight > 1f) {
            renderer.drawRect(rectX, rectY + rectHeight - 1f, rectWidth, 1f, strokeColor);
        }
        float verticalSpan = rectHeight - 2f;
        if (verticalSpan > 0f && rectWidth > 1f) {
            renderer.drawRect(rectX, rectY + 1f, 1f, verticalSpan, strokeColor);
            renderer.drawRect(rectX + rectWidth - 1f, rectY + 1f, 1f, verticalSpan, strokeColor);
        }
    }

    private static void drawScaleHandle(GuiRenderer glRenderer, float widgetX, float widgetY, float scaledWidth, float scaledHeight) {
        float snappedSize = Math.max(1f, (float) Math.round(SCALE_HANDLE_SIZE));
        float handleLeft = (float) Math.floor(widgetX + scaledWidth - snappedSize);
        float handleTop = (float) Math.floor(widgetY + scaledHeight - snappedSize);
        glRenderer.drawRect(handleLeft, handleTop, snappedSize, snappedSize, SCALE_HANDLE_FILL);
        drawAxisAlignedBorder1px(glRenderer, handleLeft, handleTop, snappedSize, snappedSize, SELECTED_COLOR);
    }

    private static void drawSettingsButton(GuiRenderer glRenderer, float widgetX, float buttonRowY, float scaledWidth, boolean hovered) {
        float buttonX = widgetX + scaledWidth - CLOSE_BUTTON_SIZE - CLOSE_BUTTON_PAD - SETTINGS_BUTTON_GAP - SETTINGS_BUTTON_SIZE;
        float buttonY = buttonRowY;
        int fill = hovered ? BUTTON_FILL_HOVER : BUTTON_FILL;
        int border = hovered ? BUTTON_BORDER_HOVER : BUTTON_BORDER;
        glRenderer.drawRect(buttonX, buttonY, SETTINGS_BUTTON_SIZE, SETTINGS_BUTTON_SIZE, fill);
        drawAxisAlignedBorder1px(glRenderer, buttonX, buttonY, SETTINGS_BUTTON_SIZE, SETTINGS_BUTTON_SIZE, border);
        glRenderer.drawTexture(ModUiTextures.SETTINGS.getId(), buttonX + BUTTON_ICON_INSET, buttonY + BUTTON_ICON_INSET, SETTINGS_BUTTON_SIZE - BUTTON_ICON_INSET * 2f, SETTINGS_BUTTON_SIZE - BUTTON_ICON_INSET * 2f, 0xFFFFFFFF);
    }

    private static void drawCloseButton(GuiRenderer glRenderer, float widgetX, float buttonRowY, float scaledWidth, boolean hovered) {
        float buttonX = widgetX + scaledWidth - CLOSE_BUTTON_SIZE - CLOSE_BUTTON_PAD;
        float buttonY = buttonRowY;
        int fill = hovered ? CLOSE_FILL_HOVER : BUTTON_FILL;
        int border = hovered ? CLOSE_BORDER_HOVER : BUTTON_BORDER;
        glRenderer.drawRect(buttonX, buttonY, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE, fill);
        drawAxisAlignedBorder1px(glRenderer, buttonX, buttonY, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE, border);
        glRenderer.drawTexture(ModUiTextures.CLOSE.getId(), buttonX + BUTTON_ICON_INSET, buttonY + BUTTON_ICON_INSET, CLOSE_BUTTON_SIZE - BUTTON_ICON_INSET * 2f, CLOSE_BUTTON_SIZE - BUTTON_ICON_INSET * 2f, 0xFFFFFFFF);
    }
}
