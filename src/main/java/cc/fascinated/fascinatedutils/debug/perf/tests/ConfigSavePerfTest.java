package cc.fascinated.fascinatedutils.debug.perf.tests;

import cc.fascinated.fascinatedutils.debug.perf.PerfTest;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;

/**
 * Writes the global config and the active profile to disk, rewriting the same content they already hold.
 */
public class ConfigSavePerfTest extends PerfTest {

    public ConfigSavePerfTest() {
        super("config.save_all", "Save global config and active profile to disk", 3, 30);
    }

    @Override
    public void runOnce() {
        ModConfig.config().save();
        ModConfig.profiles().saveActiveProfile();
    }
}
