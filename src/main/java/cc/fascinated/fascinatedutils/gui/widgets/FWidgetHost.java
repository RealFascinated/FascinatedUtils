package cc.fascinated.fascinatedutils.gui.widgets;

import cc.fascinated.fascinatedutils.gui.core.*;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import lombok.Setter;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public class FWidgetHost {
    private final FInputDispatcher inputDispatcher = new FInputDispatcher();
    @Setter
    private FWidget root;
    private int focusedId = UiFocusIds.NO_FOCUS_ID;
    private IntConsumer externalFocusSink;
    private IntSupplier externalFocusSource;

    private static void tickAnimsRecursive(FWidget node, float deltaSeconds) {
        if (node == null) {
            return;
        }
        if (node instanceof FAnimatable animatable) {
            animatable.tickAnims(deltaSeconds);
        }
        for (FWidget child : node.childrenView()) {
            tickAnimsRecursive(child, deltaSeconds);
        }
    }

    private static void renderOverlaysRecursive(FWidget node, GuiRenderer renderer, UiFrameContext frame, float deltaSeconds) {
        if (node == null || !node.visible()) {
            return;
        }
        for (FWidget child : node.childrenView()) {
            renderOverlaysRecursive(child, renderer, frame, deltaSeconds);
        }
        node.renderOverlayAfterChildren(renderer, frame, deltaSeconds);
    }

    public FWidget root() {
        return root;
    }

    public void setFocusSync(IntSupplier getFocusId, IntConsumer setFocusId) {
        this.externalFocusSource = getFocusId;
        this.externalFocusSink = setFocusId;
    }

    public int focusedId() {
        return focusedId;
    }

    public void tickAnimations(float deltaSeconds) {
        tickAnimsRecursive(root, deltaSeconds);
    }

    /**
     * Layout, sync pointer hover via a move dispatch, then paint using the same hit-test as input.
     */
    public void layoutAndRender(GuiRenderer renderer, float layoutX, float layoutY, float layoutWidth, float layoutHeight, float pointerX, float pointerY, float deltaSeconds) {
        layoutOnly(renderer, layoutX, layoutY, layoutWidth, layoutHeight);
        dispatchInput(new InputEvent.MouseMove(pointerX, pointerY));
        UiFrameContext frame = UiFrameContext.hitTest(root, pointerX, pointerY, focusedId);
        renderOnly(renderer, frame, deltaSeconds);
    }

    public void layoutOnly(GuiRenderer renderer, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
        if (root != null) {
            root.layout(renderer, layoutX, layoutY, layoutWidth, layoutHeight);
        }
    }

    public void renderOnly(GuiRenderer renderer, UiFrameContext frame, float deltaSeconds) {
        if (root != null) {
            root.render(renderer, frame, deltaSeconds);
            renderOverlaysRecursive(root, renderer, frame, deltaSeconds);
        }
    }

    public void renderOverlaysOnly(GuiRenderer renderer, UiFrameContext frame, float deltaSeconds) {
        renderOverlaysRecursive(root, renderer, frame, deltaSeconds);
    }

    public boolean dispatchInput(InputEvent event) {
        if (externalFocusSource != null) {
            focusedId = externalFocusSource.getAsInt();
        }
        GuiFocusState.setFocusedId(focusedId);
        boolean handled = inputDispatcher.dispatch(root, event, focusedId, id -> focusedId = id, () -> focusedId);
        GuiFocusState.setFocusedId(focusedId);
        if (externalFocusSink != null) {
            externalFocusSink.accept(focusedId);
        }
        return handled;
    }

    public UiPointerCursor pointerCursorAt(float pointerX, float pointerY) {
        return inputDispatcher.pointerCursorAt(root, pointerX, pointerY);
    }

    public void dispose() {
        root = null;
    }
}
