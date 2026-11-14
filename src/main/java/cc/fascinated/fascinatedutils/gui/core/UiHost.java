package cc.fascinated.fascinatedutils.gui.core;

import cc.fascinated.fascinatedutils.gui.render.GuiRenderer;
import cc.fascinated.fascinatedutils.debug.perf.core.PerfProbe;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

public class UiHost {
    private final UiInputRouter inputRouter = new UiInputRouter();
    private final UiStateStore stateStore = new UiStateStore(this::markDirty);
    private final Map<String, Integer> persistedScrollOffsets = new HashMap<>();
    private static final long RECOMPOSE_INTERVAL_NANOS = 100_000_000L;

    private static final PerfProbe.Section COMPOSE_SECTION = PerfProbe.INSTANCE.section("ui.compose");
    private static final PerfProbe.Section LAYOUT_SECTION = PerfProbe.INSTANCE.section("ui.layout");
    private static final PerfProbe.Section RENDER_SECTION = PerfProbe.INSTANCE.section("ui.render");

    private UiNode root;
    private boolean dirty = true;
    private long lastComposeNanos;
    @Setter
    private UiComposer composer;

    public UiNode root() {
        return root;
    }

    public UiStateStore stateStore() {
        return stateStore;
    }

    public void setRoot(UiNode root) {
        if (this.root != null) {
            inputRouter.focusManager().clearFocus();
            this.root.unmountSubtree();
        }
        this.root = root;
        if (this.root != null) {
            this.root.mountSubtree();
        }
    }

    public void layout(int positionX, int positionY, int width, int height, GuiRenderer renderFrame) {
        long composeToken = COMPOSE_SECTION.begin();
        recompose();
        COMPOSE_SECTION.end(composeToken);
        if (root == null) {
            return;
        }
        long layoutToken = LAYOUT_SECTION.begin();
        root.layout(renderFrame, positionX, positionY, width, height);
        LAYOUT_SECTION.end(layoutToken);
    }

    public void render(GuiRenderer renderFrame, float deltaSeconds) {
        if (root == null) {
            return;
        }
        long renderToken = RENDER_SECTION.begin();
        root.render(renderFrame, deltaSeconds);
        RENDER_SECTION.end(renderToken);
    }

    public boolean dispatch(UiEvent event) {
        if (root == null) {
            return false;
        }
        if (!(event instanceof UiEvent.PointerMove)) {
            dirty = true;
        }
        return inputRouter.dispatch(root, event);
    }

    public void tick(float deltaSeconds) {
        if (root == null) {
            return;
        }
        root.tickSubtree(deltaSeconds);
    }

    public void dispose() {
        inputRouter.focusManager().clearFocus();
        if (root != null) {
            root.unmountSubtree();
            root = null;
        }
        stateStore.clear();
        composer = null;
    }

    private void markDirty() {
        dirty = true;
    }

    private void recompose() {
        if (composer == null) {
            return;
        }
        long now = System.nanoTime();
        if (root != null && !dirty && now - lastComposeNanos < RECOMPOSE_INTERVAL_NANOS) {
            return;
        }
        dirty = false;
        lastComposeNanos = now;
        String focusedNodeId = inputRouter.focusManager().focusedNode() != null
                ? inputRouter.focusManager().focusedNode().nodeId()
                : null;

        NodeScrollContext.push(persistedScrollOffsets);
        UiNode next;
        try {
            next = composer.compose(stateStore);
        } finally {
            NodeScrollContext.pop();
        }
        if (this.root != null) {
            if (next != null) {
                next.inheritAnimationStateSubtree(this.root);
            }
            this.root.unmountSubtree();
        }
        this.root = next;
        if (this.root != null) {
            this.root.mountSubtree();
        }

        String requestedFocusId = stateStore.pollRequestedFocusNodeId();
        if (requestedFocusId != null) {
            UiNode requestedNode = findById(this.root, requestedFocusId);
            if (requestedNode != null) {
                inputRouter.focusManager().focus(requestedNode);
            }
        } else if (focusedNodeId != null) {
            UiNode restored = findById(this.root, focusedNodeId);
            if (restored != null) {
                inputRouter.focusManager().focus(restored);
            } else {
                inputRouter.focusManager().clearFocus();
            }
        }
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
