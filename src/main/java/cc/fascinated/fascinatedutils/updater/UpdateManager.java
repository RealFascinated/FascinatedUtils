package cc.fascinated.fascinatedutils.updater;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.common.SystemUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;

public final class UpdateManager {

    public static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
                try (var stream = Files.list(modsDir)) {
                    stream.filter(p -> p.getFileName().toString().startsWith("FascinatedUtils-update-")).max(Comparator.comparingLong(p -> p.toFile().lastModified())).ifPresent(UpdateManager::applyStagedUpdate);
                }
            } catch (Throwable t) {
                Client.LOG.warn("Unexpected error in update shutdown hook", t);
            }
        }, "FascinatedUtils-Updater"));
    }

    private static void applyStagedUpdate(Path staged) {
        if (!Files.exists(staged)) {
            Client.LOG.warn("Staged file does not exist: {}", staged);
            return;
        }

        ModContainer mod = FabricLoader.getInstance().getModContainer("fascinatedutils").orElse(null);
        if (mod == null) {
            Client.LOG.warn("Mod container not found; cannot apply staged update");
            return;
        }

        Path current = mod.getOrigin().getPaths().stream().findFirst().orElse(null);
        if (current == null) {
            Client.LOG.warn("Current mod path unknown; cannot apply staged update");
            return;
        }

        if (SystemUtils.isWindows()) {
            runWindowsUpdater(staged, current);
            return;
        }

        // Back up existing jar
        if (Files.isRegularFile(current)) {
            Path backup = current.resolveSibling(current.getFileName() + ".old");
            try {
                Files.move(current, backup, StandardCopyOption.REPLACE_EXISTING);
                Client.LOG.info("Backed up current mod to {}", backup);
            } catch (Exception e) {
                Client.LOG.warn("Failed to back up current mod: {}", current, e);
            }
        }

        // Apply staged jar
        try {
            Files.move(staged, current, StandardCopyOption.REPLACE_EXISTING);
            Client.LOG.info("Applied staged update: {} -> {}", staged, current);
        } catch (Exception e) {
            Client.LOG.error("Failed to apply staged update", e);
        }
    }

    private static void runWindowsUpdater(Path staged, Path current) {
        // Spawns a detached cmd process that retries the move until Minecraft releases the file lock
        String inner = String.format("for /L %%i in (1,1,600) do ( move /Y \"%s\" \"%s\" >nul 2>&1 && exit /b 0 || timeout /t 2 /nobreak >nul )", staged, current);
        try {
            new ProcessBuilder("cmd", "/c", "start \"\" /min cmd /c \"" + inner + "\"").start();
            Client.LOG.info("Spawned detached update helper: {} -> {}", staged, current);
        } catch (Exception e) {
            Client.LOG.error("Failed to spawn Windows update helper", e);
        }
    }
}