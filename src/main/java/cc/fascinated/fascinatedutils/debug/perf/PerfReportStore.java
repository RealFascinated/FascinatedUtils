package cc.fascinated.fascinatedutils.debug.perf;

import cc.fascinated.fascinatedutils.Constants;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Persists {@link PerfReport}s as JSON under the mod config directory ({@code perf/<label>.json}).
 */
public class PerfReportStore {
    public static final PerfReportStore INSTANCE = new PerfReportStore();

    private static final String EXTENSION = ".json";

    /**
     * Replaces characters that are unsafe in a file name.
     *
     * @param label the requested label
     * @return the label with anything outside letters, digits, dot, dash and underscore replaced by an underscore
     */
    public static String sanitizeLabel(String label) {
        return label.replaceAll("[^A-Za-z0-9_.-]", "_");
    }

    /**
     * Writes a report, replacing any existing report with the same label.
     *
     * @param report the report to save
     * @return the file that was written
     * @throws IOException if the file cannot be written
     */
    public Path save(PerfReport report) throws IOException {
        Path directory = directory();
        Files.createDirectories(directory);
        Path file = directory.resolve(sanitizeLabel(report.label()) + EXTENSION);
        Files.writeString(file, Constants.GSON.toJson(report.toJson()));
        return file;
    }

    /**
     * Loads a saved report.
     *
     * @param label the report label
     * @return the report, or empty when it does not exist or cannot be parsed
     */
    public Optional<PerfReport> load(String label) {
        Path file = directory().resolve(sanitizeLabel(label) + EXTENSION);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(PerfReport.fromJson(Constants.GSON.fromJson(Files.readString(file), JsonObject.class)));
        } catch (IOException | RuntimeException exception) {
            return Optional.empty();
        }
    }

    /**
     * Lists the labels of every saved report.
     *
     * @return the saved labels, sorted alphabetically
     */
    public List<String> listLabels() {
        Path directory = directory();
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(directory)) {
            return files.map(file -> file.getFileName().toString()).filter(name -> name.endsWith(EXTENSION)).map(name -> name.substring(0, name.length() - EXTENSION.length())).sorted().toList();
        } catch (IOException exception) {
            return List.of();
        }
    }

    private Path directory() {
        return ModConfig.getDirectory().resolve("perf");
    }
}
