package cc.fascinated.fascinatedutils.debug.perf.scene;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.debug.perf.PerfProperties;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * How a world run is set up: which world, which scenes and how long each is measured. Presets live in
 * {@link PerfSceneRegistry}; every field can be overridden with a JVM property.
 *
 * @param name           the preset name, recorded in the report's {@code scenario} meta
 * @param worldName      the save folder to open or create
 * @param seed           the seed used when the world has to be created
 * @param worldType      the generator used when the world has to be created
 * @param settleTicks    ticks each scene waits before measuring
 * @param measureTicks   ticks each scene is measured for
 * @param moduleMode     whether every module is enabled or the active profile is kept
 * @param sceneIds       the scenes to run, in order
 * @param printSummary   whether to print the per-area coverage summary at the end
 */
public record PerfScenario(String name, String worldName, long seed, WorldType worldType, int settleTicks, int measureTicks, ModuleMode moduleMode, List<String> sceneIds, boolean printSummary) {
    public static final int TICKS_PER_SECOND = 20;

    /**
     * The generator used for a newly created perf world.
     */
    public enum WorldType {
        NORMAL, FLAT
    }

    /**
     * Which modules are enabled during the run.
     */
    public enum ModuleMode {
        /**
         * Every module except the networked ones.
         */
        ALL,
        /**
         * Whatever the active profile has.
         */
        PROFILE
    }

    /**
     * Applies the JVM property overrides on top of this preset.
     *
     * @return the scenario with {@code alumite.perf.worldName}, {@code seed}, {@code worldType}, {@code settleSeconds},
     * {@code measureSeconds}, {@code modules} and {@code scenes} applied where set
     */
    public PerfScenario withPropertyOverrides() {
        long seedOverride = Long.getLong(PerfProperties.SEED, seed);
        WorldType worldTypeOverride = parseEnum(WorldType.class, PerfProperties.WORLD_TYPE, worldType);
        // A flat run must not reuse the normal world's save, so it gets its own folder unless one was named.
        String defaultWorldName = worldTypeOverride == worldType ? worldName : worldName + "_" + worldTypeOverride.name().toLowerCase(Locale.ROOT);
        String worldNameOverride = System.getProperty(PerfProperties.WORLD_NAME, defaultWorldName);
        int settleOverride = parseSeconds(PerfProperties.SETTLE_SECONDS, settleTicks);
        int measureOverride = parseSeconds(PerfProperties.MEASURE_SECONDS, measureTicks);
        ModuleMode moduleOverride = parseEnum(ModuleMode.class, PerfProperties.MODULES, moduleMode);
        String scenes = System.getProperty(PerfProperties.SCENES);
        List<String> scenesOverride = scenes == null || scenes.isBlank() ? sceneIds : Arrays.stream(scenes.split(",")).map(String::trim).filter(scene -> !scene.isEmpty()).toList();
        return new PerfScenario(name, worldNameOverride, seedOverride, worldTypeOverride, settleOverride, measureOverride, moduleOverride, scenesOverride, printSummary);
    }

    private static int parseSeconds(String property, int defaultTicks) {
        String value = System.getProperty(property);
        if (value == null) {
            return defaultTicks;
        }
        try {
            double seconds = Double.parseDouble(value.trim());
            if (Double.isFinite(seconds) && seconds >= 0) {
                return (int) Math.round(seconds * TICKS_PER_SECOND);
            }
        } catch (NumberFormatException exception) {
            // Falls through to the warning below.
        }
        Client.LOG.warn("[perf] ignoring -D{}={}: expected a non-negative number of seconds, using {} ticks", property, value, defaultTicks);
        return defaultTicks;
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String property, E defaultValue) {
        String value = System.getProperty(property);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            Client.LOG.warn("[perf] ignoring -D{}={}: expected one of {}, using {}", property, value, Arrays.toString(type.getEnumConstants()), defaultValue);
            return defaultValue;
        }
    }

    /**
     * The same settings under another name, used for a custom scenario name that has no preset.
     *
     * @param newName the name to record
     * @return the renamed scenario
     */
    public PerfScenario withName(String newName) {
        return new PerfScenario(newName, worldName, seed, worldType, settleTicks, measureTicks, moduleMode, sceneIds, printSummary);
    }
}
