package cc.fascinated.fascinatedutils.debug.perf.report;

import cc.fascinated.fascinatedutils.Constants;
import cc.fascinated.fascinatedutils.debug.perf.PerfProperties;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Persists {@link PerfReport}s as JSON under the mod config directory ({@code perf/<label>.json}), or under the
 * directory named by {@code -Dalumite.perf.reportDir} so several checkouts can share one report folder.
 */
public class PerfReportStore {
    public static final PerfReportStore INSTANCE = new PerfReportStore();

    private static final String EXTENSION = ".json";

    /**
     * Replaces characters that are unsafe in a file name.
     *
     * @param label the requested label
     * @return the label with anything outside letters, digits, dot, dash and underscore replaced by an underscore; a
     * changed label gets a short hash of the original appended so different labels do not share a file, and an
     * empty label becomes {@code unnamed}
     */
    public static String sanitizeLabel(String label) {
        if (label.isEmpty()) {
            return "unnamed";
        }
        String sanitized = label.replaceAll("[^A-Za-z0-9_.-]", "_");
        return sanitized.equals(label) ? label : sanitized + "_" + String.format("%08x", label.hashCode());
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
     * Loads a saved report. When no report has this exact label but reports named {@code <label>-run1},
     * {@code <label>-run2}, ... exist (several game launches), they are merged into one.
     *
     * @param label the report label
     * @return the report, or empty when it does not exist or cannot be parsed
     */
    public Optional<PerfReport> load(String label) {
        Optional<PerfReport> exact = loadFile(label);
        if (exact.isPresent()) {
            return exact;
        }
        String runPrefix = sanitizeLabel(label) + "-run";
        List<PerfReport> runs = listLabels().stream().filter(name -> name.startsWith(runPrefix) && name.substring(runPrefix.length()).matches("\\d+")).map(this::loadFile).flatMap(Optional::stream).toList();
        if (runs.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new PerfAggregate().merge(label, runs));
    }

    private Optional<PerfReport> loadFile(String label) {
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

    /**
     * Returns the directory that holds saved reports.
     *
     * @return the report directory, from {@code -Dalumite.perf.reportDir} or the mod config folder
     */
    public Path directory() {
        String reportDir = System.getProperty(PerfProperties.REPORT_DIR);
        return reportDir == null || reportDir.isBlank() ? ModConfig.getDirectory().resolve("perf") : Path.of(reportDir);
    }
}
