package cc.fascinated.fascinatedutils.gui.core;

import cc.fascinated.fascinatedutils.gui.render.ClipRegion;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public abstract class UiNode {
    private final List<UiNode> children = new ArrayList<>();
    private final List<UiNode> childrenUnmodifiableView = Collections.unmodifiableList(children);
    private final UiBounds bounds = new UiBounds(0, 0, 0, 0);
    private UiNode parent;
    private String nodeId;
    private boolean visible = true;
    private boolean mounted;
    private boolean inViewport = false;
    private Runnable pointerEnterCallback;
    private Runnable pointerLeaveCallback;
    private Supplier<String> tooltipSupplier;

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

    /**
     * Whether this node's bounds are currently intersecting the active clip region and therefore
     * visible within the rendered viewport. Updated each frame during {@link #render}.
     */
    public boolean inViewport() {
        return inViewport;
    }

    public List<UiNode> childrenView() {
        return childrenUnmodifiableView;
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

    public void layout(AlumiteGuiRenderer renderFrame, int positionX, int positionY, int width, int height) {
        bounds.set(positionX, positionY, width, height);
        for (UiNode childNode : children) {
            childNode.layout(renderFrame, positionX, positionY, width, height);
        }
    }

    /**
     * Shift this node and everything below it without re-running layout. Layout sizes never depend on position, so a
     * parent can lay a child out once and then slide it into its final slot.
     *
     * @param deltaX horizontal shift in pixels
     * @param deltaY vertical shift in pixels
     */
    public void translate(int deltaX, int deltaY) {
        if (deltaX == 0 && deltaY == 0) {
            return;
        }
        bounds.translate(deltaX, deltaY);
        onTranslated(deltaX, deltaY);
        for (UiNode childNode : children) {
            childNode.translate(deltaX, deltaY);
        }
    }

    /**
     * Hook for nodes that cache absolute coordinates outside {@link #bounds()}.
     */
    protected void onTranslated(int deltaX, int deltaY) {
    }

    public void render(AlumiteGuiRenderer renderFrame, float deltaSeconds) {
        if (!visible) {
            return;
        }

        ClipRegion clip = renderFrame.currentClip();
        inViewport = clip == null || bounds.intersects(clip);

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

    /**
     * Whether keyboard focus must stay inside this node's subtree while it is visible, as for modal popups.
     *
     * @return true when Tab navigation and key input are confined to this subtree
     */
    public boolean trapsFocus() {
        return false;
    }

    /**
     * Sets the tooltip shown after the pointer rests on this node.
     *
     * @param tooltipSupplier supplies the tooltip text, or null to remove the tooltip
     * @return this node
     */
    public UiNode setTooltip(Supplier<String> tooltipSupplier) {
        this.tooltipSupplier = tooltipSupplier;
        return this;
    }

    /**
     * Sets a fixed tooltip text.
     *
     * @param tooltipText the tooltip text, or null to remove the tooltip
     * @return this node
     */
    public UiNode setTooltip(String tooltipText) {
        this.tooltipSupplier = tooltipText == null ? null : () -> tooltipText;
        return this;
    }

    public Supplier<String> tooltip() {
        return tooltipSupplier;
    }

    /**
     * Called on each ancestor when a descendant gains focus through keyboard navigation, so scrolling containers can
     * bring it into view.
     *
     * @param descendant the node that gained focus
     */
    public void revealDescendant(UiNode descendant) {
    }

    public void onFocusGained() {
    }

    public void onFocusLost() {
    }

    public boolean clipChildren() {
        return false;
    }

    public boolean blocksHitWhenEmpty() {
        return false;
    }

    public boolean onPointerPress(float pointerX, float pointerY, int button) {
        return false;
    }

    public boolean onPointerRelease(float pointerX, float pointerY, int button) {
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

    protected void renderSelf(AlumiteGuiRenderer renderFrame, float deltaSeconds) {
    }

    /**
     * Copy in-flight animation state from the equivalent node of the previous composed tree so animations survive recomposition.
     *
     * @param previous the node that occupied the same position in the previous tree, always the same class as this node
     */
    protected void inheritAnimationState(UiNode previous) {
    }

    void inheritAnimationStateSubtree(UiNode previous) {
        if (previous == null || previous.getClass() != getClass()) {
            return;
        }
        inheritAnimationState(previous);
        List<UiNode> previousChildren = previous.children;
        if (previousChildren.size() != children.size()) {
            return;
        }
        for (int index = 0; index < children.size(); index++) {
            children.get(index).inheritAnimationStateSubtree(previousChildren.get(index));
        }
    }

    void mountSubtree() {
        if (mounted) {
            return;
        }
        mounted = true;
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
        mounted = false;
    }

    /**
     * Recursively marks this node and all descendants as outside the viewport.
     */
    void notifySubtreeViewportLeave() {
        inViewport = false;
        for (UiNode childNode : children) {
            childNode.notifySubtreeViewportLeave();
        }
    }
}
