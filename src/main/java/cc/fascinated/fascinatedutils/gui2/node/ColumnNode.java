package cc.fascinated.fascinatedutils.gui2.node;

import cc.fascinated.fascinatedutils.gui2.core.UiNode;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;

public class ColumnNode extends UiNode {
    private int gap;

    public ColumnNode setGap(int gap) {
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
        int childHeight = Math.max(0, (height - totalGap) / visibleCount);
        int cursorY = positionY;
        for (UiNode childNode : childrenView()) {
            if (!childNode.visible()) {
                continue;
            }
            childNode.layout(renderFrame, positionX, cursorY, width, childHeight);
            cursorY += childHeight + gap;
        }
    }
}
