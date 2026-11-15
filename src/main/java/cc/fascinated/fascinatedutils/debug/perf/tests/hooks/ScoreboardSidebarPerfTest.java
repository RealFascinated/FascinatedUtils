package cc.fascinated.fascinatedutils.debug.perf.tests.hooks;

import cc.fascinated.fascinatedutils.debug.perf.core.PerfTest;
import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.scoreboard.ScoreboardModule;

import java.util.List;

/**
 * The per-frame check that the scoreboard GuiMixin runs before the vanilla sidebar is drawn: whether the scoreboard
 * module is enabled, which cancels the vanilla draw.
 */
public class ScoreboardSidebarPerfTest extends PerfTest {
    private boolean lastHidesVanilla;

    public ScoreboardSidebarPerfTest() {
        super("hook.scoreboard.sidebar", "ModuleRegistry.isEnabled(ScoreboardModule) as checked by the vanilla sidebar hook", 50, 20_000);
    }

    @Override
    public void runOnce() {
        lastHidesVanilla = ModuleRegistry.INSTANCE.isEnabled(ScoreboardModule.class);
    }

    @Override
    public List<String> covers() {
        return List.of(PerfCoverage.mixin("scoreboard.GuiMixin"), PerfCoverage.module(ScoreboardModule.class));
    }
}
