package cc.fascinated.fascinatedutils.gui2.core;

import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;

public class UiHost {
    private final UiInputRouter inputRouter = new UiInputRouter();
    private final UiStateStore stateStore = new UiStateStore(() -> {});
    private UiNode root;
    private UiComposer composer;

    public UiNode root() {
        return root;
    }

    public UiStateStore stateStore() {
        return stateStore;
    }

    public void setComposer(UiComposer composer) {
        this.composer = composer;
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

    public void layout(int positionX, int positionY, int width, int height, RenderFrame renderFrame) {
        recompose();
        if (root == null) {
            return;
        }
        root.layout(renderFrame, positionX, positionY, width, height);
    }

    public void render(RenderFrame renderFrame, float deltaSeconds) {
        if (root == null) {
            return;
        }
        root.render(renderFrame, deltaSeconds);
    }

    public boolean dispatch(UiEvent event) {
        if (root == null) {
            return false;
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

    private void recompose() {
        if (composer == null) {
            return;
        }
        String focusedNodeId = inputRouter.focusManager().focusedNode() != null
                ? inputRouter.focusManager().focusedNode().nodeId()
                : null;

        UiNode next = composer.compose(stateStore);
        if (this.root != null) {
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
