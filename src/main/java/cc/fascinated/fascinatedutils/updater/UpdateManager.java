package cc.fascinated.fascinatedutils.updater;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Comparator;

public final class UpdateManager {
    private static final Logger LOG = LoggerFactory.getLogger(UpdateManager.class);

    public static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
                // look for any staged update matching the prefix we write
                try {
                    Files.list(modsDir)
                            .filter(p -> p.getFileName().toString().startsWith("FascinatedUtils-update-"))
                            .max(Comparator.comparingLong(p -> p.toFile().lastModified()))
                            .ifPresent(staged -> {
                                System.out.println("FascinatedUtils Updater: found staged update: " + staged);
                                applyStagedUpdate(staged);
                            });
                } catch (Exception ioe) {
                    LOG.debug("No staged updates or failed to list mods directory", ioe);
                }
            } catch (Throwable t) {
                LOG.warn("Unexpected error in update shutdown hook", t);
            }
        }, "FascinatedUtils-Updater"));
    }

    private static void applyStagedUpdate(Path staged) {
        System.out.println("FascinatedUtils Updater: attempting to apply staged update: " + staged);

        if (!Files.exists(staged)) {
            String msg = "Staged file does not exist: " + staged;
            LOG.debug(msg);
            System.err.println(msg);
            return;
        }

        Optional<ModContainer> mod = FabricLoader.getInstance().getModContainer("fascinatedutils");
        if (mod.isEmpty()) {
            String msg = "Mod container not found; cannot apply staged update";
            LOG.debug(msg);
            System.out.println(msg);
            return;
        }

        Optional<Path> currentOpt = mod.get().getOrigin().getPaths().stream().findFirst();
        if (currentOpt.isEmpty()) {
            String msg = "Current mod path unknown; cannot apply staged update";
            LOG.debug(msg);
            System.out.println(msg);
            return;
        }
        Path current = currentOpt.get();

        Path backup = current.resolveSibling(current.getFileName().toString() + ".old");

        try {
            if (Files.exists(current) && Files.isRegularFile(current)) {
                Files.move(current, backup, StandardCopyOption.REPLACE_EXISTING);
                LOG.info("Backed up current mod to {}", backup);
                System.out.println("FascinatedUtils Updater: backed up current mod to " + backup);
            } else {
                LOG.info("Current mod path is not a regular file, skipping backup: {}", current);
                System.out.println("FascinatedUtils Updater: current mod is not a regular file, skipping backup: " + current);
            }
        } catch (Exception ex) {
            String msg = "Failed to back up current mod: " + current + " -> " + ex;
            LOG.warn(msg, ex);
            System.err.println(msg);
            writePendingMarker(current.getParent(), staged, current, "backup_failed: " + ex.getMessage());
        }

        try {
            Files.move(staged, current, StandardCopyOption.REPLACE_EXISTING);
            LOG.info("Replaced mod with staged update: {} -> {}", staged, current);
            System.out.println("FascinatedUtils Updater: replaced mod with staged update: " + staged + " -> " + current);
        } catch (Exception ex) {
            LOG.warn("Atomic move failed, attempting copy fallback", ex);
            System.err.println("FascinatedUtils Updater: atomic move failed, attempting copy fallback: " + ex.getMessage());
            try {
                Files.copy(staged, current, StandardCopyOption.REPLACE_EXISTING);
                LOG.info("Copied staged update into place: {} -> {}", staged, current);
                System.out.println("FascinatedUtils Updater: copied staged update into place: " + staged + " -> " + current);
            } catch (Exception e) {
                String msg = "Failed to apply staged update: " + e.getMessage();
                LOG.error(msg, e);
                System.err.println(msg);
                try {
                    writePendingMarker(current.getParent(), staged, current, "apply_failed: " + e.getMessage());
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static void writePendingMarker(Path modsDir, Path staged, Path current, String reason) {
        try {
            if (modsDir == null) modsDir = staged.getParent();
            String fileName = "FascinatedUtils-update-pending-" + Instant.now().toEpochMilli() + ".txt";
            Path marker = modsDir.resolve(fileName);
            StringBuilder sb = new StringBuilder();
            sb.append("timestamp: ").append(Instant.now().toString()).append(System.lineSeparator());
            sb.append("staged: ").append(staged.toString()).append(System.lineSeparator());
            sb.append("current: ").append(current != null ? current.toString() : "null").append(System.lineSeparator());
            sb.append("reason: ").append(reason).append(System.lineSeparator());
            Files.writeString(marker, sb.toString(), StandardCharsets.UTF_8);
            System.out.println("FascinatedUtils Updater: wrote pending marker: " + marker);
            LOG.info("Wrote pending update marker: {}", marker);
        } catch (IOException ioe) {
            LOG.error("Failed to write pending marker", ioe);
        }
    }
}
