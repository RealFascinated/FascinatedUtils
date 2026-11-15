package cc.fascinated.fascinatedutils.gui.screens.impl.hudeditor;

import cc.fascinated.fascinatedutils.gui.theme.UiColor;
import cc.fascinated.fascinatedutils.systems.hud.HudPanel;
import cc.fascinated.fascinatedutils.systems.hud.anchor.HUDWidgetAnchor;

import java.util.ArrayList;
import java.util.List;

/**
 * Geometry and constants shared by the HUD editor nodes: action bar placement, scale handle placement and scale
 * snapping.
 */
public class HudEditorChrome {

    public static final float SCALE_HANDLE_SIZE = 6f;
    public static final float MIN_WIDGET_SCALE = 0.5f;
    public static final float SCALE_DRAG_MIN_REFERENCE_DISTANCE = 4f;
    /**
     * Half-width of the band around 1.0 where scale snaps to exactly default.
     */
    public static final float SCALE_SNAP_TO_UNITY_BAND = 0.07f;
    public static final int ACTION_BUTTON_SIZE = 16;
    public static final int ACTION_BUTTON_PAD = 3;
    public static final int SELECTED_COLOR = UiColor.argb("#913de2");
    public static final int SCALE_HANDLE_FILL = UiColor.argb("#ede4fa");

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
     * Pixel size of the scale handle.
     *
     * @return handle edge length, at least one pixel
     */
    public static float scaleHandleSize() {
        return Math.max(1f, (float) Math.round(SCALE_HANDLE_SIZE));
    }

    /**
     * Left edge of the scale handle for a widget.
     *
     * @param widget widget the handle belongs to
     * @return handle left in logical space
     */
    public static float scaleHandleLeft(HudPanel widget) {
        float widgetX = widget.getHudState().getPositionX();
        return (float) Math.floor(handleOnRight(widget.getHudState().getHudAnchor())
            ? widgetX + widget.getScaledWidth() - scaleHandleSize()
            : widgetX);
    }

    /**
     * Top edge of the scale handle for a widget.
     *
     * @param widget widget the handle belongs to
     * @return handle top in logical space
     */
    public static float scaleHandleTop(HudPanel widget) {
        float widgetY = widget.getHudState().getPositionY();
        return (float) Math.floor(handleOnBottom(widget.getHudState().getHudAnchor())
            ? widgetY + widget.getScaledHeight() - scaleHandleSize()
            : widgetY);
    }

    /**
     * Widgets eligible for layout editing: matches the HUD manager's render visibility so disabled HUD widgets do
     * not appear in the editor.
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
     * @return Y of the button bar top
     */
    public static float buttonBarY(float widgetY, float scaledHeight) {
        float barHeight = ACTION_BUTTON_SIZE + 2 * ACTION_BUTTON_PAD;
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
     * the widget, inside the button bar strip, preventing hover from flickering as the cursor moves toward
     * the action buttons.
     *
     * @param widget   widget to test
     * @param pointerX pointer X in logical space
     * @param pointerY pointer Y in logical space
     * @return true when the pointer is in the combined hit zone
     */
    public static boolean isInActionZone(HudPanel widget, float pointerX, float pointerY) {
        if (widget.containsPoint(pointerX, pointerY)) {
            return true;
        }
        float widgetX = widget.getHudState().getPositionX();
        float widgetY = widget.getHudState().getPositionY();
        float scaledWidth = widget.getScaledWidth();
        float barY = buttonBarY(widgetY, widget.getScaledHeight());
        if (barY == widgetY) {
            return false;
        }
        float barHeight = ACTION_BUTTON_SIZE + 2 * ACTION_BUTTON_PAD;
        return pointerX >= widgetX && pointerX <= widgetX + scaledWidth
            && pointerY >= barY && pointerY <= barY + barHeight;
    }
}
