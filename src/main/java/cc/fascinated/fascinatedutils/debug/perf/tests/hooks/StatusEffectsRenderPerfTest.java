package cc.fascinated.fascinatedutils.debug.perf.tests.hooks;

import cc.fascinated.fascinatedutils.debug.perf.core.PerfTest;
import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.statuseffects.StatusEffectsModule;

import java.util.List;

/**
 * The per-frame check that the status effects GuiMixin runs before the vanilla effect HUD is drawn: whether the
 * status effects module is enabled, which cancels the vanilla draw.
 */
public class StatusEffectsRenderPerfTest extends PerfTest {
    private boolean lastHidesVanilla;

    public StatusEffectsRenderPerfTest() {
        super("hook.statuseffects.render", "ModuleRegistry.isEnabled(StatusEffectsModule) as checked by the vanilla status effect hook", 50, 20_000);
    }

    @Override
    public void runOnce() {
        lastHidesVanilla = ModuleRegistry.INSTANCE.isEnabled(StatusEffectsModule.class);
    }

    @Override
    public List<String> covers() {
        return List.of(PerfCoverage.mixin("statuseffects.GuiMixin"), PerfCoverage.module(StatusEffectsModule.class));
    }
}
