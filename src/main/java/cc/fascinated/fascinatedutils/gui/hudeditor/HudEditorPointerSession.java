package cc.fascinated.fascinatedutils.gui.hudeditor;

import cc.fascinated.fascinatedutils.client.ModBranding;
import cc.fascinated.fascinatedutils.gui.UIScale;
import cc.fascinated.fascinatedutils.gui.core.UiFocusIds;
import cc.fascinated.fascinatedutils.gui.screens.HUDEditorScreen;
import cc.fascinated.fascinatedutils.gui.screens.ModSettingsScreen;
import cc.fascinated.fascinatedutils.systems.hud.HUDEditorSnap;
import cc.fascinated.fascinatedutils.systems.hud.HUDManager;
import cc.fascinated.fascinatedutils.systems.hud.HudLayoutCanvas;
import cc.fascinated.fascinatedutils.systems.hud.HudPanel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.BooleanSupplier;

public class HudEditorPointerSession {

    /**
     * Last canvas from {@link HUDEditorScreen#renderCustom}; used between frames so pointer math matches the same
     * bounds as paint.
     */
    private float editorCanvasWidth = HudLayoutCanvas.width();
    private float editorCanvasHeight = HudLayoutCanvas.height();
    @Nullable
    private HudPanel dragging;
    private float dragOffsetX;
    private float dragOffsetY;
    @Nullable
    private HudPanel selected;
    @Nullable
    private HudPanel scalingWidget;
    private float scaleDragReferenceDistance;
    private float scaleDragStartScale;
    private int modMenuFocusScratch = UiFocusIds.NO_FOCUS_ID;
    private float snapGuideX = Float.NaN;
    private float snapGuideY = Float.NaN;
    private boolean snapGuideXIsCenter = false;
    private boolean snapGuideYIsCenter = false;
    @Nullable
    private Screen parentScreen;

    /**
     * Sets the parent screen to return to when the mod settings shell is closed.
     *
     * @param parentScreen the screen instance to restore (typically the {@link HUDEditorScreen} that owns this session)
     */
    public void setParentScreen(@Nullable Screen parentScreen) {
        this.parentScreen = parentScreen;
    }

    /**
     * Updates logical canvas extents for editor pointer and layout math (call once per editor paint).
     *
     * @param canvasWidth  clamped logical width from the active extract context
     * @param canvasHeight clamped logical height from the active extract context
     */
    public void syncEditorCanvas(float canvasWidth, float canvasHeight) {
        editorCanvasWidth = canvasWidth;
        editorCanvasHeight = canvasHeight;
    }

    @Nullable
    public HudPanel selected() {
        return selected;
    }

    @Nullable
    public HudPanel dragging() {
        return dragging;
    }

    @Nullable
    public HudPanel scalingWidget() {
        return scalingWidget;
    }

    public float snapGuideX() {
        return snapGuideX;
    }

    public float snapGuideY() {
        return snapGuideY;
    }

    public boolean snapGuideXIsCenter() {
        return snapGuideXIsCenter;
    }

    public boolean snapGuideYIsCenter() {
        return snapGuideYIsCenter;
    }

    public void clearSnapGuides() {
        snapGuideX = Float.NaN;
        snapGuideY = Float.NaN;
        snapGuideXIsCenter = false;
        snapGuideYIsCenter = false;
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
        clearSnapGuides();
        float pointerX = UIScale.uiPointerX();
        float pointerY = UIScale.uiPointerY();
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && HudEditorOverlays.hitTestModsButton(pointerX, pointerY)) {
            Minecraft.getInstance().setScreen(new ModSettingsScreen(ModBranding.modSettingsScreenTitle(), () -> modMenuFocusScratch, id -> modMenuFocusScratch = id, null, parentScreen));
            return true;
        }
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return delegateSuper.getAsBoolean();
        }
        List<HudPanel> widgetList = HudEditorChrome.visibleLayoutWidgets(HUDManager.INSTANCE.getWidgets());
        // Check action buttons — only active when pointer is over the widget.
        for (int index = widgetList.size() - 1; index >= 0; index--) {
            HudPanel widget = widgetList.get(index);
            if (!widget.containsPoint(pointerX, pointerY)) {
                continue;
            }
            if (HudEditorChrome.settingsButtonContainsPoint(widget, pointerX, pointerY)) {
                Minecraft.getInstance().setScreen(new ModSettingsScreen(ModBranding.modSettingsScreenTitle(), () -> modMenuFocusScratch, id -> modMenuFocusScratch = id, widget.hudSettingsNavigationTarget(), parentScreen));
                return true;
            }
            if (HudEditorChrome.closeButtonContainsPoint(widget, pointerX, pointerY)) {
                widget.hudHostModule().setEnabled(false);
                HUDManager.INSTANCE.saveAll();
                selected = null;
                return true;
            }
        }
        for (int index = widgetList.size() - 1; index >= 0; index--) {
            HudPanel widget = widgetList.get(index);
            if (!widget.containsPoint(pointerX, pointerY)) {
                continue;
            }
            if (widget == selected && HudEditorChrome.scaleHandleContainsPoint(widget, pointerX, pointerY)) {
                scalingWidget = widget;
                selected = widget;
                float anchorX = widget.getHudState().getPositionX();
                float anchorY = widget.getHudState().getPositionY();
                scaleDragReferenceDistance = Math.max(HudEditorChrome.SCALE_DRAG_MIN_REFERENCE_DISTANCE, (float) Math.hypot(pointerX - anchorX, pointerY - anchorY));
                scaleDragStartScale = widget.getHudState().getScale();
                return true;
            }
            dragging = widget;
            selected = widget;
            dragOffsetX = pointerX - widget.getHudState().getPositionX();
            dragOffsetY = pointerY - widget.getHudState().getPositionY();
            return true;
        }
        selected = null;
        dragging = null;
        scalingWidget = null;
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
        float pointerX = UIScale.uiPointerX();
        float pointerY = UIScale.uiPointerY();
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
            float canvasWidth = editorCanvasWidth;
            float canvasHeight = editorCanvasHeight;
            float rawX = pointerX - dragOffsetX;
            float rawY = pointerY - dragOffsetY;
            float widgetWidth = dragging.getScaledWidth();
            float widgetHeight = dragging.getScaledHeight();
            HUDEditorSnap.SnapResult snapped = HUDEditorSnap.snapTopLeft(rawX, rawY, widgetWidth, widgetHeight, canvasWidth, canvasHeight, HudEditorChrome.visibleLayoutWidgets(HUDManager.INSTANCE.getWidgets()), dragging);
            dragging.getHudState().setPositionX(snapped.snappedLeft());
            dragging.getHudState().setPositionY(snapped.snappedTop());
            snapGuideX = snapped.hasVerticalGuide() ? snapped.verticalGuideX() : Float.NaN;
            snapGuideY = snapped.hasHorizontalGuide() ? snapped.horizontalGuideY() : Float.NaN;
            snapGuideXIsCenter = snapped.hasVerticalGuide() && snapped.verticalGuideIsCenter();
            snapGuideYIsCenter = snapped.hasHorizontalGuide() && snapped.horizontalGuideIsCenter();
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
        if (scalingWidget != null) {
            HudPanel widget = scalingWidget;
            scalingWidget = null;
            clearSnapGuides();
            widget.getHudState().setScale(HudEditorChrome.snapScaleNearUnity(widget.getHudState().getScale()));
            float canvasWidth = editorCanvasWidth;
            float canvasHeight = editorCanvasHeight;
            float maxX = Math.max(0f, canvasWidth - widget.getScaledWidth());
            float maxY = Math.max(0f, canvasHeight - widget.getScaledHeight());
            widget.getHudState().setPositionX(Mth.clamp(widget.getHudState().getPositionX(), 0f, maxX));
            widget.getHudState().setPositionY(Mth.clamp(widget.getHudState().getPositionY(), 0f, maxY));
            widget.captureNearestHudAnchorFromPosition(canvasWidth, canvasHeight);
            widget.applyHudAnchorToPosition(canvasWidth, canvasHeight);
            return true;
        }
        if (dragging != null) {
            HudPanel widget = dragging;
            dragging = null;
            clearSnapGuides();
            float canvasWidth = editorCanvasWidth;
            float canvasHeight = editorCanvasHeight;
            float maxX = Math.max(0f, canvasWidth - widget.getScaledWidth());
            float maxY = Math.max(0f, canvasHeight - widget.getScaledHeight());
            widget.getHudState().setPositionX(Mth.clamp(widget.getHudState().getPositionX(), 0f, maxX));
            widget.getHudState().setPositionY(Mth.clamp(widget.getHudState().getPositionY(), 0f, maxY));
            widget.captureNearestHudAnchorFromPosition(canvasWidth, canvasHeight);
            widget.applyHudAnchorToPosition(canvasWidth, canvasHeight);
            return true;
        }
        return delegateSuper.getAsBoolean();
    }

    /**
     * Handles scroll in the HUD editor (no embedded settings panel).
     *
     * @param verticalAmount vertical scroll delta
     * @param delegateSuper  invoked when the scroll is not consumed
     * @return whether scroll was consumed
     */
    public boolean onMouseScrolled(double verticalAmount, BooleanSupplier delegateSuper) {
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
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            HUDManager.INSTANCE.setEditMode(false);
            Minecraft minecraftClient = Minecraft.getInstance();
            if (minecraftClient != null && minecraftClient.screen instanceof HUDEditorScreen) {
                minecraftClient.setScreen(null);
            }
            return true;
        }

        if (selected != null && !HudEditorChrome.visibleLayoutWidgets(HUDManager.INSTANCE.getWidgets()).contains(selected)) {
            return delegateSuper.getAsBoolean();
        }

        if (selected != null && (event.key() == GLFW.GLFW_KEY_LEFT || event.key() == GLFW.GLFW_KEY_RIGHT || event.key() == GLFW.GLFW_KEY_UP || event.key() == GLFW.GLFW_KEY_DOWN)) {
            boolean isShiftPressed = (event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0;
            float nudgeDistance = isShiftPressed ? 5f : 1f;
            float canvasWidth = editorCanvasWidth;
            float canvasHeight = editorCanvasHeight;

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
