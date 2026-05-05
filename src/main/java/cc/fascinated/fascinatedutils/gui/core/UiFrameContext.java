package cc.fascinated.fascinatedutils.gui.core;

import cc.fascinated.fascinatedutils.gui.widgets.FWidget;

import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Collections;
import java.util.List;

/**
 * Immutable snapshot of pointer position, hit chain, and focus for one UI frame.
 *
 * <p>Hit path order is root-first, leaf-last. Paint and input share the same instance for a
 * frame after layout.</p>
 */
@Getter
@Accessors(fluent = true)
public final class UiFrameContext {
    private static final UiFrameContext EMPTY = new UiFrameContext(0f, 0f, List.of(), UiFocusIds.NO_FOCUS_ID);

    private final float pointerX;
    private final float pointerY;
    private final List<FWidget> hitPathRootToLeaf;
    private final int focusedId;

    private UiFrameContext(float pointerX, float pointerY, List<FWidget> hitPathRootToLeaf, int focusedId) {
        this.pointerX = pointerX;
        this.pointerY = pointerY;
        this.hitPathRootToLeaf = hitPathRootToLeaf;
        this.focusedId = focusedId;
    }

    /**
     * Builds a context by walking the widget tree in paint order (front-most child first).
     *
     * @param root      layout root
     * @param pointerX  logical pointer X
     * @param pointerY  logical pointer Y
     * @param focusedId current focus id
     * @return immutable context; empty hit path when pointer misses all blocking widgets
     */
    public static UiFrameContext hitTest(FWidget root, float pointerX, float pointerY, int focusedId) {
        if (root == null) {
            return EMPTY;
        }
        List<FWidget> path = PointerHitTraversal.pathRootToLeaf(root, pointerX, pointerY);
        return new UiFrameContext(pointerX, pointerY, path, focusedId);
    }

    public static UiFrameContext empty(float pointerX, float pointerY, int focusedId) {
        return new UiFrameContext(pointerX, pointerY, List.of(), focusedId);
    }

    public static List<FWidget> unmodifiableHitPath(List<FWidget> path) {
        return Collections.unmodifiableList(path);
    }

    public FWidget hitTarget() {
        if (hitPathRootToLeaf.isEmpty()) {
            return null;
        }
        return hitPathRootToLeaf.get(hitPathRootToLeaf.size() - 1);
    }

    /**
     * Whether the pointer hit chain includes this widget (it is an ancestor of the hit target,
     * or the target itself).
     */
    public boolean isOnHitPath(FWidget widget) {
        if (widget == null) {
            return false;
        }
        return hitPathRootToLeaf.contains(widget);
    }

    /**
     * Whether this widget is the deepest hit node.
     */
    public boolean isHitTarget(FWidget widget) {
        return widget != null && widget == hitTarget();
    }

    /**
     * Pointer lies inside widget bounds (axis-aligned), regardless of occlusion.
     */
    public boolean pointerInBounds(FWidget widget) {
        return widget != null && widget.containsPoint(pointerX, pointerY);
    }

    /**
     * Whether the hit leaf lies inside this widget's subtree (row/section hover).
     */
    public boolean isHitWithinSubtree(FWidget widget) {
        if (widget == null) {
            return false;
        }
        FWidget leaf = hitTarget();
        for (FWidget walk = leaf; walk != null; walk = walk.parent()) {
            if (walk == widget) {
                return true;
            }
        }
        return false;
    }

    public UiFrameContext withFocus(int newFocusedId) {
        return new UiFrameContext(pointerX, pointerY, hitPathRootToLeaf, newFocusedId);
    }
}
