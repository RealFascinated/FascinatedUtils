package cc.fascinated.fascinatedutils.debug.perf.tests.hud;

import cc.fascinated.fascinatedutils.debug.perf.core.PerfTest;
import cc.fascinated.fascinatedutils.debug.perf.scene.impl.HudOverlaysPerfScene;
import cc.fascinated.fascinatedutils.debug.perf.scene.impl.IdlePerfScene;
import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.gui.core.overlay.UiOverlay;
import cc.fascinated.fascinatedutils.gui.core.state.UiStateStore;
import cc.fascinated.fascinatedutils.systems.hud.HudManager;
import cc.fascinated.fascinatedutils.systems.hud.layout.HudLayoutCanvas;
import cc.fascinated.fascinatedutils.systems.hud.panel.HudPanel;
import cc.fascinated.fascinatedutils.systems.hud.layout.HudPanelMerger;
import cc.fascinated.fascinatedutils.systems.hud.node.HudLayerNode;
import cc.fascinated.fascinatedutils.systems.hud.node.HudPanelNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;

import java.util.List;

/**
 * One HUD panel's compose, layout and draw, exactly as {@link HudManager} runs it each frame (a one-panel HUD overlay
 * recomposed every frame), into an offscreen render state.
 * One instance is registered per panel so each panel's time and allocation are reported on their own.
 */
public class HudPanelPerfTest extends PerfTest {
    private static final float DELTA_SECONDS = 1f / 20f;

    private final String panelId;
    private final GuiRenderState renderState = new GuiRenderState();
    private final HudPanelMerger panelMerger = new HudPanelMerger();
    private GuiGraphicsExtractor graphics;
    private UiOverlay overlay;
    private HudPanel panel;
    private int drawn;

    public HudPanelPerfTest(String panelId) {
        super("hud.panel." + panelId, "Prepare and draw of the " + panelId + " HUD panel into an offscreen GuiRenderState", 50, 2_000);
        this.panelId = panelId;
    }

    @Override
    public void setup() {
        panel = HudManager.INSTANCE.getWidgets().stream().filter(widget -> widget.getId().equals(panelId)).findFirst().orElse(null);
        graphics = new GuiGraphicsExtractor(Minecraft.getInstance(), renderState, 0, 0);
        overlay = new UiOverlay(this::composePanel).recomposeEveryFrame(true);
    }

    private UiNode composePanel(UiStateStore stateStore) {
        HudLayerNode layer = new HudLayerNode(panelMerger);
        HudPanelNode panelNode = panel.composeNode(DELTA_SECONDS, false, true);
        if (panelNode != null) {
            layer.addChild(panelNode);
            drawn++;
        }
        return layer;
    }

    @Override
    public void runOnce() {
        if (panel == null) {
            return;
        }
        renderState.reset();
        overlay.render(graphics, HudLayoutCanvas.width(), HudLayoutCanvas.height(), 0f, 0f, DELTA_SECONDS);
    }

    @Override
    public void teardown() {
        renderState.reset();
        overlay.dispose();
        overlay = null;
        graphics = null;
        panel = null;
    }

    @Override
    public List<String> sceneIds() {
        return List.of(IdlePerfScene.ID, HudOverlaysPerfScene.ID, "big_scoreboard");
    }
}
