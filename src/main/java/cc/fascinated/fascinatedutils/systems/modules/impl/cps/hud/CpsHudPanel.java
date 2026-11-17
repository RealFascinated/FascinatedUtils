package cc.fascinated.fascinatedutils.systems.modules.impl.cps.hud;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.systems.modules.impl.cps.MouseClickRateTracker;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.panel.MiniMessageHudPanel;
import cc.fascinated.fascinatedutils.systems.modules.impl.cps.CpsWidget;

import java.util.List;

public class CpsHudPanel extends MiniMessageHudPanel {

    private final CpsWidget cpsWidget;

    public CpsHudPanel(CpsWidget cpsWidget) {
        super(cpsWidget, "cps", HudHostModule.UTILITY_WIDGET_MIN_WIDTH);
        this.cpsWidget = cpsWidget;
    }

    @Override
    protected List<String> computeMiniMessageLines(float deltaSeconds, boolean editorMode) {
        BooleanSetting splitButtons = cpsWidget.getSplitMouseButtons();
        int leftClicksPerSecond = MouseClickRateTracker.INSTANCE.leftClicksPerSecond();
        int rightClicksPerSecond = MouseClickRateTracker.INSTANCE.rightClicksPerSecond();
        if (splitButtons.isEnabled()) {
            return List.of("%s <grey>|</grey> %s %s".formatted(leftClicksPerSecond, rightClicksPerSecond, hudText("cps")));
        }
        return List.of(MouseClickRateTracker.INSTANCE.combinedClicksPerSecond() + " " + hudText("cps"));
    }
}
