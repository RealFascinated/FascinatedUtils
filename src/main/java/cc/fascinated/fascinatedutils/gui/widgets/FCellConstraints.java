package cc.fascinated.fascinatedutils.gui.widgets;

import cc.fascinated.fascinatedutils.gui.core.Align;

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

    public float marginStart() {
        return marginStart;
    }

    public FCellConstraints setMarginStart(float marginStart) {
        this.marginStart = Math.max(0f, marginStart);
        return this;
    }

    public float marginEnd() {
        return marginEnd;
    }

    public FCellConstraints setMarginEnd(float marginEnd) {
        this.marginEnd = Math.max(0f, marginEnd);
        return this;
    }

    public float marginTop() {
        return marginTop;
    }

    public FCellConstraints setMarginTop(float marginTop) {
        this.marginTop = Math.max(0f, marginTop);
        return this;
    }

    public float marginBottom() {
        return marginBottom;
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

    public float growWeight() {
        return growWeight;
    }

    public FCellConstraints setGrowWeight(float growWeight) {
        this.growWeight = Math.max(0f, growWeight);
        return this;
    }

    public boolean expandHorizontal() {
        return expandHorizontal;
    }

    public FCellConstraints setExpandHorizontal(boolean expandHorizontal) {
        this.expandHorizontal = expandHorizontal;
        return this;
    }

    public boolean expandVertical() {
        return expandVertical;
    }

    public FCellConstraints setExpandVertical(boolean expandVertical) {
        this.expandVertical = expandVertical;
        return this;
    }

    public Align alignHorizontal() {
        return alignHorizontal;
    }

    public FCellConstraints setAlignHorizontal(Align alignHorizontal) {
        this.alignHorizontal = alignHorizontal;
        return this;
    }

    public Align alignVertical() {
        return alignVertical;
    }

    public FCellConstraints setAlignVertical(Align alignVertical) {
        this.alignVertical = alignVertical;
        return this;
    }

    public String widthGroupKey() {
        return widthGroupKey;
    }

    public FCellConstraints setWidthGroupKey(String widthGroupKey) {
        this.widthGroupKey = widthGroupKey;
        return this;
    }

    public float minWidth() {
        return minWidth;
    }

    public FCellConstraints setMinWidth(float minWidth) {
        this.minWidth = Math.max(0f, minWidth);
        return this;
    }

    public float minHeight() {
        return minHeight;
    }

    public FCellConstraints setMinHeight(float minHeight) {
        this.minHeight = Math.max(0f, minHeight);
        return this;
    }

    public float maxWidth() {
        return maxWidth;
    }

    public FCellConstraints setMaxWidth(float maxWidth) {
        this.maxWidth = Math.max(0f, maxWidth);
        return this;
    }

    public float maxHeight() {
        return maxHeight;
    }

    public FCellConstraints setMaxHeight(float maxHeight) {
        this.maxHeight = Math.max(0f, maxHeight);
        return this;
    }
}
