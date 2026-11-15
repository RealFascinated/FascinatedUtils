package cc.fascinated.fascinatedutils.debug.perf;

/**
 * The id strings shared by the world runner's result names and the coverage summary. Changing one here changes both
 * sides, so the summary keeps finding the results the runner wrote.
 */
public class PerfIds {
    public static final String SCENE_RESULT_PREFIX = "scene.";

    private static final String TEST_COVERER_PREFIX = "test ";
    private static final String SCENE_COVERER_PREFIX = "scene ";

    /**
     * The prefix of every result a scene's measurement or world tests produce.
     *
     * @param sceneId the scene id
     * @return {@code scene.<id>/}
     */
    public static String sceneResultPrefix(String sceneId) {
        return SCENE_RESULT_PREFIX + sceneId + "/";
    }

    /**
     * The label a world test uses for itself in the coverage summary.
     *
     * @param testId the test id
     * @return {@code test <id>}
     */
    public static String testCoverer(String testId) {
        return TEST_COVERER_PREFIX + testId;
    }

    /**
     * The label a scene uses for itself in the coverage summary.
     *
     * @param sceneId the scene id
     * @return {@code scene <id>}
     */
    public static String sceneCoverer(String sceneId) {
        return SCENE_COVERER_PREFIX + sceneId;
    }

    /**
     * Reports whether a coverage label names a world test.
     *
     * @param coverer the label
     * @return true for labels made by {@link #testCoverer}
     */
    public static boolean isTestCoverer(String coverer) {
        return coverer.startsWith(TEST_COVERER_PREFIX);
    }
}
