package cc.fascinated.fascinatedutils.debug.perf.tests.module;

import cc.fascinated.fascinatedutils.debug.perf.core.PerfTest;
import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.fullbright.FullbrightModule;

import java.util.List;

/**
 * Flips a module off and on again, so the enabled-state event reaches ModuleRegistry's listener twice. The module
 * ends every call in the state it started in.
 */
public class ModuleTogglePerfTest extends PerfTest {
    private FullbrightModule module;
    private boolean originalEnabled;

    public ModuleTogglePerfTest() {
        super("module.toggle", "Module.setEnabled twice (event plus ModuleRegistry enabled-list update)", 50, 20_000);
    }

    @Override
    public void setup() {
        module = ModuleRegistry.INSTANCE.getModule(FullbrightModule.class).orElseThrow();
        originalEnabled = module.isEnabled();
    }

    @Override
    public void runOnce() {
        boolean enabled = module.isEnabled();
        try {
            module.setEnabled(!enabled);
        } finally {
            module.setEnabled(enabled);
        }
    }

    @Override
    public void teardown() {
        if (module != null && module.isEnabled() != originalEnabled) {
            module.setEnabled(originalEnabled);
        }
    }

    @Override
    public List<String> covers() {
        return List.of(PerfCoverage.listener(ModuleRegistry.class), PerfCoverage.system("modules"));
    }
}
