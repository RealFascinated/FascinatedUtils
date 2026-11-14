package cc.fascinated.fascinatedutils.debug.perf;

/**
 * Reads the JVM properties that put the client into a scripted perf run.
 */
public class PerfMode {
    public static final String PROPERTY_PREFIX = "alumite.perf.";

    /**
     * Reports whether the in-world scenario was requested. While it runs the mod does not write its config on exit, so
     * the scenario cannot overwrite the developer's real profile.
     *
     * @return true when {@code alumite.perf.world} or {@code alumite.perf.scenario} is set
     */
    public static boolean isWorldScenario() {
        return System.getProperty(PROPERTY_PREFIX + "world") != null || System.getProperty(PROPERTY_PREFIX + "scenario") != null;
    }
}
