package cc.fascinated.fascinatedutils.gui.widgets;

import cc.fascinated.fascinatedutils.gui.core.Align;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public class FCellConstraints {
    public static final FCellConstraints DEFAULT = new FCellConstraints();
    private float marginStart;
    private float marginEnd;
    private float marginTop;
    private float marginBottom;
    private float growWeight = 1f;
    private boolean expandHorizontal;
    private boolean expandVertical;
    private Align alignHorizontal;
    private Align alignVertical;
    private String widthGroupKey;
    private float minWidth;
    private float minHeight;
    private float maxWidth = Float.POSITIVE_INFINITY;
    private float maxHeight = Float.POSITIVE_INFINITY;

    /**
     * Returns a detached copy suitable for attaching to widgets without mutating a shared DEFAULT instance.
     *
     * @param source constraint values to duplicate; ignored when {@code null}
     * @return a new mutable copy, or a new default instance when {@code source} is {@code null}
     */
    public static FCellConstraints copyNullable(FCellConstraints source) {
        if (source == null) {
            return new FCellConstraints();
        }
        FCellConstraints duplicate = new FCellConstraints();
        duplicate.marginStart = source.marginStart;
        duplicate.marginEnd = source.marginEnd;
        duplicate.marginTop = source.marginTop;
        duplicate.marginBottom = source.marginBottom;
        duplicate.growWeight = source.growWeight;
        duplicate.expandHorizontal = source.expandHorizontal;
        duplicate.expandVertical = source.expandVertical;
        duplicate.alignHorizontal = source.alignHorizontal;
        duplicate.alignVertical = source.alignVertical;
        duplicate.widthGroupKey = source.widthGroupKey;
        duplicate.minWidth = source.minWidth;
        duplicate.minHeight = source.minHeight;
        duplicate.maxWidth = source.maxWidth;
        duplicate.maxHeight = source.maxHeight;
        return duplicate;
    }

    public FCellConstraints setMarginStart(float marginStart) {
        this.marginStart = Math.max(0f, marginStart);
        return this;
    }

    public FCellConstraints setMarginEnd(float marginEnd) {
        this.marginEnd = Math.max(0f, marginEnd);
        return this;
    }

    public FCellConstraints setMarginTop(float marginTop) {
        this.marginTop = Math.max(0f, marginTop);
        return this;
    }

    public FCellConstraints setMarginBottom(float marginBottom) {
        this.marginBottom = Math.max(0f, marginBottom);
        return this;
    }

    public FCellConstraints setMargins(float horizontal, float vertical) {
        float safeHorizontal = Math.max(0f, horizontal);
        float safeVertical = Math.max(0f, vertical);
        this.marginStart = safeHorizontal;
        this.marginEnd = safeHorizontal;
        this.marginTop = safeVertical;
        this.marginBottom = safeVertical;
        return this;
    }

    public FCellConstraints setGrowWeight(float growWeight) {
        this.growWeight = Math.max(0f, growWeight);
        return this;
    }

    public FCellConstraints setExpandHorizontal(boolean expandHorizontal) {
        this.expandHorizontal = expandHorizontal;
        return this;
    }

    public FCellConstraints setExpandVertical(boolean expandVertical) {
        this.expandVertical = expandVertical;
        return this;
    }

    public FCellConstraints setAlignHorizontal(Align alignHorizontal) {
        this.alignHorizontal = alignHorizontal;
        return this;
    }

    public FCellConstraints setAlignVertical(Align alignVertical) {
        this.alignVertical = alignVertical;
        return this;
    }

    public FCellConstraints setWidthGroupKey(String widthGroupKey) {
        this.widthGroupKey = widthGroupKey;
        return this;
    }

    public FCellConstraints setMinWidth(float minWidth) {
        this.minWidth = Math.max(0f, minWidth);
        return this;
    }

    public FCellConstraints setMinHeight(float minHeight) {
        this.minHeight = Math.max(0f, minHeight);
        return this;
    }

    public FCellConstraints setMaxWidth(float maxWidth) {
        this.maxWidth = Math.max(0f, maxWidth);
        return this;
    }

    public FCellConstraints setMaxHeight(float maxHeight) {
        this.maxHeight = Math.max(0f, maxHeight);
        return this;
    }
}
