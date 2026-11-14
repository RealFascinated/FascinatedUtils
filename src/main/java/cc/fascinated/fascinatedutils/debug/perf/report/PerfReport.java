package cc.fascinated.fascinatedutils.debug.perf.report;

import cc.fascinated.fascinatedutils.Constants;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A labelled set of results that can be saved and later compared against another report.
 *
 * @param label           the report name, e.g. {@code before}
 * @param timestampMillis when the report was created
 * @param modVersion      the mod version it was measured on
 * @param gameVersion     the Minecraft version it was measured on
 * @param javaVersion     the Java runtime it was measured on
 * @param meta            how it was measured: scenario, CPU count, garbage collector, JVM arguments
 * @param results         the measured results
 */
public record PerfReport(String label, long timestampMillis, String modVersion, String gameVersion, String javaVersion, Map<String, String> meta, List<PerfResult> results) {
    public static final String META_SCENARIO = "scenario";

    /**
     * Builds a report stamped with the current time and runtime details.
     *
     * @param label    the report name
     * @param scenario how the results were gathered, e.g. {@code micro}, {@code live} or {@code world}
     * @param results  the measured results
     * @return the new report
     */
    public static PerfReport create(String label, String scenario, List<PerfResult> results) {
        return create(label, scenario, results, Map.of());
    }

    /**
     * Builds a report stamped with the current time and runtime details, plus extra meta such as GC deltas.
     *
     * @param label     the report name
     * @param scenario  how the results were gathered, e.g. {@code micro}, {@code live} or {@code world:default}
     * @param results   the measured results
     * @param extraMeta extra entries appended to the meta
     * @return the new report
     */
    public static PerfReport create(String label, String scenario, List<PerfResult> results, Map<String, String> extraMeta) {
        Map<String, String> meta = new LinkedHashMap<>();
        meta.put(META_SCENARIO, scenario);
        meta.put("cpus", String.valueOf(Runtime.getRuntime().availableProcessors()));
        meta.put("maxHeapMb", String.valueOf(Runtime.getRuntime().maxMemory() / (1024 * 1024)));
        meta.put("gc", ManagementFactory.getGarbageCollectorMXBeans().stream().map(GarbageCollectorMXBean::getName).collect(Collectors.joining("+")));
        meta.put("jvmArgs", String.join(" ", ManagementFactory.getRuntimeMXBean().getInputArguments()));
        meta.put("os", System.getProperty("os.name") + " " + System.getProperty("os.arch"));
        meta.putAll(extraMeta);
        return new PerfReport(label, System.currentTimeMillis(), Constants.MOD_VERSION, Constants.GAME_VERSION, System.getProperty("java.version"), meta, results);
    }

    /**
     * Reads a report written by {@link #toJson()}.
     *
     * @param json the serialised report
     * @return the parsed report
     */
    public static PerfReport fromJson(JsonObject json) {
        List<PerfResult> results = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray("results")) {
            results.add(PerfResult.fromJson(element.getAsJsonObject()));
        }
        Map<String, String> meta = new LinkedHashMap<>();
        if (json.has("meta")) {
            json.getAsJsonObject("meta").entrySet().forEach(entry -> meta.put(entry.getKey(), entry.getValue().getAsString()));
        }
        return new PerfReport(json.get("label").getAsString(), json.get("timestampMillis").getAsLong(), json.get("modVersion").getAsString(), json.get("gameVersion").getAsString(), json.get("javaVersion").getAsString(), meta, results);
    }

    /**
     * Serialises this report.
     *
     * @return the JSON form
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("label", label);
        json.addProperty("timestampMillis", timestampMillis);
        json.addProperty("modVersion", modVersion);
        json.addProperty("gameVersion", gameVersion);
        json.addProperty("javaVersion", javaVersion);
        JsonObject metaJson = new JsonObject();
        meta.forEach(metaJson::addProperty);
        json.add("meta", metaJson);
        JsonArray resultsJson = new JsonArray();
        for (PerfResult result : results) {
            resultsJson.add(result.toJson());
        }
        json.add("results", resultsJson);
        return json;
    }
}
