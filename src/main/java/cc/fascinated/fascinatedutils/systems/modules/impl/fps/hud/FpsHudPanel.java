package cc.fascinated.fascinatedutils.systems.modules.impl.fps.hud;

import cc.fascinated.fascinatedutils.common.Colors;
import cc.fascinated.fascinatedutils.common.FrameCounter;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.theme.UiColor;
import cc.fascinated.fascinatedutils.systems.modules.impl.fps.FpsWidget;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.MiniMessageHudPanel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class FpsHudPanel extends MiniMessageHudPanel {

    private static final long UPDATE_INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(50L);
    private static final int FPS_COLOR_YELLOW = UiColor.argb("#dddd44");
    private static final int FPS_COLOR_AMBER = UiColor.argb("#ddaa33");
    private static final int FPS_COLOR_RED = UiColor.argb("#dd4444");

    private final FpsWidget fpsWidget;

    public FpsHudPanel(FpsWidget fpsWidget) {
        super(fpsWidget, "fps", HudHostModule.UTILITY_WIDGET_MIN_WIDTH);
        this.fpsWidget = fpsWidget;
    }

    @Override
    protected long miniMessageLineUpdateIntervalNanos() {
        return UPDATE_INTERVAL_NANOS;
    }

    @Override
    protected List<String> computeMiniMessageLines(float deltaSeconds, boolean editorMode) {
        FrameCounter instance = FrameCounter.getInstance();
        int fps = instance.getSmoothFps();
        List<String> lines = new ArrayList<>();
        lines.add("<" + Colors.rgbHex(fpsColorArgb(fps)) + ">" + fps + " <white>FPS");
        BooleanSetting onePercentLowSetting = fpsWidget.getShowOnePercentLows();
        if (onePercentLowSetting.isEnabled()) {
            int onePercentLows = instance.getOnePercentLowFps();
            lines.add("<grey>1%: <" + Colors.rgbHex(fpsColorArgb(onePercentLows)) + "><white>" + onePercentLows);
        }
        BooleanSetting pointOneLowSetting = fpsWidget.getShowPointOnePercentLows();
        if (pointOneLowSetting.isEnabled()) {
            int pointOnePercentLowFps = instance.getPointOnePercentLowFps();
            lines.add("<grey>0.1%: <" + Colors.rgbHex(fpsColorArgb(pointOnePercentLowFps)) + "><white>" + pointOnePercentLowFps);
        }
        return lines;
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
}
