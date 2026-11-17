package cc.fascinated.fascinatedutils.gui.core;

import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;

import cc.fascinated.fascinatedutils.gui.layout.AxisConstraints;
import cc.fascinated.fascinatedutils.gui.layout.Axis;
import cc.fascinated.fascinatedutils.gui.layout.MeasuredSize;
import cc.fascinated.fascinatedutils.gui.render.ClipRegion;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public abstract class UiNode {
    /**
     * Whether a class supplies its own {@code layout} instead of the measure and arrange passes. Such nodes cannot
     * report when their layout inputs change, so they are laid out every frame.
     */
    private static final ClassValue<Boolean> SELF_LAID_OUT = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            try {
                Class<?> owner = type.getMethod("layout", AlumiteGuiRenderer.class, int.class, int.class, int.class, int.class).getDeclaringClass();
                return owner != UiNode.class && owner != PositionedNode.class;
            } catch (NoSuchMethodException exception) {
                return false;
            }
        }
    };

    private List<UiNode> children = List.of();
    private List<UiNode> childrenUnmodifiableView = List.of();
    @Getter
    @Accessors(fluent = true)
    private final UiBounds bounds = new UiBounds(0, 0, 0, 0);
    @Getter
    @Accessors(fluent = true)
    private UiNode parent;
    @Getter
    @Accessors(fluent = true)
    private String nodeId;
    @Getter
    @Accessors(fluent = true)
    private boolean visible = true;
    private boolean mounted;
    /**
     * Whether this node's bounds are currently intersecting the active clip region and therefore
     * visible within the rendered viewport. Updated each frame during {@link #render}.
     */
    @Getter
    @Accessors(fluent = true)
    private boolean inViewport = false;
    /**
     * Whether the pointer currently rests on this node or one of its descendants. Maintained by the input router and
     * carried across recomposition.
     */
    @Getter
    private boolean hovered;
    /**
     * Whether this node consumed the pointer press that is still held. Carried across recomposition.
     */
    @Getter
    private boolean pressed;
    /**
     * Whether this node holds keyboard focus. Carried across recomposition.
     */
    @Getter
    private boolean focused;
    private boolean focusVisible;
    private boolean layoutDirty = true;
    private boolean measureValid;
    private MeasuredSize measuredSize;
    private int measuredMaxWidth;
    private int measuredMaxHeight;
    private int arrangedMaxWidth;
    private int arrangedMaxHeight;
    private Runnable pointerEnterCallback;
    private Runnable pointerLeaveCallback;
    private Supplier<String> tooltipSupplier;

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

    public void setVisible(boolean visible) {
        if (this.visible == visible) {
            return;
        }
        this.visible = visible;
        if (!visible) {
            bounds.set(bounds.positionX(), bounds.positionY(), 0, 0);
            notifySubtreeViewportLeave();
        }
        markLayoutDirty();
    }

    public void markHovered(boolean hovered) {
        this.hovered = hovered;
    }

    public void markPressed(boolean pressed) {
        this.pressed = pressed;
    }

    public void markFocused(boolean focused, boolean focusVisible) {
        this.focused = focused;
        this.focusVisible = focused && focusVisible;
    }

    /**
     * Whether this node holds focus and should show a focus indicator: focus that arrived through the keyboard or
     * code, or any key press since a pointer click focused it. Pointer-focused nodes return false until a key is used.
     *
     * @return true when a focus ring should be drawn
     */
    public boolean isFocusVisible() {
        return focused && focusVisible;
    }

    public List<UiNode> childrenView() {
        return childrenUnmodifiableView;
    }

    /**
     * Appends a child node.
     *
     * @param childNode the child to add; null is ignored
     * @return this node
     * @throws IllegalArgumentException when the child is this node or one of its ancestors
     */
    public UiNode addChild(UiNode childNode) {
        if (childNode == null) {
            return this;
        }
        for (UiNode ancestor = this; ancestor != null; ancestor = ancestor.parent) {
            if (ancestor == childNode) {
                throw new IllegalArgumentException("Cannot add " + childNode.debugName() + " as a child of " + debugName() + ": it would create a cycle");
            }
        }
        if (childNode.parent != null) {
            childNode.parent.removeChild(childNode);
        }
        childNode.parent = this;
        if (children.isEmpty()) {
            ArrayList<UiNode> mutableChildren = new ArrayList<>();
            children = mutableChildren;
            childrenUnmodifiableView = Collections.unmodifiableList(mutableChildren);
        }
        children.add(childNode);
        markLayoutDirty();
        if (mounted) {
            childNode.mountSubtree();
        }
        return this;
    }

    public void removeChild(UiNode childNode) {
        if (childNode == null) {
            return;
        }
        if (!children.isEmpty() && children.remove(childNode)) {
            if (mounted) {
                childNode.unmountSubtree();
            }
            childNode.parent = null;
            markLayoutDirty();
        }
    }

    /**
     * Flags this node and every ancestor as needing measure and arrange again, and drops their cached measurements.
     * Call it whenever something that affects this node's size or its children's placement changes outside of
     * composition. Box-property setters, {@link #addChild}, {@link #removeChild} and {@link #setVisible} call it for you.
     */
    public final void markLayoutDirty() {
        for (UiNode node = this; node != null; node = node.parent) {
            if (node != this && node.layoutDirty && !node.measureValid) {
                return;
            }
            node.layoutDirty = true;
            node.measureValid = false;
        }
    }

    /**
     * Whether this node or something below it still has to be arranged.
     *
     * @return true when the next layout pass will visit this node
     */
    public final boolean layoutDirty() {
        return layoutDirty;
    }

    /**
     * Measure pass: the size this node wants when at most the given size is available. The result is cached per node
     * and keyed on the constraints until {@link #markLayoutDirty()} is called, so repeated calls are free.
     *
     * @param renderFrame the active renderer, used to measure text
     * @param maxWidth    the available width in pixels
     * @param maxHeight   the available height in pixels
     * @return the desired border-box size, excluding this node's own margins
     */
    public final MeasuredSize measure(AlumiteGuiRenderer renderFrame, int maxWidth, int maxHeight) {
        if (measureValid && measuredMaxWidth == maxWidth && measuredMaxHeight == maxHeight) {
            return measuredSize;
        }
        measuredMaxWidth = maxWidth;
        measuredMaxHeight = maxHeight;
        measuredSize = doMeasure(renderFrame, maxWidth, maxHeight);
        measureValid = true;
        return measuredSize;
    }

    /**
     * The width constraint of the latest {@link #measure} call.
     *
     * @return the available width that measurement used
     */
    protected final int measuredMaxWidth() {
        return measuredMaxWidth;
    }

    /**
     * The height constraint of the latest {@link #measure} call.
     *
     * @return the available height that measurement used
     */
    protected final int measuredMaxHeight() {
        return measuredMaxHeight;
    }

    /**
     * Computes the desired size for {@link #measure}. The default fills the available space. Implementations must
     * depend only on the constraints and this node's own state, never on its position.
     *
     * @param renderFrame the active renderer, used to measure text
     * @param maxWidth    the available width in pixels
     * @param maxHeight   the available height in pixels
     * @return the desired border-box size
     */
    protected MeasuredSize doMeasure(AlumiteGuiRenderer renderFrame, int maxWidth, int maxHeight) {
        return new MeasuredSize(maxWidth, maxHeight);
    }

    /**
     * Places this node inside a slot: measures it, resolves its box inside the slot and arranges it there. The default
     * fills the measured size at the slot origin. Overriding this method opts the node out of change tracking, so it is
     * laid out every frame; prefer {@link #doMeasure} and {@link #arrangeChildren}, and do not call
     * {@code super.layout} from an override.
     *
     * @param renderFrame the active renderer
     * @param slotX       the slot's left edge
     * @param slotY       the slot's top edge
     * @param slotWidth   the slot's width
     * @param slotHeight  the slot's height
     */
    public void layout(AlumiteGuiRenderer renderFrame, int slotX, int slotY, int slotWidth, int slotHeight) {
        if (!visible) {
            return;
        }
        MeasuredSize size = measure(renderFrame, slotWidth, slotHeight);
        arrange(renderFrame, slotX, slotY, size.width(), size.height());
    }

    /**
     * Arrange pass: sets this node's final bounds and positions its children, top-down and once. A node that is clean,
     * unchanged in size and merely moved is shifted with its subtree instead of being arranged again.
     *
     * @param renderFrame the active renderer
     * @param positionX   the final left edge
     * @param positionY   the final top edge
     * @param width       the final width
     * @param height      the final height
     */
    public final void arrange(AlumiteGuiRenderer renderFrame, int positionX, int positionY, int width, int height) {
        if (SELF_LAID_OUT.get(getClass())) {
            layout(renderFrame, positionX, positionY, width, height);
            return;
        }
        if (!layoutDirty && bounds.width() == Math.max(0, width) && bounds.height() == Math.max(0, height)
                && arrangedMaxWidth == measuredMaxWidth && arrangedMaxHeight == measuredMaxHeight) {
            translate(positionX - bounds.positionX(), positionY - bounds.positionY());
            return;
        }
        layoutDirty = false;
        arrangedMaxWidth = measuredMaxWidth;
        arrangedMaxHeight = measuredMaxHeight;
        bounds.set(positionX, positionY, width, height);
        arrangeChildren(renderFrame, positionX, positionY, bounds.width(), bounds.height());
    }

    /**
     * Positions the children inside this node's final box. The default gives every visible child the whole box as its
     * slot; invisible children are skipped and take no space.
     *
     * @param renderFrame the active renderer
     * @param positionX   this node's final left edge
     * @param positionY   this node's final top edge
     * @param width       this node's final width
     * @param height      this node's final height
     */
    protected void arrangeChildren(AlumiteGuiRenderer renderFrame, int positionX, int positionY, int width, int height) {
        for (UiNode childNode : children) {
            if (childNode.visible) {
                childNode.layout(renderFrame, positionX, positionY, width, height);
            }
        }
    }

    /**
     * Compares the inputs of layout that live outside the tree, such as text from a supplier, with the values the last
     * measure saw. Called once per frame on every visible node before layout; return true to relayout.
     *
     * @param renderFrame the active renderer
     * @return true when this node's size or placement must be recomputed
     */
    protected boolean layoutInputsChanged(AlumiteGuiRenderer renderFrame) {
        return SELF_LAID_OUT.get(getClass());
    }

    final void validateLayout(AlumiteGuiRenderer renderFrame) {
        if (!visible) {
            return;
        }
        if (layoutInputsChanged(renderFrame)) {
            markLayoutDirty();
        }
        for (UiNode childNode : children) {
            childNode.validateLayout(renderFrame);
        }
    }

    /**
     * Space this node asks for before its box along an axis when a parent stacks it.
     *
     * @param axis          the axis being stacked
     * @param availableSize the available size along that axis
     * @return the margin in pixels
     */
    public int leadingMargin(Axis axis, int availableSize) {
        return 0;
    }

    /**
     * Space this node asks for after its box along an axis when a parent stacks it.
     *
     * @param axis          the axis being stacked
     * @param availableSize the available size along that axis
     * @return the margin in pixels
     */
    public int trailingMargin(Axis axis, int availableSize) {
        return 0;
    }

    /**
     * The unrounded size along an axis, so a stack can share rounded edges between neighbours.
     *
     * @param axis          the axis being stacked
     * @param availableSize the available size along that axis
     * @param measuredSize  the measured size along that axis
     * @return the exact size in pixels
     */
    public float exactSize(Axis axis, int availableSize, int measuredSize) {
        return measuredSize;
    }

    /**
     * Resolves this node's start and size along an axis inside a slot, honouring its alignment and margins.
     *
     * @param axis         the axis to resolve
     * @param slotStart    the slot's start coordinate
     * @param slotSize     the slot's size
     * @param measuredSize the measured size along that axis
     * @return the packed box, readable with {@link AxisConstraints#boxStart(long)} and {@link AxisConstraints#boxSize(long)}
     */
    public long resolveBox(Axis axis, int slotStart, int slotSize, int measuredSize) {
        return AxisConstraints.packBox(slotStart, measuredSize);
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
     * Whether this node and its subtree currently take part in pointer hit testing and focus navigation. Nodes that
     * are shown but must not react, such as a fully faded-out container, return false. Visibility is checked
     * separately.
     *
     * @return true when the pointer and keyboard navigation may reach this subtree
     */
    public boolean acceptsInput() {
        return true;
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
     * @param tooltipSupplier supplies the tooltip text; null removes the tooltip (use {@link #clearTooltip()} to avoid
     *                        overload ambiguity with a bare {@code null})
     * @return this node
     */
    public UiNode setTooltip(Supplier<String> tooltipSupplier) {
        this.tooltipSupplier = tooltipSupplier;
        return this;
    }

    /**
     * Sets a fixed tooltip text.
     *
     * @param tooltipText the tooltip text; null removes the tooltip (use {@link #clearTooltip()} to avoid overload
     *                    ambiguity with a bare {@code null})
     * @return this node
     */
    public UiNode setTooltip(String tooltipText) {
        this.tooltipSupplier = tooltipText == null ? null : () -> tooltipText;
        return this;
    }

    /**
     * Removes any tooltip from this node.
     *
     * @return this node
     */
    public UiNode clearTooltip() {
        this.tooltipSupplier = null;
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

    /**
     * Whether this node itself is a pointer hit target. Nodes that return false are transparent to the pointer, which
     * reaches their descendants or whatever lies beneath them.
     *
     * @return true when the pointer can land on this node
     */
    public boolean hitTarget() {
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

    public void onPointerEnter(float pointerX, float pointerY) {
        if (pointerEnterCallback != null) {
            pointerEnterCallback.run();
        }
    }

    public void onPointerLeave(float pointerX, float pointerY) {
        if (pointerLeaveCallback != null) {
            pointerLeaveCallback.run();
        }
    }

    public boolean onKeyPress(int keyCode, int modifiers) {
        return false;
    }

    /**
     * Called when a character is typed while this node or a descendant has focus.
     *
     * @param codePoint the typed Unicode code point
     * @return true when the character was consumed
     */
    public boolean onCharType(int codePoint) {
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

    /**
     * Copy any per-node state that must outlive recomposition (animations, drag state, modes) from the matching node
     * of the previous tree. Hover, press and focus flags are carried by the framework and need no handling here. The
     * default delegates to {@link #inheritAnimationState(UiNode)}.
     *
     * @param previous the matched node of the previous tree, always the same class as this node
     */
    protected void inheritState(UiNode previous) {
        inheritAnimationState(previous);
    }

    /**
     * Called on every node of a freshly composed tree, after it was reconciled with the previous tree and before
     * layout.
     *
     * @param context host data available to nodes
     */
    protected void onComposed(UiComposeContext context) {
    }

    /**
     * Called once when this node becomes part of a mounted tree.
     */
    protected void onMount() {
    }

    /**
     * Called once when this node leaves a mounted tree.
     */
    protected void onUnmount() {
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
        mounted = false;
        onUnmount();
    }

    /**
     * Recursively marks this node and all descendants as outside the viewport.
     */
    protected void notifySubtreeViewportLeave() {
        inViewport = false;
        for (UiNode childNode : children) {
            childNode.notifySubtreeViewportLeave();
        }
    }
}
