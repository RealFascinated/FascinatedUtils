package cc.fascinated.fascinatedutils.systems.modules.impl.clock;

import cc.fascinated.fascinatedutils.common.setting.impl.EnumSetting;
import cc.fascinated.fascinatedutils.systems.hud.HudDefaults;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.MiniMessageHudChrome;
import cc.fascinated.fascinatedutils.systems.modules.impl.clock.hud.ClockHudPanel;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class ClockWidget extends HudHostModule {

    private final EnumSetting<ClockFormat> clockFormat = EnumSetting.<ClockFormat>builder().id("clock_format")
            .defaultValue(ClockFormat.DMY_24H)
            .valueFormatter(ClockFormat::getFormat)
            .categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY)
            .build();

    public ClockWidget() {
        super("clock", "Clock", HudDefaults.builder().build());
        MiniMessageHudChrome.register(this);
        addSetting(clockFormat);
        registerHudPanel(new ClockHudPanel(this));
    }

    public EnumSetting<ClockFormat> clockFormatSetting() {
        return clockFormat;
    }

    @Getter
    @AllArgsConstructor
    public enum ClockFormat {
        DMY_12H("dd MMM yyyy, hh:mm a"), DMY_24H("dd MMM yyyy, HH:mm"), MDY_12H("MMM dd, yyyy hh:mm a"), MDY_24H("MMM dd, yyyy HH:mm"), ISO_24H("yyyy-MM-dd HH:mm"), ISO_SECONDS("yyyy-MM-dd HH:mm:ss"), DMY_SHORT("dd/MM/yyyy HH:mm"), TIME_ONLY_12H("hh:mm a"), TIME_ONLY_24H("HH:mm");
        private final String format;
    }
}
