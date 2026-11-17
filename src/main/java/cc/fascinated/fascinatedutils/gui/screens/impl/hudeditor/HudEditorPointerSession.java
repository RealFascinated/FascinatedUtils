package cc.fascinated.fascinatedutils.gui.screens.impl.hudeditor;

import cc.fascinated.fascinatedutils.systems.hud.editor.HudEditorSnap;
import cc.fascinated.fascinatedutils.systems.hud.HudManager;
import cc.fascinated.fascinatedutils.systems.hud.layout.HudLayoutCanvas;
import cc.fascinated.fascinatedutils.systems.hud.panel.HudPanel;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * Selection, drag, scale and snap-guide state for the HUD editor. The editor node tree is rebuilt every frame, so this
 * object holds everything that must survive between frames.
 */
@Accessors(fluent = true)
public class HudEditorPointerSession {

    @Nullable
    @Getter
    private HudPanel dragging;
    private float dragOffsetX;
    private float dragOffsetY;
    @Nullable
    @Getter
    private HudPanel selected;
    @Nullable
    @Getter
    private HudPanel scalingWidget;
    private float scaleDragReferenceDistance;
    private float scaleDragStartScale;
    @Getter
    private float snapGuideX = Float.NaN;
    @Getter
    private float snapGuideY = Float.NaN;
    @Getter
    private boolean snapGuideXIsCenter = false;
    @Getter
    private boolean snapGuideYIsCenter = false;

    public void clearSnapGuides() {
        snapGuideX = Float.NaN;
        snapGuideY = Float.NaN;
        snapGuideXIsCenter = false;
        snapGuideYIsCenter = false;
    }

    /**
     * Clears the selection and any drag in progress.
     */
    public void clearSelection() {
        selected = null;
        dragging = null;
        scalingWidget = null;
        clearSnapGuides();
    }

    /**
     * Whether the editor is mid drag or mid scale.
     *
     * @return true when a panel is being moved or scaled
     */
    public boolean interacting() {
        return dragging != null || scalingWidget != null;
    }

    /**
     * Selects a panel and starts dragging it from the pointer position.
     *
     * @param widget   panel pressed
     * @param pointerX logical pointer X
     * @param pointerY logical pointer Y
     */
    public void beginDrag(HudPanel widget, float pointerX, float pointerY) {
        clearSnapGuides();
        dragging = widget;
        selected = widget;
        dragOffsetX = pointerX - widget.getHudState().getPositionX();
        dragOffsetY = pointerY - widget.getHudState().getPositionY();
    }

    /**
     * Starts scaling a panel from its scale handle.
     *
     * @param widget   panel being scaled
     * @param pointerX logical pointer X
     * @param pointerY logical pointer Y
     */
    public void beginScale(HudPanel widget, float pointerX, float pointerY) {
        clearSnapGuides();
        scalingWidget = widget;
        selected = widget;
        float dragOriginX = HudEditorChrome.scaleDragOriginX(widget);
        float dragOriginY = HudEditorChrome.scaleDragOriginY(widget);
        scaleDragReferenceDistance = Math.max(HudEditorChrome.SCALE_DRAG_MIN_REFERENCE_DISTANCE, (float) Math.hypot(pointerX - dragOriginX, pointerY - dragOriginY));
        scaleDragStartScale = widget.getHudState().getScale();
    }

    /**
     * Moves the dragged panel toward the pointer, snapping to guides.
     *
     * @param pointerX logical pointer X
     * @param pointerY logical pointer Y
     */
    public void dragTo(float pointerX, float pointerY) {
        if (dragging == null) {
            return;
        }
        float rawX = pointerX - dragOffsetX;
        float rawY = pointerY - dragOffsetY;
        HudEditorSnap.SnapResult snapped = HudEditorSnap.snapTopLeft(rawX, rawY, dragging.getScaledWidth(), dragging.getScaledHeight(), HudLayoutCanvas.width(), HudLayoutCanvas.height(), HudEditorChrome.visibleLayoutWidgets(HudManager.INSTANCE.getWidgets()), dragging);
        dragging.getHudState().setPositionX(snapped.snappedLeft());
        dragging.getHudState().setPositionY(snapped.snappedTop());
        snapGuideX = snapped.hasVerticalGuide() ? snapped.verticalGuideX() : Float.NaN;
        snapGuideY = snapped.hasHorizontalGuide() ? snapped.horizontalGuideY() : Float.NaN;
        snapGuideXIsCenter = snapped.hasVerticalGuide() && snapped.verticalGuideIsCenter();
        snapGuideYIsCenter = snapped.hasHorizontalGuide() && snapped.horizontalGuideIsCenter();
    }

    /**
     * Rescales the panel being scaled based on pointer distance from its fixed corner.
     *
     * @param pointerX logical pointer X
     * @param pointerY logical pointer Y
     */
    public void scaleTo(float pointerX, float pointerY) {
        if (scalingWidget == null) {
            return;
        }
        clearSnapGuides();
        float dragOriginX = HudEditorChrome.scaleDragOriginX(scalingWidget);
        float dragOriginY = HudEditorChrome.scaleDragOriginY(scalingWidget);
        float pointerDistance = Math.max(HudEditorChrome.SCALE_DRAG_MIN_REFERENCE_DISTANCE, (float) Math.hypot(pointerX - dragOriginX, pointerY - dragOriginY));
        float nextScale = Mth.clamp(scaleDragStartScale * (pointerDistance / scaleDragReferenceDistance), HudPanel.MIN_SCALE, HudPanel.MAX_SCALE);
        scalingWidget.getHudState().setScale(HudEditorChrome.snapScaleNearUnity(nextScale));
        float shiftX = dragOriginX - HudEditorChrome.scaleDragOriginX(scalingWidget);
        float shiftY = dragOriginY - HudEditorChrome.scaleDragOriginY(scalingWidget);
        scalingWidget.getHudState().setPositionX(scalingWidget.getHudState().getPositionX() + shiftX);
        scalingWidget.getHudState().setPositionY(scalingWidget.getHudState().getPositionY() + shiftY);
    }

    /**
     * Finishes any drag or scale, clamping the panel onto the canvas and re-capturing its anchor.
     */
    public void endInteraction() {
        HudPanel widget = scalingWidget != null ? scalingWidget : dragging;
        boolean wasScaling = scalingWidget != null;
        scalingWidget = null;
        dragging = null;
        clearSnapGuides();
        if (widget == null) {
            return;
        }
        float canvasWidth = HudLayoutCanvas.width();
        float canvasHeight = HudLayoutCanvas.height();
        if (wasScaling) {
            widget.getHudState().setScale(HudEditorChrome.snapScaleNearUnity(widget.getHudState().getScale()));
        }
        float maxX = Math.max(0f, canvasWidth - widget.getScaledWidth());
        float maxY = Math.max(0f, canvasHeight - widget.getScaledHeight());
        widget.getHudState().setPositionX(Mth.clamp(widget.getHudState().getPositionX(), 0f, maxX));
        widget.getHudState().setPositionY(Mth.clamp(widget.getHudState().getPositionY(), 0f, maxY));
        widget.captureNearestHudAnchorFromPosition(canvasWidth, canvasHeight);
        widget.applyHudAnchorToPosition(canvasWidth, canvasHeight);
    }

    /**
     * Hides a panel and drops the selection if it was selected. The layout is saved when the editor closes.
     *
     * @param widget panel to disable
     */
    public void disable(HudPanel widget) {
        widget.hideFromHud();
        if (selected == widget) {
            selected = null;
        }
    }

    /**
     * Nudges the selected panel with the arrow keys (shift for a larger step).
     *
     * @param keyCode   GLFW key code
     * @param modifiers GLFW modifier bits
     * @return whether the key was an arrow key applied to a selected panel
     */
    public boolean nudgeSelected(int keyCode, int modifiers) {
        if (selected == null || !HudEditorChrome.visibleLayoutWidgets(HudManager.INSTANCE.getWidgets()).contains(selected)) {
            return false;
        }
        float nudgeDistance = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0 ? 5f : 1f;
        float newX = selected.getHudState().getPositionX();
        float newY = selected.getHudState().getPositionY();
        switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT -> newX -= nudgeDistance;
            case GLFW.GLFW_KEY_RIGHT -> newX += nudgeDistance;
            case GLFW.GLFW_KEY_UP -> newY -= nudgeDistance;
            case GLFW.GLFW_KEY_DOWN -> newY += nudgeDistance;
            default -> {
                return false;
            }
        }
        float canvasWidth = HudLayoutCanvas.width();
        float canvasHeight = HudLayoutCanvas.height();
        selected.getHudState().setPositionX(Mth.clamp(newX, 0f, Math.max(0f, canvasWidth - selected.getScaledWidth())));
        selected.getHudState().setPositionY(Mth.clamp(newY, 0f, Math.max(0f, canvasHeight - selected.getScaledHeight())));
        selected.captureNearestHudAnchorFromPosition(canvasWidth, canvasHeight);
        return true;
    }
}
