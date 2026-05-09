package cc.fascinated.fascinatedutils.oldgui.core;

import cc.fascinated.fascinatedutils.oldgui.widgets.FWidget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Single implementation of front-to-back hit testing shared by {@link UiFrameContext} and
 * pointer dispatch.
 */
public final class PointerHitTraversal {
    private PointerHitTraversal() {
    }

    /**
     * Deepest widget under the pointer that blocks or targets, or {@code null}.
     */
    public static FWidget hitLeaf(FWidget node, float pointerX, float pointerY) {
        List<FWidget> path = pathRootToLeaf(node, pointerX, pointerY);
        if (path.isEmpty()) {
            return null;
        }
        return path.get(path.size() - 1);
    }

    /**
     * Ordered chain from root to deepest hit widget; empty if nothing blocks at this point.
     */
    public static List<FWidget> pathRootToLeaf(FWidget node, float pointerX, float pointerY) {
        if (node == null || !node.visible()) {
            return List.of();
        }
        if (node.clipChildren() && !node.containsPoint(pointerX, pointerY)) {
            return List.of();
        }
        float childPointerY = pointerY + node.childPointerYOffset();
        List<FWidget> kids = node.childrenView();
        for (int childIndex = kids.size() - 1; childIndex >= 0; childIndex--) {
            List<FWidget> childPath = pathRootToLeaf(kids.get(childIndex), pointerX, childPointerY);
            if (!childPath.isEmpty()) {
                return prepend(node, childPath);
            }
        }
        PointerHitKind kind = node.pointerHitKind();
        if (kind != PointerHitKind.NONE && node.containsPoint(pointerX, pointerY)) {
            return List.of(node);
        }
        return List.of();
    }

    private static List<FWidget> prepend(FWidget head, List<FWidget> tail) {
        ArrayList<FWidget> out = new ArrayList<>(tail.size() + 1);
        out.add(head);
        out.addAll(tail);
        return Collections.unmodifiableList(out);
    }
}
