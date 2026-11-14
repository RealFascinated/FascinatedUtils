package cc.fascinated.fascinatedutils.debug.perf.tests;

import cc.fascinated.fascinatedutils.Constants;
import cc.fascinated.fascinatedutils.debug.perf.PerfTest;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import com.google.gson.JsonElement;

/**
 * Serialises one module's settings to JSON, as happens on every config save.
 */
public class ModuleSerializePerfTest extends PerfTest {
    private final Module module;
    private JsonElement lastResult;

    public ModuleSerializePerfTest(Module module) {
        super("module.serialize." + module.getModuleKey(), "Serialise settings of module " + module.getModuleKey(), 50, 2000);
        this.module = module;
    }

    @Override
    public void runOnce() {
        lastResult = module.serialize(Constants.GSON);
    }
}
