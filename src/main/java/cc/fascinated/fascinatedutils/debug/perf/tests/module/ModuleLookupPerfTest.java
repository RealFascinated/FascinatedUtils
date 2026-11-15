package cc.fascinated.fascinatedutils.debug.perf.tests.module;

import cc.fascinated.fascinatedutils.debug.perf.core.PerfTest;
import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.BlockOutlineModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.BlurModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.FogCustomizerModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.FreelookModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.TabModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.music.MusicModule;

import java.util.List;
import java.util.Optional;

/**
 * Ten ModuleRegistry lookups shaped like the per-frame mixin hooks: getModule followed by an enabled check, plus
 * isEnabled by class.
 */
public class ModuleLookupPerfTest extends PerfTest {
    private static final List<Class<? extends Module>> MODULE_CLASSES = List.of(BlurModule.class, MusicModule.class, TabModule.class, FreelookModule.class, BlockOutlineModule.class, FogCustomizerModule.class);

    private int sink;

    public ModuleLookupPerfTest() {
        super("modules.lookup", "Ten ModuleRegistry.getModule / isEnabled lookups as the per-frame hooks run them", 2_000, 200_000);
    }

    @Override
    public void runOnce() {
        ModuleRegistry registry = ModuleRegistry.INSTANCE;
        for (int index = 0; index < 5; index++) {
            Optional<? extends Module> module = registry.getModule(MODULE_CLASSES.get(index));
            if (module.isPresent() && module.get().isEnabled()) {
                sink++;
            }
            if (registry.isEnabled(MODULE_CLASSES.get(index + 1))) {
                sink++;
            }
        }
    }

    @Override
    public List<String> covers() {
        return List.of(PerfCoverage.system("modules"));
    }
}
