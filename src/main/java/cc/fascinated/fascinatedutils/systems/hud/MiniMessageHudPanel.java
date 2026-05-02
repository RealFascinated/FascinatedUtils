package cc.fascinated.fascinatedutils.systems.hud;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.systems.hud.anchor.HudAnchorContentAlignment;
import cc.fascinated.fascinatedutils.systems.hud.content.HudContent;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.TimeUnit;

public abstract class MiniMessageHudPanel extends HudPanel {
    private static final long DEFAULT_UPDATE_INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(100L);

    private final float baseMinWidthValue;
    private List<String> cachedMiniMessageLines;
    private long lastMiniMessageSampleNanos;

    protected MiniMessageHudPanel(HudHostModule hostModule, String panelId, float baseMinWidth) {
        super(hostModule, panelId, baseMinWidth);
        this.baseMinWidthValue = baseMinWidth;
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
     * Wall-clock sampling interval for {@link #computeMiniMessageLines}. Return {@code 0L} to refresh every frame.
     *
     * @return minimum nanoseconds between recomputes
     */
    protected long miniMessageLineUpdateIntervalNanos() {
        return DEFAULT_UPDATE_INTERVAL_NANOS;
    }

    /**
     * Resolves {@link HudHostModule#SETTING_REMOVE_MIN_WIDTH} after any panel-specific minimum width rules.
     *
     * @param baseMinWidth width from the HUD panel registration
     * @return width passed to layout
     */
    protected float resolvePanelMinimumWidth(float baseMinWidth) {
        return applyRemoveMinimumWidthFromHost(hudHostModule(), baseMinWidth);
    }

    /**
     * @param host                   settings host for {@link HudHostModule#SETTING_REMOVE_MIN_WIDTH}
     * @param resolvedMinWidthBefore width after panel rules but before stripping
     * @return zero when remove-min-width is enabled, otherwise {@code resolvedMinWidthBefore}
     */
    protected static float applyRemoveMinimumWidthFromHost(HudHostModule host, float resolvedMinWidthBefore) {
        if (host.getSetting(BooleanSetting.class, HudHostModule.SETTING_REMOVE_MIN_WIDTH).filter(BooleanSetting::isEnabled).isPresent()) {
            return 0f;
        }
        return resolvedMinWidthBefore;
    }

    /**
     * Optional horizontal alignment for centered MiniMessage lines ({@code null} = centered band).
     *
     * @return alignment override, or {@code null} for default
     */
    protected HudAnchorContentAlignment.@Nullable Horizontal textLineHorizontalAlignmentOverrideForPanel() {
        return null;
    }

    private static List<String> normalizeMiniMessageLines(List<String> rawLines) {
        if (rawLines == null || rawLines.isEmpty()) {
            return List.of("");
        }
        return List.copyOf(rawLines);
    }

    private List<String> miniMessageLinesWithCache(float deltaSeconds, boolean editorMode) {
        long intervalNanos = miniMessageLineUpdateIntervalNanos();
        long now = System.nanoTime();
        if (intervalNanos <= 0L || cachedMiniMessageLines == null || now - lastMiniMessageSampleNanos >= intervalNanos) {
            cachedMiniMessageLines = normalizeMiniMessageLines(computeMiniMessageLines(deltaSeconds, editorMode));
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
        return new HudContent.TextLines(miniMessageLinesWithCache(deltaSeconds, editorMode));
    }

    @Override
    protected HudAnchorContentAlignment.@Nullable Horizontal hudTextLineHorizontalAlignmentOverride() {
        return textLineHorizontalAlignmentOverrideForPanel();
    }
}
