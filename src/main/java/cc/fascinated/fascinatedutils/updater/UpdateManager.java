package cc.fascinated.fascinatedutils.updater;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.common.SystemUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;

public final class UpdateManager {

    public static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
                // look for any staged update matching the prefix we write
                try {
                    Files.list(modsDir).filter(p -> p.getFileName().toString().startsWith("FascinatedUtils-update-")).max(Comparator.comparingLong(p -> p.toFile().lastModified())).ifPresent(staged -> {
                        System.out.println("FascinatedUtils Updater: found staged update: " + staged);
                        applyStagedUpdate(staged);
                    });
                } catch (Exception ioe) {
                    Client.LOG.debug("No staged updates or failed to list mods directory", ioe);
                }
            } catch (Throwable t) {
                Client.LOG.warn("Unexpected error in update shutdown hook", t);
            }
        }, "FascinatedUtils-Updater"));
    }

    private static void applyStagedUpdate(Path staged) {
        System.out.println("FascinatedUtils Updater: attempting to apply staged update: " + staged);

        if (!Files.exists(staged)) {
            String msg = "Staged file does not exist: " + staged;
            Client.LOG.debug(msg);
            System.err.println(msg);
            return;
        }

        Optional<ModContainer> mod = FabricLoader.getInstance().getModContainer("fascinatedutils");
        if (mod.isEmpty()) {
            String msg = "Mod container not found; cannot apply staged update";
            Client.LOG.debug(msg);
            System.out.println(msg);
            return;
        }

        Optional<Path> currentOpt = mod.get().getOrigin().getPaths().stream().findFirst();
        if (currentOpt.isEmpty()) {
            String msg = "Current mod path unknown; cannot apply staged update";
            Client.LOG.debug(msg);
            System.out.println(msg);
            return;
        }

        Path current = currentOpt.get();
        if (SystemUtils.isWindows()) {
            runWindowsUpdater(staged, current);
        }
        else {
            Path backup = current.resolveSibling(current.getFileName().toString() + ".old");
            try {
                if (Files.exists(current) && Files.isRegularFile(current)) {
                    Files.move(current, backup, StandardCopyOption.REPLACE_EXISTING);
                    Client.LOG.info("Backed up current mod to {}", backup);
                    System.out.println("FascinatedUtils Updater: backed up current mod to " + backup);
                }
                else {
                    Client.LOG.info("Current mod path is not a regular file, skipping backup: {}", current);
                    System.out.println("FascinatedUtils Updater: current mod is not a regular file, skipping backup: " + current);
                }
            } catch (Exception ex) {
                String msg = "Failed to back up current mod: " + current + " -> " + ex;
                Client.LOG.warn(msg, ex);
            }

            try {
                Files.move(staged, current, StandardCopyOption.REPLACE_EXISTING);
                Client.LOG.info("Replaced mod with staged update: {} -> {}", staged, current);
                System.out.println("FascinatedUtils Updater: replaced mod with staged update: " + staged + " -> " + current);
            } catch (Exception ex) {
                Client.LOG.warn("Atomic move failed, attempting copy fallback", ex);
                System.err.println("FascinatedUtils Updater: atomic move failed, attempting copy fallback: " + ex.getMessage());
                try {
                    Files.copy(staged, current, StandardCopyOption.REPLACE_EXISTING);
                    Client.LOG.info("Copied staged update into place: {} -> {}", staged, current);
                    System.out.println("FascinatedUtils Updater: copied staged update into place: " + staged + " -> " + current);
                } catch (Exception e) {
                    String msg = "Failed to apply staged update: " + e.getMessage();
                    Client.LOG.error(msg, e);
                }
            }
        }
    }

    private static void runWindowsUpdater(Path staged, Path current) {
        try {
            String stagedPath = staged.toString();
            String currentPath = current.toString();
            String inner = "for /L %i in (1,1,600) do ( move /Y \"" + stagedPath + "\" \"" + currentPath + "\" >nul 2>&1 && exit /b 0 || timeout /t 2 /nobreak >nul )";
            String full = "start \"\" /min cmd /c \"" + inner + "\"";
            new ProcessBuilder("cmd", "/c", full).start();
            Client.LOG.info("Spawned detached update helper (no files): staged={} current={}", stagedPath, currentPath);
            System.out.println("FascinatedUtils Updater: spawned detached helper to apply update (no files): " + stagedPath);
        } catch (Exception ex) {
            Client.LOG.error("Failed to spawn retry helper", ex);
        }
    }
}
