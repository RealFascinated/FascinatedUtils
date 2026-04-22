package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.common.Colors;
import cc.fascinated.fascinatedutils.common.FrameCounter;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.theme.UiColor;
import cc.fascinated.fascinatedutils.systems.hud.HudMiniMessageModule;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class FpsWidget extends HudMiniMessageModule {

    private static final long UPDATE_INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(50L);
    private static final int FPS_COLOR_YELLOW = UiColor.argb("#dddd44");
    private static final int FPS_COLOR_AMBER = UiColor.argb("#ddaa33");
    private static final int FPS_COLOR_RED = UiColor.argb("#dd4444");

    private final BooleanSetting showOnePercentLows = BooleanSetting.builder().id("show_one_percent_lows")
            .defaultValue(false)
            .categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY)
            .build();

    private final BooleanSetting showPointOnePercentLows = BooleanSetting.builder().id("show_point_one_percent_lows")
            .defaultValue(false)
            .categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY)
            .build();

    public FpsWidget() {
        super("fps", "FPS", UTILITY_WIDGET_MIN_WIDTH);
        addSetting(showOnePercentLows);
        addSetting(showPointOnePercentLows);
    }

    private static int fpsColorArgb(int fps) {
        if (fps >= 55) {
            return UITheme.COLOR_TEXT_PRIMARY;
        }
        if (fps >= 40) {
            return FPS_COLOR_YELLOW;
        }
        if (fps >= 30) {
            return FPS_COLOR_AMBER;
        }
        return FPS_COLOR_RED;
    }

    @Override
    protected List<String> lines(float deltaSeconds) {
        FrameCounter instance = FrameCounter.getInstance();
        int fps = instance.getSmoothFps();
        List<String> lines = new ArrayList<>();
        lines.add("<" + Colors.rgbHex(fpsColorArgb(fps)) + ">" + fps + " FPS");
        if (showOnePercentLows.getValue()) {
            int onePercentLows = instance.getOnePercentLowFps();
            lines.add("<grey>1%: <" + Colors.rgbHex(fpsColorArgb(onePercentLows)) + ">" + onePercentLows);
        }
        if (showPointOnePercentLows.getValue()) {
            int pointOnePercentLowFps = instance.getPointOnePercentLowFps();
            lines.add("<grey>0.1%: <" + Colors.rgbHex(fpsColorArgb(pointOnePercentLowFps)) + ">" + pointOnePercentLowFps);
        }
        return lines;
    }

    @Override
    protected long hudMiniMessageUpdateIntervalNanos() {
        return UPDATE_INTERVAL_NANOS;
    }
}
