package cc.fascinated.fascinatedutils.debug.perf.core;

import cc.fascinated.fascinatedutils.debug.perf.PerfIds;
import cc.fascinated.fascinatedutils.debug.perf.PerfOut;
import cc.fascinated.fascinatedutils.debug.perf.PerfProperties;
import cc.fascinated.fascinatedutils.debug.perf.report.PerfReportStore;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Runs Spark's client profiler over one scene's measure window, then moves the profile Spark saved into the perf report
 * folder. Only active when {@code -Dalumite.perf.spark=cpu|alloc} is set, so a normal run sends no Spark command.
 *
 * <p>Commands go through Fabric's client command dispatcher as {@code sparkc profiler ...}. Stopping always passes
 * {@code --save-to-file}, so Spark writes the profile to its local folder and never uploads it.
 */
public class PerfSparkProfiler {
    public static final PerfSparkProfiler INSTANCE = new PerfSparkProfiler();

    private static final String CPU_MODE = "cpu";
    private static final String ALLOC_MODE = "alloc";
    private static final String SPARK_PROFILER_COMMAND = "sparkc profiler";
    private static final String SPARK_CONFIG_DIRECTORY = "spark";
    private static final String PROFILE_EXTENSION = ".sparkprofile";
    private static final String DESTINATION_DIRECTORY = "spark";
    private static final long FILE_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(10);

    private String activeSceneId;
    private Path activeDestination;
    private String pendingSceneId;
    private Path pendingDestination;
    private Path pendingDirectory;
    private Set<String> pendingExistingNames = Set.of();
    private long pendingDeadlineNanos;
    private long pendingLastSize;

    /**
     * Reads the Spark profile type requested with {@code -Dalumite.perf.spark}.
     *
     * @return {@code cpu} or {@code alloc}, or null when the property is unset or has another value
     */
    public String requestedMode() {
        String value = System.getProperty(PerfProperties.SPARK);
        if (value == null || value.isBlank()) {
            return null;
        }
        String mode = value.trim().toLowerCase(Locale.ROOT);
        if (mode.equals(CPU_MODE) || mode.equals(ALLOC_MODE)) {
            return mode;
        }
        PerfOut.INSTANCE.printAndLog("FAILED spark: " + PerfProperties.SPARK + " must be cpu or alloc, not " + value);
        return null;
    }

    /**
     * Starts Spark's sampler at the start of a measure window. A failed command is printed and the scene runs
     * without a profile.
     *
     * @param minecraft the client
     * @param label     the run label, which names the destination folder
     * @param sceneId   the scene being measured
     * @param mode      {@code cpu} or {@code alloc}
     */
    public void start(Minecraft minecraft, String label, String sceneId, String mode) {
        String arguments = mode.equals(ALLOC_MODE) ? "--alloc --thread *" : "--thread * --interval 2";
        if (!execute(minecraft, SPARK_PROFILER_COMMAND + " start " + arguments)) {
            return;
        }
        activeSceneId = sceneId;
        activeDestination = PerfReportStore.INSTANCE.directory()
                .resolve(DESTINATION_DIRECTORY)
                .resolve(PerfReportStore.sanitizeLabel(label))
                .resolve(sceneId + "-" + mode + PROFILE_EXTENSION);
    }

    /**
     * Stops the running sampler at the end of a measure window and arms the move of the profile Spark writes. Does
     * nothing when no sampler was started. The file is moved by {@link #poll()} on later ticks.
     *
     * @param minecraft the client
     */
    public void stop(Minecraft minecraft) {
        if (activeSceneId == null) {
            return;
        }
        String sceneId = activeSceneId;
        Path destination = activeDestination;
        activeSceneId = null;
        activeDestination = null;
        Path directory = sparkDirectory();
        Set<String> existingNames = profileNames(directory);
        if (!execute(minecraft, SPARK_PROFILER_COMMAND + " stop --save-to-file --comment " + sceneId)) {
            return;
        }
        if (pendingDestination != null) {
            abandonPending("the previous profile had not been moved yet");
        }
        pendingSceneId = sceneId;
        pendingDestination = destination;
        pendingDirectory = directory;
        pendingExistingNames = existingNames;
        pendingDeadlineNanos = System.nanoTime() + FILE_TIMEOUT_NANOS;
        pendingLastSize = -1;
    }

    /**
     * Discards a sampler that is still running, for example after a failed scene. The results are not saved.
     *
     * @param minecraft the client
     */
    public void cancel(Minecraft minecraft) {
        if (activeSceneId == null) {
            return;
        }
        activeSceneId = null;
        activeDestination = null;
        execute(minecraft, SPARK_PROFILER_COMMAND + " cancel");
    }

    /**
     * Moves the profile Spark saved after {@link #stop(Minecraft)} into the perf folder. Called every client tick. A
     * new file is moved once its size has stopped changing; after ten seconds without one, the move is abandoned and
     * a FAILED line is printed.
     */
    public void poll() {
        if (pendingDestination == null) {
            return;
        }
        Optional<Path> saved = newProfile();
        if (saved.isEmpty()) {
            if (System.nanoTime() > pendingDeadlineNanos) {
                abandonPending("no profile appeared in " + pendingDirectory);
            }
            return;
        }
        long size = sizeOf(saved.get());
        if (size <= 0 || size != pendingLastSize) {
            pendingLastSize = size;
            return;
        }
        try {
            Files.createDirectories(pendingDestination.getParent());
            Files.move(saved.get(), pendingDestination, StandardCopyOption.REPLACE_EXISTING);
            PerfOut.INSTANCE.printAndLog("spark " + PerfIds.SCENE_RESULT_PREFIX + pendingSceneId + " " + pendingDestination);
            clearPending();
        } catch (IOException exception) {
            abandonPending("could not move " + saved.get() + ": " + exception);
        }
    }

    private boolean execute(Minecraft minecraft, String command) {
        CommandDispatcher<FabricClientCommandSource> dispatcher = ClientCommands.getActiveDispatcher();
        ClientPacketListener connection = minecraft.getConnection();
        if (dispatcher == null || connection == null) {
            PerfOut.INSTANCE.printAndLog("FAILED spark: no client command dispatcher for " + command);
            return false;
        }
        if (!(connection.getSuggestionsProvider() instanceof FabricClientCommandSource source)) {
            PerfOut.INSTANCE.printAndLog("FAILED spark: the client command source is not a Fabric command source");
            return false;
        }
        try {
            dispatcher.execute(command, source);
            return true;
        } catch (CommandSyntaxException | RuntimeException exception) {
            PerfOut.INSTANCE.printAndLog("FAILED spark: " + command + ": " + exception.getMessage());
            return false;
        }
    }

    private Optional<Path> newProfile() {
        return profileNames(pendingDirectory).stream()
                .filter(name -> !pendingExistingNames.contains(name))
                .sorted()
                .findFirst()
                .map(pendingDirectory::resolve);
    }

    private void abandonPending(String reason) {
        PerfOut.INSTANCE.printAndLog("FAILED spark: " + reason);
        clearPending();
    }

    private void clearPending() {
        pendingSceneId = null;
        pendingDestination = null;
        pendingDirectory = null;
        pendingExistingNames = Set.of();
        pendingDeadlineNanos = 0;
        pendingLastSize = -1;
    }

    private static Path sparkDirectory() {
        return FabricLoader.getInstance().getConfigDir().resolve(SPARK_CONFIG_DIRECTORY);
    }

    private static Set<String> profileNames(Path directory) {
        if (!Files.isDirectory(directory)) {
            return Set.of();
        }
        try (Stream<Path> files = Files.list(directory)) {
            return files.map(file -> file.getFileName().toString())
                    .filter(name -> name.endsWith(PROFILE_EXTENSION))
                    .collect(Collectors.toUnmodifiableSet());
        } catch (IOException exception) {
            return Set.of();
        }
    }

    private static long sizeOf(Path file) {
        try {
            return Files.size(file);
        } catch (IOException exception) {
            return -1;
        }
    }
}
