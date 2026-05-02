package cc.fascinated.fascinatedutils.systems.modules.impl.clock.hud;

import cc.fascinated.fascinatedutils.systems.modules.impl.clock.ClockWidget;
import cc.fascinated.fascinatedutils.systems.hud.MiniMessageHudPanel;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClockHudPanel extends MiniMessageHudPanel {

    private final Map<ClockWidget.ClockFormat, DateTimeFormatter> formatterCache = new ConcurrentHashMap<>();
    private final ClockWidget clockWidget;

    public ClockHudPanel(ClockWidget clockWidget) {
        super(clockWidget, "clock", 0f);
        this.clockWidget = clockWidget;
    }

    @Override
    protected List<String> computeMiniMessageLines(float deltaSeconds, boolean editorMode) {
        return List.of(LocalDateTime.now().format(getFormatter(clockWidget.clockFormatSetting().getValue())));
    }

    private DateTimeFormatter getFormatter(ClockWidget.ClockFormat format) {
        return formatterCache.computeIfAbsent(format, key -> DateTimeFormatter.ofPattern(key.getFormat()));
    }
}
