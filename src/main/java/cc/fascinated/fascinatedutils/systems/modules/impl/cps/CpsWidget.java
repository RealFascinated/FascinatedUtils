package cc.fascinated.fascinatedutils.systems.modules.impl.cps;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.gui.input.MouseClickRateTracker;
import cc.fascinated.fascinatedutils.systems.modules.impl.cps.hud.CpsHudPanel;
import cc.fascinated.fascinatedutils.systems.hud.HudMiniMessageModule;

import java.util.List;

public class CpsWidget extends HudMiniMessageModule {
    private final BooleanSetting splitMouseButtons = BooleanSetting.builder().id("split_mouse_buttons")

            .defaultValue(false).categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();

    public CpsWidget() {
        super("cps", "CPS", UTILITY_WIDGET_MIN_WIDTH);
        addSetting(splitMouseButtons);
        registerHudPanel(new CpsHudPanel(this));
    }

    @Override
    protected List<String> lines(float deltaSeconds) {
        int leftClicksPerSecond = MouseClickRateTracker.INSTANCE.leftClicksPerSecond();
        int rightClicksPerSecond = MouseClickRateTracker.INSTANCE.rightClicksPerSecond();
        if (splitMouseButtons.getValue()) {
            return List.of("%s <grey>|</grey> %s CPS".formatted(leftClicksPerSecond, rightClicksPerSecond));
        }
        return List.of(MouseClickRateTracker.INSTANCE.combinedClicksPerSecond() + " CPS");
    }
}
