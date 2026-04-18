package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.common.setting.impl.EnumSetting;
import cc.fascinated.fascinatedutils.systems.hud.HudMiniMessageModule;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClockWidget extends HudMiniMessageModule {
    private final Map<ClockFormat, DateTimeFormatter> formatterCache = new ConcurrentHashMap<>();
    private final EnumSetting<ClockFormat> clockFormat = EnumSetting.<ClockFormat>builder().id("clock_format").defaultValue(ClockFormat.DMY_24H).valueFormatter(ClockFormat::getFormat).categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();

    public ClockWidget() {
        super("clock", "Clock", 0f);
        addSetting(clockFormat);
    }

    @Override
    protected List<String> lines(float deltaSeconds) {
        return List.of(LocalDateTime.now().format(getFormatter()));
    }

    private DateTimeFormatter getFormatter() {
        return this.formatterCache.computeIfAbsent(clockFormat.getValue(), formatter -> DateTimeFormatter.ofPattern(formatter.getFormat()));
    }

    @Getter
    @AllArgsConstructor
    public enum ClockFormat {
        DMY_12H("dd MMM yyyy, hh:mm a"), DMY_24H("dd MMM yyyy, HH:mm"), MDY_12H("MMM dd, yyyy hh:mm a"), MDY_24H("MMM dd, yyyy HH:mm"), ISO_24H("yyyy-MM-dd HH:mm"), ISO_SECONDS("yyyy-MM-dd HH:mm:ss"), DMY_SHORT("dd/MM/yyyy HH:mm"), TIME_ONLY_12H("hh:mm a"), TIME_ONLY_24H("HH:mm");
        private final String format;
    }
}
