package cc.fascinated.fascinatedutils.gui2.core;

import cc.fascinated.fascinatedutils.gui2.render.ClipRegion;
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
    private boolean inViewport = false;
    private Runnable enterViewportCallback;
    private Runnable leaveViewportCallback;
    private Runnable pointerEnterCallback;
    private Runnable pointerLeaveCallback;

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
        if (this.visible == visible) {
            return;
        }
        this.visible = visible;
        if (!visible) {
            notifySubtreeViewportLeave();
        }
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Whether this node's bounds are currently intersecting the active clip region and therefore
     * visible within the rendered viewport. Updated each frame during {@link #render}.
     */
    public boolean inViewport() {
        return inViewport;
    }

    /**
     * Sets a callback invoked the first time this node's bounds enter the rendered viewport.
     *
     * @param callback called when the node becomes visible in the viewport
     */
    public UiNode setOnEnterViewport(Runnable callback) {
        this.enterViewportCallback = callback;
        return this;
    }

    /**
     * Sets a callback invoked when this node's bounds leave the rendered viewport, or when the
     * node is hidden or unmounted.
     *
     * @param callback called when the node leaves the viewport
     */
    public UiNode setOnLeaveViewport(Runnable callback) {
        this.leaveViewportCallback = callback;
        return this;
    }

    public List<UiNode> childrenView() {
        return Collections.unmodifiableList(children);
    }

    public UiNode addChild(UiNode childNode) {
        if (childNode == null) {
            return this;
        }
        if (childNode.parent != null) {
            childNode.parent.removeChild(childNode);
        }
        childNode.parent = this;
        children.add(childNode);
        if (mounted) {
            childNode.mountSubtree();
        }
        return this;
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

        ClipRegion clip = renderFrame.currentClip();
        boolean nowInViewport = clip == null || bounds.intersects(clip);
        if (nowInViewport != inViewport) {
            inViewport = nowInViewport;
            if (inViewport) {
                if (enterViewportCallback != null) enterViewportCallback.run();
            } else {
                if (leaveViewportCallback != null) leaveViewportCallback.run();
            }
        }

        if (inViewport) {
            renderSelf(renderFrame, deltaSeconds);
        }

        if (children.isEmpty()) {
            return;
        }

        boolean shouldClipChildren = clipChildren();

        // If this node clips its children and is itself out of the viewport, no child can be
        // visible — propagate the leave notification and skip the render pass entirely.
        if (!inViewport && shouldClipChildren) {
            for (UiNode childNode : children) {
                childNode.notifySubtreeViewportLeave();
            }
            return;
        }

        if (shouldClipChildren) {
            renderFrame.pushClip(bounds.asClipRegion());
        }
        try {
            for (UiNode childNode : children) {
                renderFrame.flushText();
                childNode.render(renderFrame, deltaSeconds);
            }
        } finally {
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

    public UiNode setOnPointerEnter(Runnable callback) {
        this.pointerEnterCallback = callback;
        return this;
    }

    public UiNode setOnPointerLeave(Runnable callback) {
        this.pointerLeaveCallback = callback;
        return this;
    }

    public boolean onPointerEnter(float pointerX, float pointerY) {
        if (pointerEnterCallback != null) pointerEnterCallback.run();
        return false;
    }

    public boolean onPointerLeave(float pointerX, float pointerY) {
        if (pointerLeaveCallback != null) pointerLeaveCallback.run();
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
        inViewport = false;
        for (UiNode childNode : children) {
            childNode.unmountSubtree();
        }
        onUnmount();
        mounted = false;
    }

    /**
     * Recursively notifies this node and all descendants that they have left the viewport,
     * firing {@link #onLeaveViewport()} on each node that was previously in the viewport.
     */
    void notifySubtreeViewportLeave() {
        if (inViewport) {
            inViewport = false;
            if (leaveViewportCallback != null) leaveViewportCallback.run();
        }
        for (UiNode childNode : children) {
            childNode.notifySubtreeViewportLeave();
        }
    }
}
