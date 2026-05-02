package cc.fascinated.fascinatedutils.gui.widgets;

import cc.fascinated.fascinatedutils.gui.core.ScrollChrome;
import cc.fascinated.fascinatedutils.gui.core.UiPointerCursor;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import lombok.Setter;

import java.util.List;
import java.util.function.Consumer;

public class FScrollColumnWidget extends FWidget implements FScrollable {
    private static final float SMOOTH_SCROLL_LERP_FACTOR = 0.35f;
    private static final float SCROLLBAR_HOVER_EXPAND_PX = 2f;
    private static final float SCROLLBAR_HIT_SLOP_PX = 6f;
    private static final float SCROLLBAR_FADE_IDLE_ALPHA = 0.45f;
    private static final float SCROLLBAR_FADE_HOVER_ALPHA = 0.75f;
    private static final float SCROLLBAR_FADE_DRAG_ALPHA = 1.00f;
    private final FWidget body;
    private final float rowGap;
    private float scrollOffsetY;
    private float targetScrollOffsetY;
    private float contentHeight;
    @Setter
    private boolean fillVerticalInColumn;
    private boolean fixedViewportHeightEnabled;
    private float fixedViewportHeightLogicalPixels;
    private Consumer<Float> scrollOffsetChangeListener = offset -> {};
    private boolean thumbHovered = false;
    private boolean thumbDragging = false;
    private float dragStartMouseY;
    private float dragStartScrollOffset;
    private float cachedThumbX;
    private float cachedThumbY;
    private float cachedThumbW;
    private float cachedThumbH;

    public FScrollColumnWidget(FWidget bodyColumn, float rowGap) {
        this.body = bodyColumn;
        this.rowGap = rowGap;
        addChild(bodyColumn);
    }

    public float scrollClipRowGap() {
        return rowGap;
    }

    /**
     * The scroll body's root widget (normally an {@link FColumnWidget}).
     *
     * @return body column/widget passed at construction time
     */
    public FWidget scrollBodyRoot() {
        return body;
    }

    private static int applyAlphaFactor(int argb, float factor) {
        int a = Math.round(((argb >>> 24) & 0xFF) * factor);
        return (argb & 0x00FFFFFF) | (a << 24);
    }

    public void setFixedViewportHeight(float viewportHeightLogicalPixels) {
        if (viewportHeightLogicalPixels <= 0f) {
            fixedViewportHeightEnabled = false;
            fixedViewportHeightLogicalPixels = 0f;
            return;
        }
        fixedViewportHeightEnabled = true;
        this.fixedViewportHeightLogicalPixels = viewportHeightLogicalPixels;
    }

    @Override
    public boolean fillsVerticalInColumn() {
        return fillVerticalInColumn && !fixedViewportHeightEnabled;
    }

    public float scrollOffsetY() {
        return scrollOffsetY;
    }

    public void setScrollOffsetY(float scrollOffsetY) {
        this.scrollOffsetY = scrollOffsetY;
        this.targetScrollOffsetY = scrollOffsetY;
    }

    public void setScrollOffsetChangeListener(Consumer<Float> scrollOffsetChangeListener) {
        this.scrollOffsetChangeListener = scrollOffsetChangeListener == null ? offset -> {} : scrollOffsetChangeListener;
    }

    public float contentHeight() {
        return contentHeight;
    }

    @Override
    public boolean clipChildren() {
        return true;
    }

    @Override
    public boolean wantsPointer() {
        return true;
    }

    @Override
    public UiPointerCursor pointerCursor(float pointerX, float pointerY) {
        if (thumbDragging || isOverThumbHitArea(pointerX, pointerY)) {
            return UiPointerCursor.HAND;
        }
        return UiPointerCursor.DEFAULT;
    }

    @Override
    public boolean applyScroll(float delta) {
        float scaledDelta = delta * FTheme.scrollWheelScale() * 1f;
        float next = FColumnWidget.clampScrollOffset(contentHeight, h(), targetScrollOffsetY - scaledDelta);
        if (Math.abs(next - targetScrollOffsetY) < 1e-4f) {
            return false;
        }
        targetScrollOffsetY = next;
        scrollOffsetChangeListener.accept(targetScrollOffsetY);
        return true;
    }

    @Override
    public boolean mouseDownCapture(float pointerX, float pointerY, int button) {
        if (button == 0 && isScrollbarVisible() && isOverThumbHitArea(pointerX, pointerY)) {
            thumbDragging = true;
            dragStartMouseY = pointerY;
            dragStartScrollOffset = targetScrollOffsetY;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseUpCapture(float pointerX, float pointerY, int button) {
        if (button == 0 && thumbDragging) {
            thumbDragging = false;
            thumbHovered = isOverThumbHitArea(pointerX, pointerY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseMove(float pointerX, float pointerY) {
        if (thumbDragging) {
            float mouseDelta = pointerY - dragStartMouseY;
            float viewHeight = h();
            float scrollRange = contentHeight - viewHeight;
            if (scrollRange > 0f && viewHeight > 0f) {
                float[] thumb = ScrollChrome.verticalThumbRect(x(), y(), w(), h(), contentHeight, dragStartScrollOffset);
                float thumbH = thumb[1];
                float trackH = viewHeight - thumbH;
                float scrollDelta = trackH > 0f ? (mouseDelta / trackH) * scrollRange : 0f;
                float next = FColumnWidget.clampScrollOffset(contentHeight, viewHeight, dragStartScrollOffset + scrollDelta);
                targetScrollOffsetY = next;
                scrollOffsetChangeListener.accept(targetScrollOffsetY);
            }
            return true;
        }
        boolean wasHovered = thumbHovered;
        thumbHovered = isScrollbarVisible() && isOverThumbHitArea(pointerX, pointerY);
        return thumbHovered != wasHovered;
    }

    @Override
    public boolean mouseLeave(float pointerX, float pointerY) {
        super.mouseLeave(pointerX, pointerY);
        if (!thumbDragging) {
            thumbHovered = false;
        }
        return false;
    }

    @Override
    public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
        if (fixedViewportHeightEnabled) {
            return fixedViewportHeightLogicalPixels;
        }
        float measured = measureContentHeight(measure, widthBudget);
        return ScrollChrome.resolveScrollableContentHeight(Float.NaN, measured);
    }

    @Override
    public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
        setBounds(layoutX, layoutY, layoutWidth, layoutHeight);
        float innerWidth = layoutWidth;
        float measured = measureContentHeight(measure, innerWidth);
        contentHeight = ScrollChrome.resolveScrollableContentHeight(Float.NaN, measured);
        float maxScroll = FColumnWidget.clampScrollOffset(contentHeight, layoutHeight, targetScrollOffsetY);
        targetScrollOffsetY = maxScroll;
        float interpolated = scrollOffsetY + (targetScrollOffsetY - scrollOffsetY) * SMOOTH_SCROLL_LERP_FACTOR;
        float scrollBeforeClamp = scrollOffsetY;
        scrollOffsetY = FColumnWidget.clampScrollOffset(contentHeight, layoutHeight, interpolated);
        if (Math.abs(targetScrollOffsetY - scrollOffsetY) < 0.01f) {
            scrollOffsetY = targetScrollOffsetY;
        }
        if (Math.abs(scrollBeforeClamp - scrollOffsetY) > 1e-3f) {
            scrollOffsetChangeListener.accept(scrollOffsetY);
        }
        body.layout(measure, layoutX, layoutY - scrollOffsetY, innerWidth, contentHeight);
    }

    @Override
    public void render(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
        if (!visible()) {
            return;
        }
        graphics.pushClipWithLogicalOutset(x(), y(), w(), h(), FTheme.scrollClipHorizontalOutsetLogical(), 0f);
        renderVisibleContent(graphics, mouseX, mouseY, deltaSeconds);
        graphics.popClip();
        if (isScrollbarVisible()) {
            renderScrollbar(graphics);
        }
    }

    private void renderScrollbar(GuiRenderer graphics) {
        float[] thumb = ScrollChrome.verticalThumbRect(x(), y(), w(), h(), contentHeight, scrollOffsetY);
        float thumbY = thumb[0];
        float thumbH = thumb[1];
        float thumbX = thumb[2];
        float thumbW = thumb[3];
        if (thumbH <= 0f || thumbW <= 0f) {
            return;
        }
        cachedThumbX = thumbX;
        cachedThumbY = thumbY;
        cachedThumbW = thumbW;
        cachedThumbH = thumbH;
        boolean active = thumbDragging || thumbHovered;
        float expand = active ? SCROLLBAR_HOVER_EXPAND_PX : 0f;
        float alpha = thumbDragging ? SCROLLBAR_FADE_DRAG_ALPHA : thumbHovered ? SCROLLBAR_FADE_HOVER_ALPHA : SCROLLBAR_FADE_IDLE_ALPHA;
        int color = applyAlphaFactor(FTheme.scrollbarThumb(), alpha);
        graphics.drawRect(thumbX - expand, thumbY, thumbW + expand * 2f, thumbH, color);
    }

    private boolean isScrollbarVisible() {
        return contentHeight > h();
    }

    private boolean isOverThumbHitArea(float px, float py) {
        if (!isScrollbarVisible()) {
            return false;
        }
        float slop = SCROLLBAR_HIT_SLOP_PX;
        return px >= cachedThumbX - slop && px <= cachedThumbX + cachedThumbW + slop && py >= cachedThumbY - slop && py <= cachedThumbY + cachedThumbH + slop;
    }

    private void renderVisibleContent(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
        if (!(body instanceof FColumnWidget)) {
            body.render(graphics, mouseX, mouseY, deltaSeconds);
            return;
        }
        for (FWidget child : body.childrenView()) {
            if (!child.visible()) {
                continue;
            }
            float childBottom = child.y() + child.h();
            if (childBottom < y() || child.y() > y() + h()) {
                continue;
            }
            child.render(graphics, mouseX, mouseY, deltaSeconds);
        }
    }

    private float measureContentHeight(UIRenderer measure, float innerWidth) {
        List<FWidget> rows = body.childrenView();
        if (rows.isEmpty()) {
            return 0f;
        }
        float gapBetweenRows = body instanceof FColumnWidget columnWidget ? columnWidget.gap() : rowGap;
        float sum = 0f;
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            sum += rows.get(rowIndex).intrinsicHeightForColumn(measure, innerWidth);
            if (rowIndex < rows.size() - 1) {
                sum += gapBetweenRows;
            }
        }
        return sum;
    }
}