package cc.fascinated.fascinatedutils.debug.perf;

import cc.fascinated.fascinatedutils.Constants;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * A labelled set of results that can be saved and later compared against another report.
 *
 * @param label           the report name, e.g. {@code before}
 * @param timestampMillis when the report was created
 * @param modVersion      the mod version it was measured on
 * @param gameVersion     the Minecraft version it was measured on
 * @param javaVersion     the Java runtime it was measured on
 * @param results         the measured results
 */
public record PerfReport(String label, long timestampMillis, String modVersion, String gameVersion, String javaVersion, List<PerfResult> results) {

    /**
     * Builds a report stamped with the current time and runtime versions.
     *
     * @param label   the report name
     * @param results the measured results
     * @return the new report
     */
    public static PerfReport create(String label, List<PerfResult> results) {
        return new PerfReport(label, System.currentTimeMillis(), Constants.MOD_VERSION, Constants.GAME_VERSION, System.getProperty("java.version"), results);
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
        return new PerfReport(json.get("label").getAsString(), json.get("timestampMillis").getAsLong(), json.get("modVersion").getAsString(), json.get("gameVersion").getAsString(), json.get("javaVersion").getAsString(), results);
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
        JsonArray resultsJson = new JsonArray();
        for (PerfResult result : results) {
            resultsJson.add(result.toJson());
        }
        json.add("results", resultsJson);
        return json;
    }
}
