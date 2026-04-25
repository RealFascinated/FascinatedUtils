package cc.fascinated.fascinatedutils.systems.hud;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.ColorSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.systems.hud.content.HudContent;

import java.util.List;
import java.util.concurrent.TimeUnit;

public abstract class HudMiniMessageModule extends HudModule {
    public static final float UTILITY_WIDGET_MIN_WIDTH = 90f;
    private static final long DEFAULT_UPDATE_INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(100L);

    private final BooleanSetting showBackground = HudWidgetAppearanceBuilders.showBackground().build();
    private final BooleanSetting roundedCorners = HudWidgetAppearanceBuilders.roundedCorners().build();
    private final SliderSetting roundingRadius = HudWidgetAppearanceBuilders.roundingRadius().build();
    private final BooleanSetting showBorder = HudWidgetAppearanceBuilders.showBorder().build();
    private final SliderSetting borderThickness = HudWidgetAppearanceBuilders.borderThickness().build();
    private final ColorSetting backgroundColor = HudWidgetAppearanceBuilders.backgroundColor().build();
    private final ColorSetting borderColor = HudWidgetAppearanceBuilders.borderColor().build();

    private List<String> cachedMiniMessageLines;
    private long lastMiniMessageSampleNanos;

    protected HudMiniMessageModule(String widgetId, String name, float minWidth) {
        super(widgetId, name, minWidth);
        addSetting(showBackground);
        addSetting(roundedCorners);
        addSetting(showBorder);
        addSetting(roundingRadius);
        addSetting(borderThickness);
        addSetting(backgroundColor);
        addSetting(borderColor);
        showBackground.addSubSetting(backgroundColor);
        roundedCorners.addSubSetting(roundingRadius);
        showBorder.addSubSetting(borderThickness);
        showBorder.addSubSetting(borderColor);
    }

    private static List<String> normalizeMiniMessageLines(List<String> rawLines) {
        if (rawLines == null || rawLines.isEmpty()) {
            return List.of("");
        }
        return List.copyOf(rawLines);
    }

    protected abstract List<String> lines(float deltaSeconds);

    /**
     * Wall-clock interval between recomputing HUD lines from {@link #resolveRawMiniMessageLines}.
     * Return {@code 0L} to refresh every frame.
     *
     * @return elapsed time threshold in nanoseconds
     */
    protected long hudMiniMessageUpdateIntervalNanos() {
        return DEFAULT_UPDATE_INTERVAL_NANOS;
    }

    /**
     * Raw MiniMessage lines before normalization and caching. The default delegates to {@link #lines}.
     *
     * @param deltaSeconds frame delta in seconds
     * @param editorMode   true in the HUD editor
     * @return lines to display, or {@code null} / empty to fall back to a single blank line
     */
    protected List<String> resolveRawMiniMessageLines(float deltaSeconds, boolean editorMode) {
        return lines(deltaSeconds);
    }

    @Override
    protected HudContent produceContent(float deltaSeconds, boolean editorMode) {
        return new HudContent.TextLines(miniMessageLinesWithCache(deltaSeconds, editorMode));
    }

    private List<String> miniMessageLinesWithCache(float deltaSeconds, boolean editorMode) {
        long intervalNanos = hudMiniMessageUpdateIntervalNanos();
        long now = System.nanoTime();
        if (intervalNanos <= 0L || cachedMiniMessageLines == null || now - lastMiniMessageSampleNanos >= intervalNanos) {
            cachedMiniMessageLines = normalizeMiniMessageLines(resolveRawMiniMessageLines(deltaSeconds, editorMode));
            lastMiniMessageSampleNanos = now;
        }
        return cachedMiniMessageLines;
    }
}
