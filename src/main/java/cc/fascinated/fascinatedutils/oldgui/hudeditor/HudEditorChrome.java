package cc.fascinated.fascinatedutils.oldgui.hudeditor;

import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.oldgui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.oldgui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.oldgui.theme.UiColor;
import cc.fascinated.fascinatedutils.systems.hud.HUDManager;
import cc.fascinated.fascinatedutils.systems.hud.HudLayoutCanvas;
import cc.fascinated.fascinatedutils.systems.hud.HudPanel;
import cc.fascinated.fascinatedutils.systems.hud.anchor.HUDWidgetAnchor;

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
    private static final int SELECTED_COLOR = UiColor.argb("#913de2");
    private static final int SCALE_HANDLE_FILL = UiColor.argb("#ede4fa");
    private static final int BUTTON_BG_IDLE = UiColor.argb("#CC000000");
    private static final int BUTTON_BG_HOVERED = UiColor.argb("#EE1a0a2e");
    private static final float BUTTON_ICON_INSET = 2f;

    /** True when the scale handle sits on the right edge of the widget for the given anchor. */
    private static boolean handleOnRight(HUDWidgetAnchor anchor) {
        return switch (anchor) {
            case TOP_RIGHT, BOTTOM_RIGHT, RIGHT -> false;
            default -> true;
        };
    }

    /** True when the scale handle sits on the bottom edge of the widget for the given anchor. */
    private static boolean handleOnBottom(HUDWidgetAnchor anchor) {
        return switch (anchor) {
            case BOTTOM_LEFT, BOTTOM_RIGHT, BOTTOM -> false;
            default -> true;
        };
    }

    /**
     * X of the fixed corner used as the reference point for scale drag math.
     * This is the corner diagonally opposite the scale handle.
     *
     * @param widget widget being scaled
     * @return reference X in logical space
     */
    public static float scaleDragOriginX(HudPanel widget) {
        return handleOnRight(widget.getHudState().getHudAnchor())
            ? widget.getHudState().getPositionX()
            : widget.getHudState().getPositionX() + widget.getScaledWidth();
    }

    /**
     * Y of the fixed corner used as the reference point for scale drag math.
     *
     * @param widget widget being scaled
     * @return reference Y in logical space
     */
    public static float scaleDragOriginY(HudPanel widget) {
        return handleOnBottom(widget.getHudState().getHudAnchor())
            ? widget.getHudState().getPositionY()
            : widget.getHudState().getPositionY() + widget.getScaledHeight();
    }

    /**
     * Widgets eligible for layout editing: matches {@link HUDManager#renderHUD}
     * visibility so disabled HUD widgets do not appear in the editor.
     *
     * @param registeredWidgets widgets from the HUD manager
     * @return visible widgets in registration order
     */
    public static List<HudPanel> visibleLayoutWidgets(List<HudPanel> registeredWidgets) {
        List<HudPanel> visible = new ArrayList<>(registeredWidgets.size());
        for (HudPanel widget : registeredWidgets) {
            if (widget.shouldRenderHudPanel()) {
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
     * Y coordinate of the top of the action button bar.
     *
     * <p>When the widget is tall enough the buttons sit inside its top edge; otherwise they float above
     * (preferred) or below when there is no space above.
     *
     * @param widgetY      widget top in logical space
     * @param scaledHeight widget height including scale
     * @param canvasHeight logical canvas height
     * @return Y of the button bar top
     */
    private static float buttonBarY(float widgetY, float scaledHeight, float canvasHeight) {
        float barHeight = CLOSE_BUTTON_SIZE + 2 * CLOSE_BUTTON_PAD;
        if (scaledHeight >= barHeight) {
            return widgetY;
        }
        float aboveY = widgetY - barHeight;
        if (aboveY >= 0f) {
            return aboveY;
        }
        return widgetY + scaledHeight;
    }

    /**
     * Returns true when the pointer is inside the widget bounds or, when the button bar is rendered outside
     * the widget, inside the button bar strip — preventing hover from flickering as the cursor moves toward
     * the action buttons.
     *
     * @param widget      widget to test
     * @param pointerX    pointer X in logical space
     * @param pointerY    pointer Y in logical space
     * @param canvasHeight logical canvas height
     * @return true when the pointer is in the combined hit zone
     */
    public static boolean isInActionZone(HudPanel widget, float pointerX, float pointerY, float canvasHeight) {
        if (widget.containsPoint(pointerX, pointerY)) {
            return true;
        }
        float widgetX = widget.getHudState().getPositionX();
        float widgetY = widget.getHudState().getPositionY();
        float scaledWidth = widget.getScaledWidth();
        float scaledHeight = widget.getScaledHeight();
        float barY = buttonBarY(widgetY, scaledHeight, canvasHeight);
        if (barY == widgetY) {
            return false; // buttons are inside the widget; containsPoint() already covered it
        }
        float barHeight = CLOSE_BUTTON_SIZE + 2 * CLOSE_BUTTON_PAD;
        return pointerX >= widgetX && pointerX <= widgetX + scaledWidth
            && pointerY >= barY && pointerY <= barY + barHeight;
    }

    /**
     * Whether the pointer lies over the settings button for the given widget.
     *
     * @param widget      widget to test
     * @param pointerX    pointer X in logical space
     * @param pointerY    pointer Y in logical space
     * @param canvasHeight logical canvas height
     * @return true when the point is inside the settings button rectangle
     */
    public static boolean settingsButtonContainsPoint(HudPanel widget, float pointerX, float pointerY, float canvasHeight) {
        float barY = buttonBarY(widget.getHudState().getPositionY(), widget.getScaledHeight(), canvasHeight);
        float buttonX = widget.getHudState().getPositionX() + CLOSE_BUTTON_PAD;
        float buttonY = barY + CLOSE_BUTTON_PAD;
        return pointerX >= buttonX && pointerX <= buttonX + SETTINGS_BUTTON_SIZE && pointerY >= buttonY && pointerY <= buttonY + SETTINGS_BUTTON_SIZE;
    }

    public static boolean closeButtonContainsPoint(HudPanel widget, float pointerX, float pointerY, float canvasHeight) {
        float widgetX = widget.getHudState().getPositionX();
        float scaledWidth = widget.getScaledWidth();
        float barY = buttonBarY(widget.getHudState().getPositionY(), widget.getScaledHeight(), canvasHeight);
        float buttonX = widgetX + scaledWidth - CLOSE_BUTTON_SIZE - CLOSE_BUTTON_PAD;
        float buttonY = barY + CLOSE_BUTTON_PAD;
        return pointerX >= buttonX && pointerX <= buttonX + CLOSE_BUTTON_SIZE && pointerY >= buttonY && pointerY <= buttonY + CLOSE_BUTTON_SIZE;
    }

    public static boolean scaleHandleContainsPoint(HudPanel widget, float pointerX, float pointerY) {
        float snappedSize = Math.max(1f, (float) Math.round(SCALE_HANDLE_SIZE));
        HUDWidgetAnchor anchor = widget.getHudState().getHudAnchor();
        float widgetX = widget.getHudState().getPositionX();
        float widgetY = widget.getHudState().getPositionY();
        float handleLeft = (float) Math.floor(handleOnRight(anchor)
            ? widgetX + widget.getScaledWidth() - snappedSize
            : widgetX);
        float handleTop = (float) Math.floor(handleOnBottom(anchor)
            ? widgetY + widget.getScaledHeight() - snappedSize
            : widgetY);
        return pointerX >= handleLeft && pointerX <= handleLeft + snappedSize
            && pointerY >= handleTop && pointerY <= handleTop + snappedSize;
    }

    public static void drawWidgetEditorChrome(GuiRenderer glRenderer, HudPanel widget, HudPanel selectedWidget, float deltaSeconds, float canvasWidth, float canvasHeight, boolean repositionFromAnchor) {
        drawWidgetEditorChrome(glRenderer, widget, selectedWidget, deltaSeconds, canvasWidth, canvasHeight, repositionFromAnchor, Float.NaN, Float.NaN);
    }

    /**
     * Draws clipped HUD preview, post-stabilization, and widget action buttons.
     * Buttons are drawn only when the pointer is over the widget or the widget is selected.
     *
     * @param glRenderer           renderer for this pass
     * @param widget               widget being painted
     * @param selectedWidget       currently selected widget, or null
     * @param deltaSeconds         animation delta in seconds
     * @param canvasWidth          logical canvas width
     * @param canvasHeight         logical canvas height
     * @param repositionFromAnchor whether to reposition from anchor before drawing
     * @param pointerX             logical pointer X for hover detection
     * @param pointerY             logical pointer Y for hover detection
     */
    public static void drawWidgetEditorChrome(GuiRenderer glRenderer, HudPanel widget, HudPanel selectedWidget, float deltaSeconds, float canvasWidth, float canvasHeight, boolean repositionFromAnchor, float pointerX, float pointerY) {
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

        boolean pointerOverWidget = !Float.isNaN(pointerX) && isInActionZone(widget, pointerX, pointerY, canvasHeight);
        if (pointerOverWidget) {
            float widgetX = widget.getHudState().getPositionX();
            float widgetY = widget.getHudState().getPositionY();
            float scaledWidth = widget.getScaledWidth();
            float scaledHeight = widget.getScaledHeight();
            float barY = buttonBarY(widgetY, scaledHeight, canvasHeight);
            boolean closeHovered = closeButtonContainsPoint(widget, pointerX, pointerY, canvasHeight);
            boolean settingsHovered = settingsButtonContainsPoint(widget, pointerX, pointerY, canvasHeight);
            drawSettingsButton(glRenderer, widgetX, barY, settingsHovered);
            drawCloseButton(glRenderer, widgetX, barY, scaledWidth, closeHovered);
        }

        if (widget == selectedWidget) {
            drawScaleHandle(glRenderer, widget);
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

    private static void drawScaleHandle(GuiRenderer glRenderer, HudPanel widget) {
        float snappedSize = Math.max(1f, (float) Math.round(SCALE_HANDLE_SIZE));
        HUDWidgetAnchor anchor = widget.getHudState().getHudAnchor();
        float widgetX = widget.getHudState().getPositionX();
        float widgetY = widget.getHudState().getPositionY();
        float handleLeft = (float) Math.floor(handleOnRight(anchor)
            ? widgetX + widget.getScaledWidth() - snappedSize
            : widgetX);
        float handleTop = (float) Math.floor(handleOnBottom(anchor)
            ? widgetY + widget.getScaledHeight() - snappedSize
            : widgetY);
        glRenderer.drawRect(handleLeft, handleTop, snappedSize, snappedSize, SCALE_HANDLE_FILL);
        drawAxisAlignedBorder1px(glRenderer, handleLeft, handleTop, snappedSize, snappedSize, SELECTED_COLOR);
    }

    private static void drawSettingsButton(GuiRenderer glRenderer, float widgetX, float widgetY, boolean hovered) {
        float buttonX = widgetX + CLOSE_BUTTON_PAD;
        float buttonY = widgetY + CLOSE_BUTTON_PAD;
        glRenderer.drawRect(buttonX, buttonY, SETTINGS_BUTTON_SIZE, SETTINGS_BUTTON_SIZE, hovered ? BUTTON_BG_HOVERED : BUTTON_BG_IDLE);
        int iconColor = hovered ? 0xFFFFFFFF : 0xDDFFFFFF;
        glRenderer.drawTexture(ModUiTextures.SETTINGS.getId(), buttonX + BUTTON_ICON_INSET, buttonY + BUTTON_ICON_INSET, SETTINGS_BUTTON_SIZE - BUTTON_ICON_INSET * 2f, SETTINGS_BUTTON_SIZE - BUTTON_ICON_INSET * 2f, iconColor);
    }

    private static void drawCloseButton(GuiRenderer glRenderer, float widgetX, float widgetY, float scaledWidth, boolean hovered) {
        float buttonX = widgetX + scaledWidth - CLOSE_BUTTON_SIZE - CLOSE_BUTTON_PAD;
        float buttonY = widgetY + CLOSE_BUTTON_PAD;
        glRenderer.drawRect(buttonX, buttonY, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE, hovered ? BUTTON_BG_HOVERED : BUTTON_BG_IDLE);
        int iconColor = hovered ? 0xFFFFFFFF : 0xDDFFFFFF;
        glRenderer.drawTexture(ModUiTextures.CLOSE.getId(), buttonX + BUTTON_ICON_INSET, buttonY + BUTTON_ICON_INSET, CLOSE_BUTTON_SIZE - BUTTON_ICON_INSET * 2f, CLOSE_BUTTON_SIZE - BUTTON_ICON_INSET * 2f, iconColor);
    }
}
