package cc.fascinated.fascinatedutils.debug.perf.tests.ui;

import cc.fascinated.fascinatedutils.debug.perf.core.PerfTest;
import cc.fascinated.fascinatedutils.gui.screens.RootScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.state.gui.GuiRenderState;

import java.util.List;
import java.util.function.Supplier;

/**
 * Cost of opening one mod screen: constructing it, initialising it at the window's GUI scale and rendering its first
 * frame offscreen. The new screen is removed again after every call, so the scene's open screen is never touched.
 */
public class ScreenOpenPerfTest extends PerfTest {
    private final String sceneId;
    private final Supplier<Screen> screenFactory;
    private final GuiRenderState renderState = new GuiRenderState();

    public ScreenOpenPerfTest(String screenName, Supplier<Screen> screenFactory) {
        super("ui.screen_open." + screenName, "Construct, init and first offscreen frame of the " + screenName + " screen", 3, 30);
        this.sceneId = "screen." + screenName;
        this.screenFactory = screenFactory;
    }

    @Override
    public void runOnce() {
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = screenFactory.get();
        screen.init(minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
        if (screen instanceof RootScreen rootScreen) {
            renderState.reset();
            rootScreen.renderCustom(new GuiGraphicsExtractor(minecraft, renderState, 0, 0), 0, 0, 1f);
        }
        screen.removed();
    }

    @Override
    public void teardown() {
        renderState.reset();
    }

    @Override
    public List<String> sceneIds() {
        return List.of(sceneId);
    }
}
