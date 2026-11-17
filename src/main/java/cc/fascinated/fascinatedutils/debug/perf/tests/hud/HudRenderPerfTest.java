package cc.fascinated.fascinatedutils.debug.perf.tests.hud;

import cc.fascinated.fascinatedutils.debug.perf.core.PerfTest;
import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.debug.perf.scene.impl.HudOverlaysPerfScene;
import cc.fascinated.fascinatedutils.debug.perf.scene.impl.IdlePerfScene;
import cc.fascinated.fascinatedutils.systems.hud.HudModuleWidgetsElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;

import java.util.List;

/**
 * The mod's whole HUD element as the game calls it each frame (every visible panel plus toasts), extracted into an
 * offscreen render state so nothing reaches the screen. Runs in the idle and overlay scenes so both a light and a
 * crowded HUD are measured.
 */
public class HudRenderPerfTest extends PerfTest {
    private final GuiRenderState renderState = new GuiRenderState();

    public HudRenderPerfTest() {
        super("hud.render_full", "HudModuleWidgetsElement.extractRenderState into an offscreen GuiRenderState", 50, 2_000);
    }

    @Override
    public void runOnce() {
        renderState.reset();
        GuiGraphicsExtractor graphics = new GuiGraphicsExtractor(Minecraft.getInstance(), renderState, 0, 0);
        HudModuleWidgetsElement.INSTANCE.extractRenderState(graphics, DeltaTracker.ONE);
    }

    @Override
    public void teardown() {
        renderState.reset();
    }

    @Override
    public List<String> sceneIds() {
        return List.of(IdlePerfScene.ID, HudOverlaysPerfScene.ID);
    }

    @Override
    public List<String> covers() {
        return List.of(PerfCoverage.system("hud"), PerfCoverage.gui("render"), PerfCoverage.gui("toast"), PerfCoverage.gui("theme"), PerfCoverage.renderer("Renderer2D"), PerfCoverage.renderer("MeshBuilder"), PerfCoverage.renderer("text"));
    }
}
