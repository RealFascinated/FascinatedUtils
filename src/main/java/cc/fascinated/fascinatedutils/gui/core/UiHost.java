package cc.fascinated.fascinatedutils.gui.core;

import cc.fascinated.fascinatedutils.gui.core.state.UiStateStore;

import cc.fascinated.fascinatedutils.gui.core.input.UiInputRouter;

import cc.fascinated.fascinatedutils.gui.core.input.UiEvent;

import cc.fascinated.fascinatedutils.debug.perf.core.PerfProbe;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

public class UiHost {
    private static final long RECOMPOSE_INTERVAL_NANOS = 1_000_000_000L;

    private static final PerfProbe.Section COMPOSE_SECTION = PerfProbe.INSTANCE.section("ui.compose");
    private static final PerfProbe.Section LAYOUT_SECTION = PerfProbe.INSTANCE.section("ui.layout");
    private static final PerfProbe.Section RENDER_SECTION = PerfProbe.INSTANCE.section("ui.render");

    private final UiTooltips tooltips = new UiTooltips();
    private final UiInputRouter inputRouter = new UiInputRouter(tooltips);
    @Getter
    @Accessors(fluent = true)
    private final UiStateStore stateStore = new UiStateStore(this::markDirty);
    private final UiComposeContext composeContext = new UiComposeContext();
    private final UiReconciler reconciler = new UiReconciler();

    @Getter
    @Accessors(fluent = true)
    private UiNode root;
    private boolean dirty = true;
    private boolean composing;
    private boolean recomposedSinceLayout;
    private long lastComposeNanos;
    @Setter
    private UiComposer composer;
    /**
     * Whether every layout pass recomposes, for content that changes every frame. Reconciliation still carries node
     * state over, and the host's collections are reused, so this stays cheap for small trees.
     */
    @Setter
    private boolean recomposeEveryFrame;

    public void layout(int positionX, int positionY, int width, int height, AlumiteGuiRenderer renderFrame) {
        long composeToken = COMPOSE_SECTION.begin();
        long composeAllocationToken = PerfProbe.INSTANCE.beginAllocation();
        recompose();
        COMPOSE_SECTION.end(composeToken);
        COMPOSE_SECTION.endAllocation(composeAllocationToken);
        if (root == null) {
            return;
        }
        long layoutToken = LAYOUT_SECTION.begin();
        long layoutAllocationToken = PerfProbe.INSTANCE.beginAllocation();
        // Clean subtrees are skipped by layout itself; this only flags nodes whose content changed since last frame.
        root.validateLayout(renderFrame);
        root.layout(renderFrame, positionX, positionY, width, height);
        LAYOUT_SECTION.end(layoutToken);
        LAYOUT_SECTION.endAllocation(layoutAllocationToken);
        if (recomposedSinceLayout) {
            recomposedSinceLayout = false;
            // New nodes under the pointer must hover now, not after the next pointer move.
            inputRouter.refreshHover(root);
        }
    }

    public void render(AlumiteGuiRenderer renderFrame, float deltaSeconds) {
        if (root == null) {
            return;
        }
        long renderToken = RENDER_SECTION.begin();
        long renderAllocationToken = PerfProbe.INSTANCE.beginAllocation();
        root.render(renderFrame, deltaSeconds);
        tooltips.render(renderFrame, root.bounds().positionX() + root.bounds().width(), root.bounds().positionY() + root.bounds().height());
        RENDER_SECTION.end(renderToken);
        RENDER_SECTION.endAllocation(renderAllocationToken);
    }

    public boolean dispatch(UiEvent event) {
        if (root == null) {
            return false;
        }
        // A held pointer (e.g. dragging a scrollbar) mutates state that only a recompose picks up.
        if (!(event instanceof UiEvent.PointerMove) || inputRouter.hasPressed()) {
            dirty = true;
        }
        return inputRouter.dispatch(root, event);
    }

    /**
     * Whether a pointer hit target of the live tree lies under a point, honouring any active focus trap.
     *
     * @param pointerX the pointer X in the host's canvas
     * @param pointerY the pointer Y in the host's canvas
     * @return true when the point is over a node that takes pointer input
     */
    public boolean isOverHitTarget(float pointerX, float pointerY) {
        return root != null && inputRouter.isOverHitTarget(root, pointerX, pointerY);
    }

    public void dispose() {
        inputRouter.reset();
        tooltips.reset();
        if (root != null) {
            root.unmountSubtree();
            root = null;
        }
        stateStore.clear();
        composeContext.clear();
        composer = null;
    }

    private void markDirty() {
        // State written while composing is already reflected in the tree being built.
        if (!composing) {
            dirty = true;
        }
    }

    private void recompose() {
        if (composer == null) {
            return;
        }
        long now = System.nanoTime();
        if (root != null && !recomposeEveryFrame && !dirty && now - lastComposeNanos < RECOMPOSE_INTERVAL_NANOS) {
            return;
        }
        dirty = false;
        lastComposeNanos = now;

        UiNode next;
        composing = true;
        try {
            next = composer.compose(stateStore);
        } finally {
            composing = false;
        }
        UiNode previousRoot = this.root;
        reconciler.reconcile(previousRoot, next, composeContext);
        inputRouter.retarget(reconciler.previousToNext(), reconciler.focusTraps());
        tooltips.retarget(reconciler.previousToNext());
        if (previousRoot != null) {
            previousRoot.unmountSubtree();
        }
        this.root = next;
        if (next != null) {
            next.mountSubtree();
        }
        recomposedSinceLayout = true;
        applyFocusRequest();
    }

    private void applyFocusRequest() {
        stateStore.applyFocusRequest(requestedFocusId -> {
            UiNode requestedNode = findById(root, requestedFocusId);
            if (requestedNode == null) {
                return false;
            }
            inputRouter.focusManager().focus(requestedNode);
            return true;
        });
    }

    private UiNode findById(UiNode node, String nodeId) {
        if (node == null) {
            return null;
        }
        if (nodeId.equals(node.nodeId())) {
            return node;
        }
        for (UiNode child : node.childrenView()) {
            UiNode found = findById(child, nodeId);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
