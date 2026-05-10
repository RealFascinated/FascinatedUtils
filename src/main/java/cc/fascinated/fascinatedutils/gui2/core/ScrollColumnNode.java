package cc.fascinated.fascinatedutils.gui2.core;

import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;

import java.util.function.Consumer;

public class ScrollColumnNode extends UiNode {
    private static final int SCROLLBAR_WIDTH = 3;
    private static final int SCROLLBAR_LEFT_MARGIN = 4;
    private static final int SCROLLBAR_RIGHT_MARGIN = 2;
    private static final int SCROLLBAR_VERTICAL_PADDING = 2;
    private static final int SCROLLBAR_MIN_THUMB_HEIGHT = 16;
    private static final int SCROLLBAR_TOTAL = SCROLLBAR_LEFT_MARGIN + SCROLLBAR_WIDTH + SCROLLBAR_RIGHT_MARGIN;

    private int gap;
    private int scrollOffset;
    private int contentHeight;
    private Consumer<Integer> onScrollOffsetChanged = ignored -> {};

    private boolean scrollbarDragging;
    private float scrollbarDragStartY;
    private int scrollbarDragStartOffset;

    public ScrollColumnNode setGap(int gap) {
        this.gap = Math.max(0, gap);
        return this;
    }

    public int scrollOffset() {
        return scrollOffset;
    }

    public void setScrollOffset(int scrollOffset) {
        updateScrollOffset(Math.max(0, scrollOffset));
    }

    public int maxScrollOffset() {
        return Math.max(0, contentHeight - bounds().height());
    }

    public ScrollColumnNode setOnScrollOffsetChanged(Consumer<Integer> onScrollOffsetChanged) {
        this.onScrollOffsetChanged = onScrollOffsetChanged == null ? ignored -> {} : onScrollOffsetChanged;
        return this;
    }

    @Override
    public boolean clipChildren() {
        return true;
    }

    @Override
    public boolean blocksHitWhenEmpty() {
        return true;
    }

    @Override
    public boolean onPointerScroll(float pointerX, float pointerY, float delta) {
        int maxScroll = maxScrollOffset();
        int nextOffset = scrollOffset - Math.round(delta * 28f);
        updateScrollOffset(Math.max(0, Math.min(maxScroll, nextOffset)));
        return true;
    }

    @Override
    public void render(RenderFrame renderFrame, float deltaSeconds) {
        super.render(renderFrame, deltaSeconds);
        if (maxScrollOffset() > 0) {
            renderScrollbar(renderFrame);
        }
    }

    private void renderScrollbar(RenderFrame renderFrame) {
        int trackX = bounds().right() - SCROLLBAR_WIDTH - SCROLLBAR_RIGHT_MARGIN;
        int trackY = bounds().positionY() + SCROLLBAR_VERTICAL_PADDING;
        int trackH = bounds().height() - 2 * SCROLLBAR_VERTICAL_PADDING;

        renderFrame.drawRoundedRect(trackX, trackY, SCROLLBAR_WIDTH, trackH, SCROLLBAR_WIDTH / 2, renderFrame.theme().scrollbarTrack());

        int thumbH = thumbHeight(trackH, bounds().height());
        int thumbY = trackY + thumbOffset(trackH, thumbH);
        int thumbColor = scrollbarDragging ? renderFrame.theme().accent() : renderFrame.theme().scrollbarThumb();
        renderFrame.drawRoundedRect(trackX, thumbY, SCROLLBAR_WIDTH, thumbH, SCROLLBAR_WIDTH / 2, thumbColor);
    }

    private int thumbHeight(int trackH, int viewportH) {
        return Math.max(SCROLLBAR_MIN_THUMB_HEIGHT, Math.round((float) viewportH / contentHeight * trackH));
    }

    private int thumbOffset(int trackH, int thumbH) {
        int trackRange = trackH - thumbH;
        int maxScroll = maxScrollOffset();
        return maxScroll > 0 ? Math.round((float) scrollOffset / maxScroll * trackRange) : 0;
    }

    private boolean isOverThumb(float pointerX, float pointerY) {
        if (maxScrollOffset() <= 0) {
            return false;
        }
        int trackX = bounds().right() - SCROLLBAR_WIDTH - SCROLLBAR_RIGHT_MARGIN;
        if (pointerX < trackX || pointerX > bounds().right()) {
            return false;
        }
        int trackY = bounds().positionY() + SCROLLBAR_VERTICAL_PADDING;
        int trackH = bounds().height() - 2 * SCROLLBAR_VERTICAL_PADDING;
        int thumbH = thumbHeight(trackH, bounds().height());
        int thumbY = trackY + thumbOffset(trackH, thumbH);
        return pointerY >= thumbY && pointerY <= thumbY + thumbH;
    }

    @Override
    public boolean onPointerPress(float pointerX, float pointerY, int button) {
        if (button == 0 && isOverThumb(pointerX, pointerY)) {
            scrollbarDragging = true;
            scrollbarDragStartY = pointerY;
            scrollbarDragStartOffset = scrollOffset;
            return true;
        }
        return false;
    }

    @Override
    public boolean onPointerMove(float pointerX, float pointerY) {
        if (!scrollbarDragging) {
            return false;
        }
        int trackH = bounds().height() - 2 * SCROLLBAR_VERTICAL_PADDING;
        int thumbH = thumbHeight(trackH, bounds().height());
        int trackRange = trackH - thumbH;
        if (trackRange <= 0) {
            return true;
        }
        float dragDelta = pointerY - scrollbarDragStartY;
        setScrollOffset(scrollbarDragStartOffset + Math.round(dragDelta / trackRange * maxScrollOffset()));
        return true;
    }

    @Override
    public boolean onPointerRelease(float pointerX, float pointerY, int button) {
        if (!scrollbarDragging) {
            return false;
        }
        scrollbarDragging = false;
        return true;
    }

    @Override
    public void layout(RenderFrame renderFrame, int positionX, int positionY, int width, int height) {
        bounds().set(positionX, positionY, width, height);

        UiNode[] visibleChildren = childrenView().stream().filter(UiNode::visible).toArray(UiNode[]::new);
        int visibleCount = visibleChildren.length;
        if (visibleCount == 0) {
            contentHeight = 0;
            scrollOffset = 0;
            return;
        }

        // First pass: measure content height using full width.
        contentHeight = 0;
        for (UiNode childNode : visibleChildren) {
            childNode.layout(renderFrame, positionX, 0, width, height);
            contentHeight += Math.max(0, childNode.bounds().height());
        }
        contentHeight += gap * Math.max(0, visibleCount - 1);

        // Reserve scrollbar space (including left margin) only when content actually overflows.
        boolean needsScrollbar = contentHeight > height;
        int contentWidth = needsScrollbar ? width - SCROLLBAR_TOTAL : width;

        // Re-measure with the narrower width in case children reflow with scrollbar present.
        if (needsScrollbar) {
            contentHeight = 0;
            for (UiNode childNode : visibleChildren) {
                childNode.layout(renderFrame, positionX, 0, contentWidth, height);
                contentHeight += Math.max(0, childNode.bounds().height());
            }
            contentHeight += gap * Math.max(0, visibleCount - 1);
        }

        int maxScroll = Math.max(0, contentHeight - height);
        if (scrollOffset > maxScroll) {
            updateScrollOffset(maxScroll);
        }

        int cursorY = positionY - scrollOffset;
        for (UiNode childNode : visibleChildren) {
            childNode.layout(renderFrame, positionX, cursorY, contentWidth, height);
            cursorY += Math.max(0, childNode.bounds().height()) + gap;
        }
    }

    private void updateScrollOffset(int nextOffset) {
        if (scrollOffset == nextOffset) {
            return;
        }
        scrollOffset = nextOffset;
        onScrollOffsetChanged.accept(scrollOffset);
    }
}
