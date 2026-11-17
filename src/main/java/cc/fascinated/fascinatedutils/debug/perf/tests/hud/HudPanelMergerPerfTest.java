package cc.fascinated.fascinatedutils.debug.perf.tests.hud;

import cc.fascinated.fascinatedutils.debug.perf.core.PerfTest;
import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.debug.perf.scene.impl.HudOverlaysPerfScene;
import cc.fascinated.fascinatedutils.systems.hud.HudManager;
import cc.fascinated.fascinatedutils.systems.hud.panel.HudPanel;
import cc.fascinated.fascinatedutils.systems.hud.layout.HudPanelMerger;

import java.util.List;

/**
 * HudPanelMerger.update over the HUD panels visible in the HUD overlays scene, with seamless panels on as the default
 * setting has them. The panels keep the positions real frames gave them. Teardown clears the merge state the test
 * left on the panels, so the next frame starts unmerged.
 */
public class HudPanelMergerPerfTest extends PerfTest {
    private final HudPanelMerger panelMerger = new HudPanelMerger();
    private List<HudPanel> panels = List.of();

    public HudPanelMergerPerfTest() {
        super("hud.panel_merger", "HudPanelMerger.update over the visible HUD panels with seamless panels on", 50, 2_000);
    }

    @Override
    public void setup() {
        panels = HudManager.INSTANCE.visibleWidgets();
    }

    @Override
    public void runOnce() {
        panelMerger.update(panels, true);
    }

    @Override
    public void teardown() {
        panelMerger.update(panels, false);
        panels = List.of();
    }

    @Override
    public List<String> sceneIds() {
        return List.of(HudOverlaysPerfScene.ID);
    }

    @Override
    public List<String> covers() {
        return List.of(PerfCoverage.system("hud"));
    }
}
