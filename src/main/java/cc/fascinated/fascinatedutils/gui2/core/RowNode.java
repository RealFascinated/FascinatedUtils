package cc.fascinated.fascinatedutils.gui2.core;

import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;

/**
 * Horizontal container that divides available width across visible children.
 */
public class RowNode extends UiNode {
    private int gap;

    public RowNode setGap(int gap) {
        this.gap = Math.max(0, gap);
        return this;
    }

    @Override
    public void layout(RenderFrame renderFrame, int positionX, int positionY, int width, int height) {
        bounds().set(positionX, positionY, width, height);

        int visibleCount = 0;
        for (UiNode childNode : childrenView()) {
            if (childNode.visible()) {
                visibleCount++;
            }
        }
        if (visibleCount == 0) {
            return;
        }

        int totalGap = gap * Math.max(0, visibleCount - 1);
        int childWidth = Math.max(0, (width - totalGap) / visibleCount);
        int cursorX = positionX;
        for (UiNode childNode : childrenView()) {
            if (!childNode.visible()) {
                continue;
            }
            childNode.layout(renderFrame, cursorX, positionY, childWidth, height);
            cursorX += childWidth + gap;
        }
    }
}
