package cc.fascinated.fascinatedutils.gui2.core;

import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;

public class GridNode extends UiNode {
    private int cols = 1;
    private int gap;
    private int rowHeight = -1;
    private int minCellWidth = -1;
    private float cellAspectRatio = -1f;
    private int rowFooterHeight = 0;

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

    /**
     * Sets the minimum cell width used to compute the column count dynamically at layout
     * time. The grid will fit as many columns as possible without any cell going below this
     * width, clamped to the actual number of visible children.
     *
     * @param minCellWidth minimum cell width in pixels
     */
    public GridNode setMinCellWidth(int minCellWidth) {
        this.minCellWidth = minCellWidth;
        return this;
    }

    /**
     * Configures a dynamic row height derived from the computed cell width and a fixed
     * aspect ratio. Use together with {@link #setRowFooterHeight} when cells have a
     * content area (e.g. a thumbnail) plus a fixed footer.
     *
     * @param aspectRatio width-to-height ratio for the cell content area (e.g. {@code 16f / 9f})
     */
    public GridNode setCellAspectRatio(float aspectRatio) {
        this.cellAspectRatio = aspectRatio;
        return this;
    }

    /**
     * Extra fixed pixels added on top of the aspect-ratio-derived height when
     * {@link #setCellAspectRatio} is used.
     *
     * @param rowFooterHeight footer height in pixels
     */
    public GridNode setRowFooterHeight(int rowFooterHeight) {
        this.rowFooterHeight = rowFooterHeight;
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

        if (minCellWidth > 0) {
            cols = Math.max(1, width / minCellWidth);
        }

        int effectiveCols = Math.min(cols, visibleCount);
        int rows = (int) Math.ceil((double) visibleCount / effectiveCols);
        int cellWidth = Math.max(0, (width - gap * (effectiveCols - 1)) / effectiveCols);

        int resolvedRowHeight = cellAspectRatio > 0
                ? Math.round(cellWidth / cellAspectRatio) + rowFooterHeight
                : rowHeight;

        int cellHeight;
        int totalHeight;
        if (resolvedRowHeight >= 0) {
            cellHeight = resolvedRowHeight;
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
