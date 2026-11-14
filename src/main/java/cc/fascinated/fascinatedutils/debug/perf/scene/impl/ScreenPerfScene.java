package cc.fascinated.fascinatedutils.debug.perf.scene.impl;

import cc.fascinated.fascinatedutils.debug.perf.scene.PerfScene;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfSceneContext;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;
import java.util.function.Supplier;

/**
 * Keeps one screen open over the world for the whole measurement. One instance per screen, registered in
 * {@code PerfSceneRegistry}; results are named {@code scene.screen.<name>/...}.
 */
public class ScreenPerfScene extends PerfScene {
    private final Supplier<Screen> screenFactory;
    private final Runnable onClose;
    private final List<String> covers;

    public ScreenPerfScene(String name, String description, Supplier<Screen> screenFactory, Runnable onClose, List<String> covers) {
        super("screen." + name, description);
        this.screenFactory = screenFactory;
        this.onClose = onClose;
        this.covers = covers;
    }

    public ScreenPerfScene(String name, String description, Supplier<Screen> screenFactory, List<String> covers) {
        this(name, description, screenFactory, () -> {
        }, covers);
    }

    @Override
    public void setup(PerfSceneContext context) {
        context.keepScreenOpen(screenFactory);
    }

    @Override
    public void teardown(PerfSceneContext context) {
        onClose.run();
    }

    @Override
    public List<String> covers() {
        return covers;
    }
}
