package cc.fascinated.fascinatedutils.gui2.core;

import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class UiNode {
    private final List<UiNode> children = new ArrayList<>();
    private final UiBounds bounds = new UiBounds(0, 0, 0, 0);
    private UiNode parent;
    private String nodeId;
    private boolean visible = true;
    private boolean enabled = true;
    private boolean mounted;

    public UiNode parent() {
        return parent;
    }

    public UiBounds bounds() {
        return bounds;
    }

    public String nodeId() {
        return nodeId;
    }

    public UiNode setNodeId(String nodeId) {
        this.nodeId = nodeId;
        return this;
    }

    public String debugName() {
        if (nodeId != null && !nodeId.isBlank()) {
            return nodeId;
        }
        return getClass().getSimpleName();
    }

    public String debugPath() {
        if (parent == null) {
            return debugName();
        }
        return parent.debugPath() + "/" + debugName();
    }

    public boolean visible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<UiNode> childrenView() {
        return Collections.unmodifiableList(children);
    }

    public void addChild(UiNode childNode) {
        if (childNode == null) {
            return;
        }
        if (childNode.parent != null) {
            childNode.parent.removeChild(childNode);
        }
        childNode.parent = this;
        children.add(childNode);
        if (mounted) {
            childNode.mountSubtree();
        }
    }

    public void removeChild(UiNode childNode) {
        if (childNode == null) {
            return;
        }
        if (children.remove(childNode)) {
            if (mounted) {
                childNode.unmountSubtree();
            }
            childNode.parent = null;
        }
    }

    public void clearChildren() {
        for (UiNode childNode : new ArrayList<>(children)) {
            if (mounted) {
                childNode.unmountSubtree();
            }
            childNode.parent = null;
        }
        children.clear();
    }

    public void layout(RenderFrame renderFrame, int positionX, int positionY, int width, int height) {
        bounds.set(positionX, positionY, width, height);
        for (UiNode childNode : children) {
            childNode.layout(renderFrame, positionX, positionY, width, height);
        }
    }

    public void render(RenderFrame renderFrame, float deltaSeconds) {
        if (!visible) {
            return;
        }
        renderSelf(renderFrame, deltaSeconds);
        if (children.isEmpty()) {
            return;
        }
        boolean shouldClipChildren = clipChildren();
        if (shouldClipChildren) {
            renderFrame.pushClip(bounds.asClipRegion());
        }
        try {
            for (UiNode childNode : children) {
                renderFrame.flushText();
                childNode.render(renderFrame, deltaSeconds);
            }
        }
        finally {
            if (shouldClipChildren) {
                renderFrame.popClip();
            }
        }
    }

    public boolean contains(float pointerX, float pointerY) {
        return visible && bounds.contains(pointerX, pointerY);
    }

    public boolean focusable() {
        return false;
    }

    public void onFocusGained() {
    }

    public void onFocusLost() {
    }

    public void onTick(float deltaSeconds) {
    }

    public void onMount() {
    }

    public void onUnmount() {
    }

    public boolean clipChildren() {
        return false;
    }

    public boolean blocksHitWhenEmpty() {
        return false;
    }

    public boolean capturesPointerPress(float pointerX, float pointerY, int button) {
        return false;
    }

    public boolean onPointerPress(float pointerX, float pointerY, int button) {
        return false;
    }

    public boolean capturesPointerRelease(float pointerX, float pointerY, int button) {
        return false;
    }

    public boolean onPointerRelease(float pointerX, float pointerY, int button) {
        return false;
    }

    public boolean capturesClick(float pointerX, float pointerY, int button) {
        return false;
    }

    public boolean onClick(float pointerX, float pointerY, int button) {
        return false;
    }

    public boolean onPointerMove(float pointerX, float pointerY) {
        return false;
    }

    public boolean onPointerEnter(float pointerX, float pointerY) {
        return false;
    }

    public boolean onPointerLeave(float pointerX, float pointerY) {
        return false;
    }

    public boolean capturesKeyPress(int keyCode, int modifiers) {
        return false;
    }

    public boolean onKeyPress(int keyCode, int modifiers) {
        return false;
    }

    public boolean capturesCharType(char character) {
        return false;
    }

    public boolean onCharType(char character) {
        return false;
    }

    public boolean onPointerScroll(float pointerX, float pointerY, float delta) {
        return false;
    }

    protected void renderSelf(RenderFrame renderFrame, float deltaSeconds) {
    }

    void tickSubtree(float deltaSeconds) {
        onTick(deltaSeconds);
        for (UiNode childNode : children) {
            childNode.tickSubtree(deltaSeconds);
        }
    }

    void mountSubtree() {
        if (mounted) {
            return;
        }
        mounted = true;
        onMount();
        for (UiNode childNode : children) {
            childNode.mountSubtree();
        }
    }

    void unmountSubtree() {
        if (!mounted) {
            return;
        }
        for (UiNode childNode : children) {
            childNode.unmountSubtree();
        }
        onUnmount();
        mounted = false;
    }
}
