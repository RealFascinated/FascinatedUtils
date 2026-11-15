package cc.fascinated.fascinatedutils.debug.perf.tests.ui;

import cc.fascinated.fascinatedutils.debug.perf.core.PerfTest;
import cc.fascinated.fascinatedutils.gui.screens.RootScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;

import java.util.List;

/**
 * One frame of the open mod screen (compose when due, layout, pointer hover and render), extracted into an offscreen
 * render state. Runs in every mod screen scene, so each screen reports its own frame cost and allocation.
 */
public class ScreenFramePerfTest extends PerfTest {
    private final GuiRenderState renderState = new GuiRenderState();
    private final List<String> screenScenes;
    private RootScreen screen;

    public ScreenFramePerfTest(List<String> screenScenes) {
        super("ui.screen_frame", "RootScreen.renderCustom of the open mod screen into an offscreen GuiRenderState", 50, 1_000);
        this.screenScenes = screenScenes;
    }

    @Override
    public void setup() {
        screen = Minecraft.getInstance().screen instanceof RootScreen rootScreen ? rootScreen : null;
    }

    @Override
    public void runOnce() {
        if (screen == null) {
            return;
        }
        renderState.reset();
        screen.renderCustom(new GuiGraphicsExtractor(Minecraft.getInstance(), renderState, 0, 0), 0, 0, 1f);
    }

    @Override
    public void teardown() {
        renderState.reset();
        screen = null;
    }

    @Override
    public List<String> sceneIds() {
        return screenScenes;
    }
}
