package cc.fascinated.fascinatedutils.gui.node.layout;

import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.gui.layout.MeasuredSize;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;

/**
 * Lays visible children out in equal cells. With a cell aspect ratio the grid is as tall as its rows, so it can sit in
 * a {@link ScrollColumnNode}; otherwise the rows share the available height.
 */
public class GridNode extends UiNode {
    private int gap;
    private int minCellWidth = -1;
    private float cellAspectRatio = -1f;
    private int rowFooterHeight = 0;

    public GridNode setGap(int gap) {
        this.gap = Math.max(0, gap);
        markLayoutDirty();
        return this;
    }

    /**
     * Sets the minimum cell width used to compute the column count dynamically at layout
     * time. The grid will fit as many columns as possible without any cell going below this
     * width once gaps are counted, clamped to the actual number of visible children.
     *
     * @param minCellWidth minimum cell width in pixels
     */
    public GridNode setMinCellWidth(int minCellWidth) {
        this.minCellWidth = minCellWidth;
        markLayoutDirty();
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
        markLayoutDirty();
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
        markLayoutDirty();
        return this;
    }

    @Override
    protected MeasuredSize doMeasure(AlumiteGuiRenderer renderFrame, int maxWidth, int maxHeight) {
        return new MeasuredSize(maxWidth, gridHeight(maxWidth, maxHeight));
    }

    @Override
    protected void arrangeChildren(AlumiteGuiRenderer renderFrame, int positionX, int positionY, int width, int height) {
        int visibleCount = visibleChildCount();
        if (visibleCount == 0) {
            return;
        }
        int columns = columnCount(width, visibleCount);
        int rows = (visibleCount + columns - 1) / columns;
        float cellWidth = cellWidth(width, columns);
        float cellHeight = cellHeight(width, height, columns, rows);
        int index = 0;
        for (UiNode childNode : childrenView()) {
            if (!childNode.visible()) {
                continue;
            }
            float cellX = positionX + (index % columns) * (cellWidth + gap);
            float cellY = positionY + (index / columns) * (cellHeight + gap);
            int childX = Math.round(cellX);
            int childY = Math.round(cellY);
            childNode.layout(renderFrame, childX, childY, Math.round(cellX + cellWidth) - childX, Math.round(cellY + cellHeight) - childY);
            index++;
        }
    }

    private int visibleChildCount() {
        int visibleCount = 0;
        for (UiNode childNode : childrenView()) {
            if (childNode.visible()) {
                visibleCount++;
            }
        }
        return visibleCount;
    }

    private int columnCount(int width, int visibleCount) {
        int columns = minCellWidth > 0 ? Math.max(1, (width + gap) / (minCellWidth + gap)) : 1;
        return Math.min(columns, visibleCount);
    }

    private float cellWidth(int width, int columns) {
        return Math.max(0f, (width - gap * (columns - 1)) / (float) columns);
    }

    private float cellHeight(int width, int height, int columns, int rows) {
        if (cellAspectRatio > 0) {
            return cellWidth(width, columns) / cellAspectRatio + rowFooterHeight;
        }
        return Math.max(0f, (height - gap * (rows - 1)) / (float) rows);
    }

    private int gridHeight(int width, int height) {
        int visibleCount = visibleChildCount();
        if (visibleCount == 0) {
            return 0;
        }
        if (cellAspectRatio <= 0) {
            return height;
        }
        int columns = columnCount(width, visibleCount);
        int rows = (visibleCount + columns - 1) / columns;
        return Math.round(rows * cellHeight(width, height, columns, rows) + gap * (rows - 1));
    }
}
