package cc.fascinated.fascinatedutils.systems.modules.impl.days.hud;

import cc.fascinated.fascinatedutils.systems.modules.impl.days.DaysWidget;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.MiniMessageHudPanel;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class DaysHudPanel extends MiniMessageHudPanel {

    private static final long TICKS_PER_DAY = 24000L;
    private static final long UPDATE_INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(500L);

    public DaysHudPanel(DaysWidget daysWidget) {
        super(daysWidget, "days", HudHostModule.UTILITY_WIDGET_MIN_WIDTH);
    }

    @Override
    protected long miniMessageLineUpdateIntervalNanos() {
        return UPDATE_INTERVAL_NANOS;
    }

    @Override
    protected List<String> computeMiniMessageLines(float deltaSeconds, boolean editorMode) {
        if (editorMode && Minecraft.getInstance().level == null) {
            return List.of("Day <white>124");
        }
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return List.of("<grey>Day —</grey>");
        }
        long worldDay = Math.floorDiv(client.level.getGameTime(), TICKS_PER_DAY) + 1L;
        return List.of("Day <white>" + worldDay);
    }
}
