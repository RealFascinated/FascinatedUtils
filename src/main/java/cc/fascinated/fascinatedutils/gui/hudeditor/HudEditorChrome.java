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
    public static final float CLOSE_BUTTON_SIZE = 10f;
    public static final float CLOSE_BUTTON_PAD = 2f;
    public static final float SETTINGS_BUTTON_SIZE = 10f;
    public static final float SETTINGS_BUTTON_GAP = 2f;
    private static final int SELECTED_COLOR = UiColor.argb("#913de2");
    private static final int SCALE_HANDLE_FILL = UiColor.argb("#ede4fa");

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
     * Whether the pointer lies over the scale handle for the given widget.
     *
     * @param widget   selected widget
     * @param pointerX pointer X in logical space
     * @param pointerY pointer Y in logical space
     * @return true when the point is inside the handle rectangle
     */
    public static boolean settingsButtonContainsPoint(HudModule widget, float pointerX, float pointerY) {
        float widgetX = widget.getHudState().getPositionX();
        float widgetY = widget.getHudState().getPositionY();
        float scaledWidth = widget.getScaledWidth();
        float closeLeft = widgetX + scaledWidth - CLOSE_BUTTON_SIZE - CLOSE_BUTTON_PAD;
        float buttonX = closeLeft - SETTINGS_BUTTON_GAP - SETTINGS_BUTTON_SIZE;
        float buttonY = widgetY + CLOSE_BUTTON_PAD;
        return pointerX >= buttonX && pointerX <= buttonX + SETTINGS_BUTTON_SIZE && pointerY >= buttonY && pointerY <= buttonY + SETTINGS_BUTTON_SIZE;
    }

    public static boolean closeButtonContainsPoint(HudModule widget, float pointerX, float pointerY) {
        float widgetX = widget.getHudState().getPositionX();
        float widgetY = widget.getHudState().getPositionY();
        float scaledWidth = widget.getScaledWidth();
        float buttonX = widgetX + scaledWidth - CLOSE_BUTTON_SIZE - CLOSE_BUTTON_PAD;
        float buttonY = widgetY + CLOSE_BUTTON_PAD;
        return pointerX >= buttonX && pointerX <= buttonX + CLOSE_BUTTON_SIZE && pointerY >= buttonY && pointerY <= buttonY + CLOSE_BUTTON_SIZE;
    }

    public static boolean scaleHandleContainsPoint(HudModule widget, float pointerX, float pointerY) {
        float widgetX = widget.getHudState().getPositionX();
        float widgetY = widget.getHudState().getPositionY();
        float handleLeft = widgetX + widget.getScaledWidth() - SCALE_HANDLE_SIZE;
        float handleTop = widgetY + widget.getScaledHeight() - SCALE_HANDLE_SIZE;
        return pointerX >= handleLeft && pointerX <= widgetX + widget.getScaledWidth() && pointerY >= handleTop && pointerY <= widgetY + widget.getScaledHeight();
    }

    /**
     * Draws clipped HUD preview, post-stabilization, and optional scale handle when selected.
     *
     * @param glRenderer     renderer for this pass
     * @param widget         widget being painted
     * @param selectedWidget currently selected widget, or null
     * @param deltaSeconds   animation delta in seconds
     * @param canvasWidth    logical canvas width
     * @param canvasHeight   logical canvas height
     */
    public static void drawWidgetEditorChrome(GuiRenderer glRenderer, HudModule widget, HudModule selectedWidget, float deltaSeconds, float canvasWidth, float canvasHeight, boolean repositionFromAnchor) {
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

        drawSettingsButton(glRenderer, widget.getHudState().getPositionX(), widget.getHudState().getPositionY(), widget.getScaledWidth());
        drawCloseButton(glRenderer, widget.getHudState().getPositionX(), widget.getHudState().getPositionY(), widget.getScaledWidth());

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

    private static void drawSettingsButton(GuiRenderer glRenderer, float widgetX, float widgetY, float scaledWidth) {
        float buttonX = widgetX + scaledWidth - CLOSE_BUTTON_SIZE - CLOSE_BUTTON_PAD - SETTINGS_BUTTON_GAP - SETTINGS_BUTTON_SIZE;
        float buttonY = widgetY + CLOSE_BUTTON_PAD;
        glRenderer.drawRect(buttonX, buttonY, SETTINGS_BUTTON_SIZE, SETTINGS_BUTTON_SIZE, UiColor.argb("#44000000"));
        float inset = 1f;
        glRenderer.drawTexture(ModUiTextures.SETTINGS.getId(), buttonX + inset, buttonY + inset, SETTINGS_BUTTON_SIZE - inset * 2f, SETTINGS_BUTTON_SIZE - inset * 2f, 0xFFFFFFFF);
    }

    private static void drawCloseButton(GuiRenderer glRenderer, float widgetX, float widgetY, float scaledWidth) {
        float buttonX = widgetX + scaledWidth - CLOSE_BUTTON_SIZE - CLOSE_BUTTON_PAD;
        float buttonY = widgetY + CLOSE_BUTTON_PAD;
        glRenderer.drawRect(buttonX, buttonY, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE, UiColor.argb("#44000000"));
        float textX = buttonX + (CLOSE_BUTTON_SIZE - glRenderer.measureMiniMessageTextWidth("X")) * 0.5f;
        float textY = buttonY + (CLOSE_BUTTON_SIZE - glRenderer.getFontHeight()) * 0.5f;
        glRenderer.drawMiniMessageText("<color:#CC0000>X</color>", textX, textY, false);
    }
}
