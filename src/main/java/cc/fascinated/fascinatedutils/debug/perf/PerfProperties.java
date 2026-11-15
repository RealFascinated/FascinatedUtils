package cc.fascinated.fascinatedutils.debug.perf;

/**
 * The JVM system property names that drive the perf harness. Scripts and {@code build.gradle} set these, so the
 * names are part of the harness's external interface.
 *
 * <p>Every property is prefixed with {@link #PREFIX}, for example {@code -Dalumite.perf.run=<label>}.
 */
public class PerfProperties {
    public static final String PREFIX = "alumite.perf.";

    public static final String RUN = PREFIX + "run";
    public static final String WORLD = PREFIX + "world";
    public static final String SCENARIO = PREFIX + "scenario";
    public static final String FILTER = PREFIX + "filter";
    public static final String EXIT = PREFIX + "exit";
    public static final String COMPARE = PREFIX + "compare";
    public static final String COMPARE_REPORTS = PREFIX + "compareReports";
    public static final String COVERAGE = PREFIX + "coverage";
    public static final String REPORT_DIR = PREFIX + "reportDir";
    public static final String WORLD_NAME = PREFIX + "worldName";
    public static final String SEED = PREFIX + "seed";
    public static final String WORLD_TYPE = PREFIX + "worldType";
    public static final String SETTLE_SECONDS = PREFIX + "settleSeconds";
    public static final String MEASURE_SECONDS = PREFIX + "measureSeconds";
    public static final String MODULES = PREFIX + "modules";
    public static final String SCENES = PREFIX + "scenes";
    public static final String SHOTS = PREFIX + "shots";
    public static final String SHOTS_GUI_SCALE = PREFIX + "shots.guiScale";
    public static final String SHOTS_SIZE = PREFIX + "shots.size";
    public static final String ALLOC_PROFILE = PREFIX + "allocProfile";
    public static final String SPARK = PREFIX + "spark";
    public static final String SOAK_MINUTES = PREFIX + "soakMinutes";
    public static final String DEEP = PREFIX + "deep";
}
