package cc.fascinated.fascinatedutils.debug.perf.tests;

import cc.fascinated.fascinatedutils.debug.perf.PerfTest;
import cc.fascinated.fascinatedutils.systems.hud.HUDManager;
import cc.fascinated.fascinatedutils.systems.hud.HudPanel;

/**
 * Asks every HUD panel whether it should render, as the HUD does at the start of each frame.
 */
public class HudPanelVisibilityPerfTest extends PerfTest {
    private int visibleCount;

    public HudPanelVisibilityPerfTest() {
        super("hud.should_render_all", "shouldRenderHudPanel() on every HUD panel", 50, 2000);
    }

    @Override
    public void runOnce() {
        int visible = 0;
        for (HudPanel panel : HUDManager.INSTANCE.getWidgets()) {
            if (panel.shouldRenderHudPanel()) {
                visible++;
            }
        }
        visibleCount = visible;
    }
}
