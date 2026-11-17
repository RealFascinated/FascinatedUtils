package cc.fascinated.fascinatedutils.gui.layout;

/**
 * Start, end and size constraints along one axis. Every setter fully defines the edge it sets, so
 * {@code center().left(10)} anchors to the left edge rather than keeping the centre anchor. A start and an end
 * constraint together fix the size, so a size set while both exist is ignored and a start or end set while a size
 * exists replaces that size.
 *
 * <p>Relative values resolve against the available size handed to the node, which is the parent's resolved size, or
 * the parent's own available size when the parent is auto-sized. Edges are rounded rather than sizes, so neighbouring
 * relative boxes tile without one-pixel gaps.
 */
public class AxisConstraints {
    /**
     * Shared empty constraints for nodes that never set a property. Never mutate it.
     */
    public static final AxisConstraints NONE = new AxisConstraints();

    private boolean hasStart;
    private boolean hasEnd;
    private boolean hasSize;
    private boolean startRelative;
    private boolean endRelative;
    private boolean sizeRelative;
    private float startValue;
    private float endValue;
    private float sizeValue;
    private int startOffset;
    private int endOffset;
    private float startAnchor;
    private float endAnchor;

    /**
     * Packs a resolved start and size into one value.
     *
     * @param start the start edge in pixels
     * @param size  the size in pixels
     * @return the packed box, readable with {@link #boxStart(long)} and {@link #boxSize(long)}
     */
    public static long packBox(int start, int size) {
        return ((long) start << 32) | (size & 0xFFFFFFFFL);
    }

    /**
     * Reads the start edge of a packed box.
     *
     * @param box a value from {@link #packBox(int, int)} or {@link #resolveBox(int, int, int)}
     * @return the start edge in pixels
     */
    public static int boxStart(long box) {
        return (int) (box >> 32);
    }

    /**
     * Reads the size of a packed box.
     *
     * @param box a value from {@link #packBox(int, int)} or {@link #resolveBox(int, int, int)}
     * @return the size in pixels
     */
    public static int boxSize(long box) {
        return (int) box;
    }

    public AxisConstraints startPx(int value) {
        return setStart(value, false, 0, 0f);
    }

    public AxisConstraints startRel(float value) {
        return setStart(value, true, 0, 0f);
    }

    public AxisConstraints startRel(float value, int offset, float anchor) {
        return setStart(value, true, offset, anchor);
    }

    public AxisConstraints endPx(int value) {
        return setEnd(value, false, 0, 0f);
    }

    public AxisConstraints endRel(float value) {
        return setEnd(value, true, 0, 0f);
    }

    public AxisConstraints endRel(float value, int offset, float anchor) {
        return setEnd(value, true, offset, anchor);
    }

    public AxisConstraints sizePx(int value) {
        return setSize(value, false);
    }

    public AxisConstraints sizeRel(float value) {
        return setSize(value, true);
    }

    /**
     * Whether this axis fixes its own size, either by an explicit size or by both start and end constraints.
     *
     * @return true when the size does not depend on the node's content
     */
    public boolean hasDefinedSize() {
        return hasSize || (hasStart && hasEnd);
    }

    /**
     * Resolves the size along this axis, falling back to the content size when the axis does not define one.
     *
     * @param availableSize the available size along this axis in pixels
     * @param contentSize   the content size used when {@link #hasDefinedSize()} is false
     * @return the size in pixels, never negative
     */
    public int resolveSize(int availableSize, int contentSize) {
        return Math.max(0, Math.round(sizeFloat(availableSize, contentSize)));
    }

    /**
     * Resolves the unrounded size along this axis, so stacks can place neighbours on shared rounded edges.
     *
     * @param availableSize the available size along this axis in pixels
     * @param contentSize   the content size used when {@link #hasDefinedSize()} is false
     * @return the size in pixels as a float, never negative
     */
    public float resolveExactSize(int availableSize, int contentSize) {
        return sizeFloat(availableSize, contentSize);
    }

    /**
     * Resolves start and size within a slot. Both edges are rounded and the size is derived from them, so adjacent
     * relative boxes meet exactly.
     *
     * @param slotStart   the slot's start coordinate in pixels
     * @param slotSize    the slot's size in pixels
     * @param contentSize the content size used when {@link #hasDefinedSize()} is false
     * @return the packed box, readable with {@link #boxStart(long)} and {@link #boxSize(long)}
     */
    public long resolveBox(int slotStart, int slotSize, int contentSize) {
        float size = sizeFloat(slotSize, contentSize);
        float start;
        if (hasStart) {
            start = slotStart + startPixels(slotSize) - startAnchor * size;
        }
        else if (hasEnd) {
            start = slotStart + slotSize - (endPixels(slotSize) + size - endAnchor * size);
        }
        else {
            start = slotStart;
        }
        int startEdge = Math.round(start);
        int endEdge = Math.round(start + size);
        return packBox(startEdge, Math.max(0, endEdge - startEdge));
    }

    /**
     * The space this axis asks for before the box when the node is stacked. Only a start constraint that is not
     * anchored counts as a margin.
     *
     * @param availableSize the available size along this axis in pixels
     * @return the leading margin in pixels, never negative
     */
    public int leadingMargin(int availableSize) {
        if (!hasStart || startAnchor != 0f) {
            return 0;
        }
        return Math.max(0, Math.round(startPixels(availableSize)));
    }

    /**
     * The space this axis asks for after the box when the node is stacked. Only an end constraint that is not
     * anchored counts as a margin.
     *
     * @param availableSize the available size along this axis in pixels
     * @return the trailing margin in pixels, never negative
     */
    public int trailingMargin(int availableSize) {
        if (!hasEnd || endAnchor != 0f) {
            return 0;
        }
        return Math.max(0, Math.round(endPixels(availableSize)));
    }

    private AxisConstraints setStart(float value, boolean relative, int offset, float anchor) {
        hasStart = true;
        startValue = value;
        startRelative = relative;
        startOffset = offset;
        startAnchor = anchor;
        if (hasEnd) {
            hasSize = false;
        }
        return this;
    }

    private AxisConstraints setEnd(float value, boolean relative, int offset, float anchor) {
        hasEnd = true;
        endValue = value;
        endRelative = relative;
        endOffset = offset;
        endAnchor = anchor;
        if (hasStart) {
            hasSize = false;
        }
        return this;
    }

    private AxisConstraints setSize(float value, boolean relative) {
        if (hasStart && hasEnd) {
            return this;
        }
        hasSize = true;
        sizeValue = value;
        sizeRelative = relative;
        return this;
    }

    private float startPixels(int availableSize) {
        return (startRelative ? startValue * availableSize : startValue) + startOffset;
    }

    private float endPixels(int availableSize) {
        return (endRelative ? endValue * availableSize : endValue) + endOffset;
    }

    private float sizeFloat(int availableSize, int contentSize) {
        if (hasStart && hasEnd) {
            return Math.max(0f, availableSize - startPixels(availableSize) - endPixels(availableSize));
        }
        if (hasSize) {
            return Math.max(0f, sizeRelative ? sizeValue * availableSize : sizeValue);
        }
        return Math.max(0, contentSize);
    }
}
