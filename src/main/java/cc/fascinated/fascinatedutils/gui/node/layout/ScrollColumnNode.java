package cc.fascinated.fascinatedutils.gui.node.layout;

import cc.fascinated.fascinatedutils.gui.core.UiComposeContext;
import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.gui.core.state.UiScrollState;
import cc.fascinated.fascinatedutils.gui.layout.Axis;
import cc.fascinated.fascinatedutils.gui.layout.AxisConstraints;
import cc.fascinated.fascinatedutils.gui.layout.MeasuredSize;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

@Accessors(fluent = true)
public class ScrollColumnNode extends UiNode {
    private static final int SCROLLBAR_WIDTH = 3;
    private static final int SCROLLBAR_HOVER_WIDTH = 6;
    private static final int FOCUS_REVEAL_MARGIN = 4;
    private static final float PAGE_SCROLL_FRACTION = 0.9f;
    private static final int SCROLLBAR_LEFT_MARGIN = 4;
    private static final int SCROLLBAR_RIGHT_MARGIN = 2;
    private static final int SCROLLBAR_VERTICAL_PADDING = 2;
    private static final int SCROLLBAR_MIN_THUMB_HEIGHT = 16;
    private static final float WHEEL_PIXELS_PER_NOTCH = 28f;
    private static final float SMOOTHING_RATE = 18f;
    private static final float MAX_FRAME_SECONDS = 0.05f;
    private static final int SCROLLBAR_TOTAL = SCROLLBAR_LEFT_MARGIN + SCROLLBAR_WIDTH + SCROLLBAR_RIGHT_MARGIN;

    private final List<UiNode> visibleChildrenScratch = new ArrayList<>();
    private int gap;
    @Getter
    private int scrollOffset;
    private float visualOffset;
    private int lastLayoutMicros;
    private boolean hasLastLayout;
    private float wheelRemainder;
    private int contentHeight;
    private boolean reservesScrollbar;
    private String scrollPersistKey;
    private UiScrollState scrollPersistState;

    private boolean scrollbarDragging;
    private boolean scrollbarHovered;
    private float scrollbarDragStartY;
    private int scrollbarDragStartOffset;

    public ScrollColumnNode setGap(int gap) {
        this.gap = Math.max(0, gap);
        markLayoutDirty();
        return this;
    }

    public int maxScrollOffset() {
        return Math.max(0, contentHeight - bounds().height());
    }

    /**
     * Opts this scroll column into automatic scroll-position persistence across recomposes. The
     * given {@code key} must be unique within the screen. Once the tree is composed the offset is restored from the
     * host's remembered scroll state and written back whenever it changes.
     *
     * @param key the persistence key, unique within the screen
     * @return this node
     */
    public ScrollColumnNode persistScroll(String key) {
        this.scrollPersistKey = key;
        return this;
    }

    @Override
    protected void onComposed(UiComposeContext context) {
        if (scrollPersistKey == null) {
            return;
        }
        scrollPersistState = context.scrollState(scrollPersistKey);
        if (scrollPersistState.isInitialized()) {
            scrollOffset = Math.max(0, scrollPersistState.getOffset());
            visualOffset = scrollPersistState.getVisualOffset();
            lastLayoutMicros = scrollPersistState.getLastLayoutMicros();
            hasLastLayout = scrollPersistState.isHasLastLayout();
        }
    }

    @Override
    protected void inheritState(UiNode previous) {
        ScrollColumnNode previousColumn = (ScrollColumnNode) previous;
        scrollOffset = previousColumn.scrollOffset;
        visualOffset = previousColumn.visualOffset;
        lastLayoutMicros = previousColumn.lastLayoutMicros;
        hasLastLayout = previousColumn.hasLastLayout;
        wheelRemainder = previousColumn.wheelRemainder;
        contentHeight = previousColumn.contentHeight;
        reservesScrollbar = previousColumn.reservesScrollbar;
        scrollbarHovered = previousColumn.scrollbarHovered;
        scrollbarDragging = previousColumn.scrollbarDragging;
        scrollbarDragStartY = previousColumn.scrollbarDragStartY;
        scrollbarDragStartOffset = previousColumn.scrollbarDragStartOffset;
    }

    @Override
    public boolean clipChildren() {
        return true;
    }

    @Override
    public boolean hitTarget() {
        return true;
    }

    @Override
    public boolean onPointerScroll(float pointerX, float pointerY, float delta) {
        // Small deltas (smooth-scroll wheels, trackpads) accumulate until they amount to a whole pixel.
        float pending = wheelRemainder + delta * WHEEL_PIXELS_PER_NOTCH;
        int wholePixels = (int) pending;
        wheelRemainder = pending - wholePixels;
        if (!scrollTo(scrollOffset - wholePixels)) {
            wheelRemainder = 0f;
            return false;
        }
        return true;
    }

    @Override
    public boolean onKeyPress(int keyCode, int modifiers) {
        int viewportHeight = bounds().height();
        int pageDistance = Math.max(1, Math.round(viewportHeight * PAGE_SCROLL_FRACTION));
        int targetOffset;
        switch (keyCode) {
            case GLFW.GLFW_KEY_PAGE_UP -> targetOffset = scrollOffset - pageDistance;
            case GLFW.GLFW_KEY_PAGE_DOWN -> targetOffset = scrollOffset + pageDistance;
            case GLFW.GLFW_KEY_HOME -> targetOffset = 0;
            case GLFW.GLFW_KEY_END -> targetOffset = maxScrollOffset();
            default -> {
                return false;
            }
        }
        return scrollTo(targetOffset);
    }

    @Override
    public void revealDescendant(UiNode descendant) {
        int viewportTop = bounds().positionY();
        int viewportBottom = bounds().bottom();
        int descendantTop = descendant.bounds().positionY();
        int descendantBottom = descendant.bounds().bottom();
        if (descendantTop < viewportTop + FOCUS_REVEAL_MARGIN) {
            scrollTo(scrollOffset - (viewportTop + FOCUS_REVEAL_MARGIN - descendantTop));
        }
        else if (descendantBottom > viewportBottom - FOCUS_REVEAL_MARGIN) {
            scrollTo(scrollOffset + (descendantBottom - (viewportBottom - FOCUS_REVEAL_MARGIN)));
        }
    }

    private boolean scrollTo(int targetOffset) {
        int clampedOffset = Math.max(0, Math.min(maxScrollOffset(), targetOffset));
        if (clampedOffset == scrollOffset) {
            return false;
        }
        updateScrollOffset(clampedOffset);
        return true;
    }

    @Override
    public void onPointerLeave(float pointerX, float pointerY) {
        scrollbarHovered = false;
        super.onPointerLeave(pointerX, pointerY);
    }

    @Override
    public void render(AlumiteGuiRenderer renderFrame, float deltaSeconds) {
        super.render(renderFrame, deltaSeconds);
        if (visible() && inViewport() && maxScrollOffset() > 0) {
            renderScrollbar(renderFrame);
        }
    }

    private void renderScrollbar(AlumiteGuiRenderer renderFrame) {
        boolean emphasised = scrollbarHovered || scrollbarDragging;
        int barWidth = emphasised ? SCROLLBAR_HOVER_WIDTH : SCROLLBAR_WIDTH;
        int trackX = bounds().right() - barWidth - SCROLLBAR_RIGHT_MARGIN;
        int trackY = bounds().positionY() + SCROLLBAR_VERTICAL_PADDING;
        int trackH = bounds().height() - 2 * SCROLLBAR_VERTICAL_PADDING;

        renderFrame.drawRoundedRect(trackX, trackY, barWidth, trackH, barWidth / 2, renderFrame.theme().scrollbarTrack());

        int thumbH = thumbHeight(trackH, bounds().height());
        int thumbY = trackY + thumbOffset(trackH, thumbH);
        int thumbColor = scrollbarDragging ? renderFrame.theme().accent()
                : scrollbarHovered ? renderFrame.theme().textMuted() : renderFrame.theme().scrollbarThumb();
        renderFrame.drawRoundedRect(trackX, thumbY, barWidth, thumbH, barWidth / 2, thumbColor);
    }

    private int thumbHeight(int trackH, int viewportH) {
        return Math.max(SCROLLBAR_MIN_THUMB_HEIGHT, Math.round((float) viewportH / contentHeight * trackH));
    }

    private int thumbOffset(int trackH, int thumbH) {
        int trackRange = trackH - thumbH;
        int maxScroll = maxScrollOffset();
        return maxScroll > 0 ? Math.round(visualOffset / maxScroll * trackRange) : 0;
    }

    private boolean isOverTrack(float pointerX) {
        return maxScrollOffset() > 0 && pointerX >= bounds().right() - SCROLLBAR_HOVER_WIDTH - SCROLLBAR_RIGHT_MARGIN && pointerX <= bounds().right();
    }

    private boolean isOverThumb(float pointerX, float pointerY) {
        if (!isOverTrack(pointerX)) {
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
        if (button == 0 && isOverTrack(pointerX)) {
            int pageDistance = Math.max(1, Math.round(bounds().height() * PAGE_SCROLL_FRACTION));
            int trackY = bounds().positionY() + SCROLLBAR_VERTICAL_PADDING;
            int trackH = bounds().height() - 2 * SCROLLBAR_VERTICAL_PADDING;
            int thumbY = trackY + thumbOffset(trackH, thumbHeight(trackH, bounds().height()));
            scrollTo(pointerY < thumbY ? scrollOffset - pageDistance : scrollOffset + pageDistance);
            return true;
        }
        return false;
    }

    @Override
    public boolean onPointerMove(float pointerX, float pointerY) {
        scrollbarHovered = isOverTrack(pointerX);
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
        scrollTo(scrollbarDragStartOffset + Math.round(dragDelta / trackRange * maxScrollOffset()));
        visualOffset = scrollOffset;
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
    protected boolean layoutInputsChanged(AlumiteGuiRenderer renderFrame) {
        // The displayed offset eases toward the target over several frames.
        return super.layoutInputsChanged(renderFrame) || visualOffset != scrollOffset;
    }

    @Override
    protected void arrangeChildren(AlumiteGuiRenderer renderFrame, int positionX, int positionY, int width, int height) {
        visibleChildrenScratch.clear();
        for (UiNode childNode : childrenView()) {
            if (childNode.visible()) {
                visibleChildrenScratch.add(childNode);
            }
        }
        if (visibleChildrenScratch.isEmpty()) {
            contentHeight = 0;
            updateScrollOffset(0, false);
            visualOffset = 0f;
            reservesScrollbar = false;
            return;
        }

        // Reserve scrollbar space (including left margin) only when content actually overflows. The previous frame's
        // answer is the first guess, so a stable column measures its children once.
        int contentWidth = reservesScrollbar ? width - SCROLLBAR_TOTAL : width;
        contentHeight = measureContentHeight(renderFrame, contentWidth, height);
        if ((contentHeight > height) != reservesScrollbar) {
            reservesScrollbar = !reservesScrollbar;
            contentWidth = reservesScrollbar ? width - SCROLLBAR_TOTAL : width;
            contentHeight = measureContentHeight(renderFrame, contentWidth, height);
        }

        int maxScroll = Math.max(0, contentHeight - height);
        if (scrollOffset > maxScroll) {
            updateScrollOffset(maxScroll, false);
        }

        advanceVisualOffset(maxScroll);
        float cursorY = positionY - Math.round(visualOffset);
        for (UiNode childNode : visibleChildrenScratch) {
            MeasuredSize childSize = childNode.measure(renderFrame, contentWidth, height);
            cursorY += childNode.leadingMargin(Axis.VERTICAL, height);
            long horizontalBox = childNode.resolveBox(Axis.HORIZONTAL, positionX, contentWidth, childSize.width());
            childNode.arrange(renderFrame, AxisConstraints.boxStart(horizontalBox), Math.round(cursorY), AxisConstraints.boxSize(horizontalBox), childSize.height());
            cursorY += childSize.height() + childNode.trailingMargin(Axis.VERTICAL, height) + gap;
        }
    }

    private int measureContentHeight(AlumiteGuiRenderer renderFrame, int contentWidth, int height) {
        int total = gap * Math.max(0, visibleChildrenScratch.size() - 1);
        for (UiNode childNode : visibleChildrenScratch) {
            total += childNode.leadingMargin(Axis.VERTICAL, height) + Math.max(0, childNode.measure(renderFrame, contentWidth, height).height()) + childNode.trailingMargin(Axis.VERTICAL, height);
        }
        return total;
    }

    /**
     * Eases the displayed offset toward the target offset using frame-time based exponential smoothing, so wheel
     * scrolling glides instead of jumping. Snaps once within half a pixel, and on the first layout.
     *
     * @param maxScroll the largest valid offset for the current content
     */
    private void advanceVisualOffset(int maxScroll) {
        int nowMicros = (int) (System.nanoTime() / 1000L);
        float frameSeconds = hasLastLayout ? Math.min(MAX_FRAME_SECONDS, Math.max(0, nowMicros - lastLayoutMicros) / 1_000_000f) : 1f / 60f;
        lastLayoutMicros = nowMicros;
        hasLastLayout = true;
        visualOffset = Math.max(0f, Math.min(maxScroll, visualOffset));
        float distance = scrollOffset - visualOffset;
        if (scrollbarDragging || Math.abs(distance) < 0.5f) {
            visualOffset = scrollOffset;
        } else {
            visualOffset += distance * (1f - (float) Math.exp(-SMOOTHING_RATE * frameSeconds));
        }
        if (scrollPersistState != null) {
            scrollPersistState.setVisualOffset(visualOffset);
            scrollPersistState.setLastLayoutMicros(lastLayoutMicros);
            scrollPersistState.setHasLastLayout(true);
        }
    }

    private void updateScrollOffset(int nextOffset) {
        updateScrollOffset(nextOffset, true);
    }

    private void updateScrollOffset(int nextOffset, boolean relayout) {
        if (scrollOffset == nextOffset) {
            return;
        }
        scrollOffset = nextOffset;
        if (relayout) {
            markLayoutDirty();
        }
        if (scrollPersistState != null) {
            scrollPersistState.setOffset(scrollOffset);
            scrollPersistState.setInitialized(true);
        }
    }
}
