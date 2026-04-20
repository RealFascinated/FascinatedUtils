package cc.fascinated.fascinatedutils.gui.core;

import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import net.minecraft.util.Mth;

public class ScrollChrome {

    public static float scrollbarReservedWidthPx() {
        return UITheme.SCROLLBAR_RESERVED_W;
    }

    public static float scrollbarThumbWidthPx() {
        return UITheme.SCROLLBAR_THUMB_W;
    }

    public static float scrollbarMinThumbHeightPx() {
        return UITheme.SCROLLBAR_MIN_THUMB_H;
    }

    public static float innerWidthInsetForScrollbarPx(float totalContentHeight, float viewportInnerHeight) {
        if (totalContentHeight <= viewportInnerHeight) {
            return 0f;
        }
        return scrollbarReservedWidthPx();
    }

    /**
     * Resolve scrollable content height from an explicit override or measured child height.
     */
    public static float resolveScrollableContentHeight(float explicitScrollContentHeight, float measuredChildContentHeight) {
        if (Float.isFinite(explicitScrollContentHeight) && explicitScrollContentHeight >= 0f) {
            return Math.max(0f, explicitScrollContentHeight);
        }
        return Math.max(0f, measuredChildContentHeight);
    }

    public static float[] verticalThumbRect(float containerX, float containerY, float containerWidth, float containerHeight, float totalContentHeight, float scrollOffsetY) {
        if (totalContentHeight <= containerHeight) {
            return new float[]{containerY, 0f, containerX, 0f};
        }
        float visibleFraction = containerHeight / totalContentHeight;
        float minThumb = scrollbarMinThumbHeightPx();
        float thumbHeight = Math.min(containerHeight, Math.max(minThumb, containerHeight * visibleFraction));
        float scrollRange = totalContentHeight - containerHeight;
        float scrollFraction = scrollRange > 0f ? scrollOffsetY / scrollRange : 0f;
        scrollFraction = Mth.clamp(scrollFraction, 0f, 1f);
        float thumbY = containerY + scrollFraction * (containerHeight - thumbHeight);
        float reserved = scrollbarReservedWidthPx();
        float thumbWidth = scrollbarThumbWidthPx();
        float thumbX = containerX + containerWidth - reserved + (reserved - thumbWidth) / 2f;
        return new float[]{thumbY, thumbHeight, thumbX, thumbWidth};
    }
}
