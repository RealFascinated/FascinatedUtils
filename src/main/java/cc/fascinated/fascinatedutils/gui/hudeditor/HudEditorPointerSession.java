package cc.fascinated.fascinatedutils.gui.hudeditor;

import cc.fascinated.fascinatedutils.gui.UIScale;
import cc.fascinated.fascinatedutils.gui.core.InputEvent;
import cc.fascinated.fascinatedutils.systems.hud.HUDEditorSnap;
import cc.fascinated.fascinatedutils.systems.hud.HUDManager;
import cc.fascinated.fascinatedutils.systems.hud.HudModule;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.BooleanSupplier;

public class HudEditorPointerSession {

    /**
     * Total logical drag delta above which a gesture counts as move/scale (not a click).
     */
    private static final float APPEARANCE_DRAG_MOVE_THRESHOLD_LOGICAL = 3f;
    private final HudEditorAppearancePanelController appearancePanel;
    @Nullable
    private HudModule dragging;
    private float dragOffsetX;
    private float dragOffsetY;
    @Nullable
    private HudModule selected;
    @Nullable
    private HudModule scalingWidget;
    private float scaleDragReferenceDistance;
    private float scaleDragStartScale;
    private boolean showControlsHint = true;
    private float snapGuideX = Float.NaN;
    private float snapGuideY = Float.NaN;
    /**
     * Accumulates {@code |dragX|+|dragY|} during a left drag or scale gesture (logical pixels).
     */
    private float leftPointerDragAccumulated;
    /**
     * When true, the appearance side panel stays hidden until the user completes a left press+release on a HUD widget
     * without a meaningful drag; avoids opening the panel right after moving or scaling a widget.
     */
    private boolean blockAppearancePanelUntilClickWithoutDrag;

    public HudEditorPointerSession(HudEditorAppearancePanelController appearancePanel) {
        this.appearancePanel = appearancePanel;
    }

    public boolean showControlsHint() {
        return showControlsHint;
    }

    @Nullable
    public HudModule selected() {
        return selected;
    }

    @Nullable
    public HudModule dragging() {
        return dragging;
    }

    @Nullable
    public HudModule scalingWidget() {
        return scalingWidget;
    }

    /**
     * @return false while the user must click a widget (without a drag gesture) before the appearance panel may show
     */
    public boolean appearancePanelUnblocked() {
        return !blockAppearancePanelUntilClickWithoutDrag;
    }

    public float snapGuideX() {
        return snapGuideX;
    }

    public float snapGuideY() {
        return snapGuideY;
    }

    public void clearSnapGuides() {
        snapGuideX = Float.NaN;
        snapGuideY = Float.NaN;
    }

    /**
     * Handles a mouse click in logical pointer space.
     *
     * @param event         click event from Minecraft
     * @param doubled       whether this is a double-click
     * @param delegateSuper invoked when the session does not consume the event for the left button path
     * @return whether the event was consumed
     */
    public boolean onMouseClicked(MouseButtonEvent event, boolean doubled, BooleanSupplier delegateSuper) {
        showControlsHint = false;
        clearSnapGuides();
        float pointerX = UIScale.logicalPointerX();
        float pointerY = UIScale.logicalPointerY();
        if (appearancePanel.host().root() != null && appearancePanel.containsPoint(pointerX, pointerY)) {
            appearancePanel.host().dispatchInput(new InputEvent.MousePress(pointerX, pointerY, event.button()));
            return true;
        }
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return delegateSuper.getAsBoolean();
        }
        List<HudModule> widgetList = HudEditorChrome.visibleLayoutWidgets(HUDManager.INSTANCE.getWidgets());
        for (int index = widgetList.size() - 1; index >= 0; index--) {
            HudModule widget = widgetList.get(index);
            if (!widget.containsPoint(pointerX, pointerY)) {
                continue;
            }
            if (widget.closeButtonContainsPoint(pointerX, pointerY)) {
                HUDManager.INSTANCE.setWidgetVisible(widget, false, true);
                selected = null;
                return true;
            }
            if (widget == selected && HudEditorChrome.scaleHandleContainsPoint(widget, pointerX, pointerY)) {
                leftPointerDragAccumulated = 0f;
                scalingWidget = widget;
                selected = widget;
                float anchorX = widget.getHudState().getPositionX();
                float anchorY = widget.getHudState().getPositionY();
                scaleDragReferenceDistance = Math.max(HudEditorChrome.SCALE_DRAG_MIN_REFERENCE_DISTANCE, (float) Math.hypot(pointerX - anchorX, pointerY - anchorY));
                scaleDragStartScale = widget.getHudState().getScale();
                return true;
            }
            leftPointerDragAccumulated = 0f;
            dragging = widget;
            selected = widget;
            dragOffsetX = pointerX - widget.getHudState().getPositionX();
            dragOffsetY = pointerY - widget.getHudState().getPositionY();
            return true;
        }
        selected = null;
        dragging = null;
        scalingWidget = null;
        blockAppearancePanelUntilClickWithoutDrag = false;
        leftPointerDragAccumulated = 0f;
        return true;
    }

    /**
     * Handles mouse drag in logical pointer space.
     *
     * @param event         drag event from Minecraft
     * @param dragX         drag delta X from Minecraft
     * @param dragY         drag delta Y from Minecraft
     * @param delegateSuper invoked when the session does not handle the drag
     * @return whether the drag was consumed
     */
    public boolean onMouseDragged(MouseButtonEvent event, double dragX, double dragY, BooleanSupplier delegateSuper) {
        float pointerX = UIScale.logicalPointerX();
        float pointerY = UIScale.logicalPointerY();
        if (appearancePanel.host().root() != null && appearancePanel.host().dispatchInput(new InputEvent.MouseMove(pointerX, pointerY))) {
            return true;
        }
        if (dragging != null || scalingWidget != null) {
            leftPointerDragAccumulated += (float) (Math.abs(dragX) + Math.abs(dragY));
        }
        if (scalingWidget != null) {
            clearSnapGuides();
            float anchorX = scalingWidget.getHudState().getPositionX();
            float anchorY = scalingWidget.getHudState().getPositionY();
            float pointerDistance = Math.max(HudEditorChrome.SCALE_DRAG_MIN_REFERENCE_DISTANCE, (float) Math.hypot(pointerX - anchorX, pointerY - anchorY));
            float nextScale = scaleDragStartScale * (pointerDistance / scaleDragReferenceDistance);
            if (nextScale < HudEditorChrome.MIN_WIDGET_SCALE) {
                nextScale = HudEditorChrome.MIN_WIDGET_SCALE;
            }
            scalingWidget.getHudState().setScale(HudEditorChrome.snapScaleNearUnity(nextScale));
            return true;
        }
        if (dragging != null) {
            float canvasWidth = HudEditorChrome.clampCanvasExtent(UIScale.logicalWidth());
            float canvasHeight = HudEditorChrome.clampCanvasExtent(UIScale.logicalHeight());
            float rawX = pointerX - dragOffsetX;
            float rawY = pointerY - dragOffsetY;
            float widgetWidth = dragging.getScaledWidth();
            float widgetHeight = dragging.getScaledHeight();
            HUDEditorSnap.SnapResult snapped = HUDEditorSnap.snapTopLeft(rawX, rawY, widgetWidth, widgetHeight, canvasWidth, canvasHeight, HudEditorChrome.visibleLayoutWidgets(HUDManager.INSTANCE.getWidgets()), dragging);
            dragging.getHudState().setPositionX(snapped.snappedLeft());
            dragging.getHudState().setPositionY(snapped.snappedTop());
            snapGuideX = snapped.hasVerticalGuide() ? snapped.verticalGuideX() : Float.NaN;
            snapGuideY = snapped.hasHorizontalGuide() ? snapped.horizontalGuideY() : Float.NaN;
            return true;
        }
        clearSnapGuides();
        return delegateSuper.getAsBoolean();
    }

    /**
     * Handles mouse release in logical pointer space.
     *
     * @param event         release event from Minecraft
     * @param delegateSuper invoked when the session does not handle the release
     * @return whether the release was consumed
     */
    public boolean onMouseReleased(MouseButtonEvent event, BooleanSupplier delegateSuper) {
        float pointerX = UIScale.logicalPointerX();
        float pointerY = UIScale.logicalPointerY();
        if (appearancePanel.host().root() != null && appearancePanel.host().dispatchInput(new InputEvent.MouseRelease(pointerX, pointerY, event.button()))) {
            return true;
        }
        if (scalingWidget != null) {
            HudModule widget = scalingWidget;
            scalingWidget = null;
            clearSnapGuides();
            widget.getHudState().setScale(HudEditorChrome.snapScaleNearUnity(widget.getHudState().getScale()));
            float canvasWidth = HudEditorChrome.clampCanvasExtent(UIScale.logicalWidth());
            float canvasHeight = HudEditorChrome.clampCanvasExtent(UIScale.logicalHeight());
            float maxX = Math.max(0f, canvasWidth - widget.getScaledWidth());
            float maxY = Math.max(0f, canvasHeight - widget.getScaledHeight());
            widget.getHudState().setPositionX(Mth.clamp(widget.getHudState().getPositionX(), 0f, maxX));
            widget.getHudState().setPositionY(Mth.clamp(widget.getHudState().getPositionY(), 0f, maxY));
            widget.captureNearestHudAnchorFromPosition(canvasWidth, canvasHeight);
            widget.applyHudAnchorToPosition(canvasWidth, canvasHeight);
            blockAppearancePanelUntilClickWithoutDrag = leftPointerDragAccumulated >= APPEARANCE_DRAG_MOVE_THRESHOLD_LOGICAL;
            leftPointerDragAccumulated = 0f;
            return true;
        }
        if (dragging != null) {
            HudModule widget = dragging;
            dragging = null;
            clearSnapGuides();
            float canvasWidth = HudEditorChrome.clampCanvasExtent(UIScale.logicalWidth());
            float canvasHeight = HudEditorChrome.clampCanvasExtent(UIScale.logicalHeight());
            float maxX = Math.max(0f, canvasWidth - widget.getScaledWidth());
            float maxY = Math.max(0f, canvasHeight - widget.getScaledHeight());
            widget.getHudState().setPositionX(Mth.clamp(widget.getHudState().getPositionX(), 0f, maxX));
            widget.getHudState().setPositionY(Mth.clamp(widget.getHudState().getPositionY(), 0f, maxY));
            widget.captureNearestHudAnchorFromPosition(canvasWidth, canvasHeight);
            widget.applyHudAnchorToPosition(canvasWidth, canvasHeight);
            blockAppearancePanelUntilClickWithoutDrag = leftPointerDragAccumulated >= APPEARANCE_DRAG_MOVE_THRESHOLD_LOGICAL;
            leftPointerDragAccumulated = 0f;
            return true;
        }
        return delegateSuper.getAsBoolean();
    }

    /**
     * Dispatches mouse move to the appearance panel when it is active.
     */
    public void onMouseMovedLogical() {
        appearancePanel.dispatchMouseMoveLogical();
    }

    /**
     * Handles scroll when the pointer is over the appearance panel.
     *
     * @param verticalAmount vertical scroll delta
     * @param delegateSuper  invoked when the scroll is not consumed
     * @return whether scroll was consumed
     */
    public boolean onMouseScrolled(double verticalAmount, BooleanSupplier delegateSuper) {
        float pointerX = UIScale.logicalPointerX();
        float pointerY = UIScale.logicalPointerY();
        if (appearancePanel.host().root() != null && appearancePanel.containsPoint(pointerX, pointerY) && appearancePanel.host().dispatchInput(new InputEvent.MouseScroll(pointerX, pointerY, (float) verticalAmount))) {
            return true;
        }
        return delegateSuper.getAsBoolean();
    }

    /**
     * Handles Escape to exit edit mode.
     *
     * @param event         key event from Minecraft
     * @param delegateSuper invoked when Escape is not pressed or after handling other keys
     * @return whether the key event was consumed
     */
    public boolean onKeyPressed(KeyEvent event, BooleanSupplier delegateSuper) {
        showControlsHint = false;
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            HUDManager.INSTANCE.setEditMode(false);
            return true;
        }

        // Keyboard nudge: arrow keys to move selected widget by 1px (or 5px with Shift)
        if (selected != null && !HudEditorChrome.visibleLayoutWidgets(HUDManager.INSTANCE.getWidgets()).contains(selected)) {
            return delegateSuper.getAsBoolean();
        }

        if (selected != null && (event.key() == GLFW.GLFW_KEY_LEFT || event.key() == GLFW.GLFW_KEY_RIGHT || event.key() == GLFW.GLFW_KEY_UP || event.key() == GLFW.GLFW_KEY_DOWN)) {
            boolean isShiftPressed = (event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0;
            float nudgeDistance = isShiftPressed ? 5f : 1f;
            float canvasWidth = UIScale.logicalWidth();
            float canvasHeight = UIScale.logicalHeight();

            float newX = selected.getHudState().getPositionX();
            float newY = selected.getHudState().getPositionY();

            switch (event.key()) {
                case GLFW.GLFW_KEY_LEFT -> newX -= nudgeDistance;
                case GLFW.GLFW_KEY_RIGHT -> newX += nudgeDistance;
                case GLFW.GLFW_KEY_UP -> newY -= nudgeDistance;
                case GLFW.GLFW_KEY_DOWN -> newY += nudgeDistance;
            }

            newX = Mth.clamp(newX, 0f, Math.max(0f, canvasWidth - selected.getScaledWidth()));
            newY = Mth.clamp(newY, 0f, Math.max(0f, canvasHeight - selected.getScaledHeight()));

            selected.getHudState().setPositionX(newX);
            selected.getHudState().setPositionY(newY);
            selected.captureNearestHudAnchorFromPosition(canvasWidth, canvasHeight);
            return true;
        }

        return delegateSuper.getAsBoolean();
    }
}
