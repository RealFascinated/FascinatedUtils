package cc.fascinated.fascinatedutils.gui2.layout;

/**
 * Constraint set for one axis with start, end, and size properties.
 *
 * <p>At most two properties should be set at once. When all three are set, size is dropped.
 */
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

    public int resolveSize(int parentSize) {
        return resolveSize(parentSize, "axis", "unknown-node");
    }

    public int resolveSize(int parentSize, String axisLabel, String nodePath) {
        dropSizeWhenOverConstrained();
        if (size != null) {
            return Math.max(0, Math.round(size.resolve(parentSize)));
        }
        if (start != null && end != null) {
            float startValue = start.resolve(parentSize) + startOffset;
            float endValue = end.resolve(parentSize) + endOffset;
            return Math.max(0, Math.round(parentSize - startValue - endValue));
        }
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("Axis size is undefined for ");
        messageBuilder.append(axisLabel);
        messageBuilder.append(" on node ");
        messageBuilder.append(nodePath);
        messageBuilder.append(". Set sizePx/sizeRel or set both start and end constraints.");
        throw new IllegalStateException(messageBuilder.toString());
    }

    private void dropSizeWhenOverConstrained() {
        if (start != null && end != null && size != null) {
            size = null;
        }
    }
}
