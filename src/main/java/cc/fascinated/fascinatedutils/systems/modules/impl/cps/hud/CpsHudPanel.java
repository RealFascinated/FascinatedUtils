package cc.fascinated.fascinatedutils.systems.modules.impl.cps.hud;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.gui.input.MouseClickRateTracker;
import cc.fascinated.fascinatedutils.systems.modules.impl.cps.CpsWidget;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.MiniMessageHudPanel;

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
            return List.of("%s <grey>|</grey> %s CPS".formatted(leftClicksPerSecond, rightClicksPerSecond));
        }
        return List.of(MouseClickRateTracker.INSTANCE.combinedClicksPerSecond() + " CPS");
    }
}
