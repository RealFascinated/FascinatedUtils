package cc.fascinated.fascinatedutils.gui.layout;

public class AxisConstraints {
    private LayoutValue start;
    private LayoutValue end;
    private LayoutValue size;
    private int startOffset;
    private int endOffset;
    private float startAnchor;
    private float endAnchor;

    public AxisConstraints startPx(int value) {
        this.start = LayoutValue.pixels(value);
        return this;
    }

    public AxisConstraints startRel(float value) {
        this.start = LayoutValue.relative(value);
        return this;
    }

    public AxisConstraints startRel(float value, int offset, float anchor) {
        this.start = LayoutValue.relative(value);
        this.startOffset = offset;
        this.startAnchor = anchor;
        return this;
    }

    public AxisConstraints endPx(int value) {
        this.end = LayoutValue.pixels(value);
        return this;
    }

    public AxisConstraints endRel(float value) {
        this.end = LayoutValue.relative(value);
        return this;
    }

    public AxisConstraints endRel(float value, int offset, float anchor) {
        this.end = LayoutValue.relative(value);
        this.endOffset = offset;
        this.endAnchor = anchor;
        return this;
    }

    public AxisConstraints sizePx(int value) {
        this.size = LayoutValue.pixels(value);
        return this;
    }

    public AxisConstraints sizeRel(float value) {
        this.size = LayoutValue.relative(value);
        return this;
    }

    public boolean hasStartConstraint() {
        return start != null;
    }

    public boolean hasEndConstraint() {
        return end != null;
    }

    public boolean hasSizeConstraint() {
        return size != null;
    }

    public int resolvePosition(int parentPosition, int parentSize, int resolvedSize) {
        dropSizeWhenOverConstrained();
        if (start != null) {
            float startValue = start.resolve(parentSize) + startOffset;
            return parentPosition + Math.round(startValue - startAnchor * resolvedSize);
        }
        if (end != null) {
            float endValue = end.resolve(parentSize) + endOffset;
            return parentPosition + parentSize - Math.round(endValue + resolvedSize - endAnchor * resolvedSize);
        }
        return parentPosition;
    }

    /**
     * Whether this axis fixes its own size, either by an explicit size or by both start and end constraints. Only then
     * may {@link #resolveSize(int)} be called.
     *
     * @return true when {@link #resolveSize(int)} can resolve the size
     */
    public boolean hasDefinedSize() {
        return hasSizeConstraint() || (hasStartConstraint() && hasEndConstraint());
    }

    /**
     * Resolves the size of an axis that has a defined size. Check {@link #hasDefinedSize()} first.
     *
     * @param parentSize the parent's size along this axis in pixels
     * @return the resolved size in pixels, never negative
     */
    public int resolveSize(int parentSize) {
        dropSizeWhenOverConstrained();
        if (size != null) {
            return Math.max(0, Math.round(size.resolve(parentSize)));
        }
        float startValue = start.resolve(parentSize) + startOffset;
        float endValue = end.resolve(parentSize) + endOffset;
        return Math.max(0, Math.round(parentSize - startValue - endValue));
    }

    private void dropSizeWhenOverConstrained() {
        if (start != null && end != null && size != null) {
            size = null;
        }
    }
}
