package cc.fascinated.fascinatedutils.systems.hud;

import cc.fascinated.fascinatedutils.systems.hud.anchor.HudAnchorContentAlignment;
import cc.fascinated.fascinatedutils.systems.hud.content.HudContent;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.TimeUnit;

public abstract class MiniMessageHudPanel extends HudPanel {
    private static final long DEFAULT_UPDATE_INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(100L);

    private final float baseMinWidthValue;
    private final long lineUpdateIntervalNanos;
    private List<String> cachedMiniMessageLines;
    private HudContent.TextLines cachedTextLines;
    private boolean cachedLinesHidden;
    private long lastMiniMessageSampleNanos;

    protected MiniMessageHudPanel(HudHostModule hostModule, String panelId, float baseMinWidth) {
        this(hostModule, panelId, baseMinWidth, DEFAULT_UPDATE_INTERVAL_NANOS);
    }

    /**
     * Creates a MiniMessage panel that recomputes its lines at the given interval.
     *
     * @param hostModule          host module that owns this panel
     * @param panelId             stable identifier for this panel within the host
     * @param baseMinWidth        width from the HUD panel registration
     * @param lineUpdateIntervalNanos minimum nanoseconds between {@link #computeMiniMessageLines} calls; {@code 0L} refreshes every frame
     */
    protected MiniMessageHudPanel(HudHostModule hostModule, String panelId, float baseMinWidth, long lineUpdateIntervalNanos) {
        super(hostModule, panelId, baseMinWidth);
        this.baseMinWidthValue = baseMinWidth;
        this.lineUpdateIntervalNanos = lineUpdateIntervalNanos;
    }

    private static List<String> normalizeMiniMessageLines(List<String> rawLines) {
        if (rawLines == null || rawLines.isEmpty()) {
            return List.of("");
        }
        return List.copyOf(rawLines);
    }

    /**
     * Lines to measure and draw this frame before normalization.
     *
     * @param deltaSeconds frame delta in seconds
     * @param editorMode   true inside the HUD layout editor preview
     * @return lines to render, or {@code null} / empty to fall back to a single blank line
     */
    protected abstract List<String> computeMiniMessageLines(float deltaSeconds, boolean editorMode);

    /**
     * Resolves {@link HudHostModule#SETTING_REMOVE_MIN_WIDTH} after any panel-specific minimum width rules.
     *
     * @param baseMinWidth width from the HUD panel registration
     * @return width passed to layout
     */
    protected float resolvePanelMinimumWidth(float baseMinWidth) {
        if (hudHostModule().isRemoveMinimumWidthEnabled()) {
            return 0f;
        }
        return baseMinWidth;
    }

    /**
     * Optional horizontal alignment for centered MiniMessage lines ({@code null} = centered band).
     *
     * @return alignment override, or {@code null} for default
     */
    protected HudAnchorContentAlignment.@Nullable Horizontal textLineHorizontalAlignmentOverrideForPanel() {
        return null;
    }

    /**
     * Whether the panel should draw nothing (no background or border either) when
     * {@link #computeMiniMessageLines} returns {@code null} or empty, instead of a single blank line.
     *
     * @return {@code true} to hide the panel when there are no lines
     */
    protected boolean hideWhenNoLines() {
        return false;
    }

    private List<String> miniMessageLinesWithCache(float deltaSeconds, boolean editorMode) {
        long now = System.nanoTime();
        if (lineUpdateIntervalNanos <= 0L || cachedMiniMessageLines == null || now - lastMiniMessageSampleNanos >= lineUpdateIntervalNanos) {
            List<String> rawLines = computeMiniMessageLines(deltaSeconds, editorMode);
            cachedLinesHidden = hideWhenNoLines() && (rawLines == null || rawLines.isEmpty());
            cachedMiniMessageLines = normalizeMiniMessageLines(rawLines);
            cachedTextLines = new HudContent.TextLines(cachedMiniMessageLines);
            lastMiniMessageSampleNanos = now;
        }
        return cachedMiniMessageLines;
    }

    @Override
    protected float effectiveMinWidth() {
        return resolvePanelMinimumWidth(baseMinWidthValue);
    }

    @Override
    protected HudContent produceHudContent(float deltaSeconds, boolean editorMode) {
        miniMessageLinesWithCache(deltaSeconds, editorMode);
        if (cachedLinesHidden) {
            return null;
        }
        return cachedTextLines;
    }

    @Override
    protected HudAnchorContentAlignment.@Nullable Horizontal hudTextLineHorizontalAlignmentOverride() {
        return textLineHorizontalAlignmentOverrideForPanel();
    }
}
