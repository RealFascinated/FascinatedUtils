package cc.fascinated.fascinatedutils.debug.perf.tests.hud;

import cc.fascinated.fascinatedutils.debug.perf.core.PerfTest;
import cc.fascinated.fascinatedutils.debug.perf.scene.impl.HudOverlaysPerfScene;
import cc.fascinated.fascinatedutils.debug.perf.scene.impl.IdlePerfScene;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
import cc.fascinated.fascinatedutils.systems.hud.HUDManager;
import cc.fascinated.fascinatedutils.systems.hud.HudLayoutCanvas;
import cc.fascinated.fascinatedutils.systems.hud.HudPanel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;

import java.util.List;

/**
 * One HUD panel's prepare and draw, exactly as {@link HUDManager} runs it each frame, into an offscreen render state.
 * One instance is registered per panel so each panel's time and allocation are reported on their own.
 */
public class HudPanelPerfTest extends PerfTest {
    private static final float DELTA_SECONDS = 1f / 20f;

    private final String panelId;
    private final GuiRenderState renderState = new GuiRenderState();
    private AlumiteGuiRenderer renderer;
    private HudPanel panel;
    private int drawn;

    public HudPanelPerfTest(String panelId) {
        super("hud.panel." + panelId, "Prepare and draw of the " + panelId + " HUD panel into an offscreen GuiRenderState", 50, 2_000);
        this.panelId = panelId;
    }

    @Override
    public void setup() {
        panel = HUDManager.INSTANCE.getWidgets().stream().filter(widget -> widget.getId().equals(panelId)).findFirst().orElse(null);
        renderer = new AlumiteGuiRenderer(new GuiGraphicsExtractor(Minecraft.getInstance(), renderState, 0, 0));
    }

    @Override
    public void runOnce() {
        if (panel == null) {
            return;
        }
        renderState.reset();
        renderer.beginFrame(HudLayoutCanvas.width(), HudLayoutCanvas.height());
        Runnable draw = panel.prepareAndDraw(renderer, DELTA_SECONDS, false);
        if (draw != null) {
            draw.run();
            renderer.endRenderSegment();
            drawn++;
        }
        renderer.endFrame();
    }

    @Override
    public void teardown() {
        renderState.reset();
        renderer = null;
        panel = null;
    }

    @Override
    public List<String> sceneIds() {
        return List.of(IdlePerfScene.ID, HudOverlaysPerfScene.ID, "big_scoreboard");
    }
}
