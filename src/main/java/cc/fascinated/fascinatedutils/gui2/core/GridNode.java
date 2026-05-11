package cc.fascinated.fascinatedutils.gui2.core;

import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;

public class GridNode extends UiNode {
    private int cols = 1;
    private int gap;
    private int rowHeight = -1;

    public GridNode setCols(int cols) {
        this.cols = Math.max(1, cols);
        return this;
    }

    public GridNode setGap(int gap) {
        this.gap = Math.max(0, gap);
        return this;
    }

    /**
     * Sets a fixed pixel height for every row. When set, the grid reports its own total
     * content height ({@code rows * rowHeight + gaps}) as its bounds height, allowing it
     * to be measured and scrolled correctly inside a {@link ScrollColumnNode}.
     *
     * <p>When not set, the grid divides the parent-provided height equally across rows
     * (fill behaviour).
     *
     * @param rowHeight height in pixels for each row
     */
    public GridNode setRowHeight(int rowHeight) {
        this.rowHeight = Math.max(0, rowHeight);
        return this;
    }

    @Override
    public void layout(RenderFrame renderFrame, int positionX, int positionY, int width, int height) {
        int visibleCount = 0;
        for (UiNode childNode : childrenView()) {
            if (childNode.visible()) {
                visibleCount++;
            }
        }
        if (visibleCount == 0) {
            bounds().set(positionX, positionY, width, 0);
            return;
        }

        int effectiveCols = Math.min(cols, visibleCount);
        int rows = (int) Math.ceil((double) visibleCount / effectiveCols);
        int cellWidth = Math.max(0, (width - gap * (effectiveCols - 1)) / effectiveCols);

        int cellHeight;
        int totalHeight;
        if (rowHeight >= 0) {
            cellHeight = rowHeight;
            totalHeight = rows * cellHeight + gap * Math.max(0, rows - 1);
        } else {
            cellHeight = Math.max(0, (height - gap * (rows - 1)) / rows);
            totalHeight = height;
        }

        bounds().set(positionX, positionY, width, totalHeight);

        int col = 0;
        int row = 0;
        for (UiNode childNode : childrenView()) {
            if (!childNode.visible()) {
                continue;
            }
            int childX = positionX + col * (cellWidth + gap);
            int childY = positionY + row * (cellHeight + gap);
            childNode.layout(renderFrame, childX, childY, cellWidth, cellHeight);
            col++;
            if (col >= effectiveCols) {
                col = 0;
                row++;
            }
        }
    }
}
