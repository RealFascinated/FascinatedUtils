package cc.fascinated.fascinatedutils.systems.modules.impl.reach.hud;

import cc.fascinated.fascinatedutils.common.Colors;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.MiniMessageHudPanel;
import cc.fascinated.fascinatedutils.systems.modules.impl.reach.ReachWidget;

import java.util.List;
import java.util.Locale;

public class ReachHudPanel extends MiniMessageHudPanel {

    private final ReachWidget reachWidget;

    public ReachHudPanel(ReachWidget reachWidget) {
        super(reachWidget, "reach", HudHostModule.UTILITY_WIDGET_MIN_WIDTH);
        this.reachWidget = reachWidget;
    }

    @Override
    protected List<String> computeMiniMessageLines(float deltaSeconds, boolean editorMode) {
        float lastEntityReach = reachWidget.getLastEntityReach();
        if (!Float.isFinite(lastEntityReach)) {
            return List.of("<yellow>N/A <white>blocks");
        }
        float fraction = Math.min((lastEntityReach - 3f) / 3f, 1f);
        String color = Colors.rgbHex(Colors.getGoodBadColor(Math.max(fraction, 0f), true));
        return List.of(String.format(Locale.ENGLISH, "<color:%s>%.2f <white>blocks", color, lastEntityReach));
    }
}
