package cc.fascinated.fascinatedutils.updater;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

public class UpdateManager {
    public static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
                Path staged = modsDir.resolve("FascinatedUtils-update.jar");
                if (!Files.exists(staged)) return;

                Optional<ModContainer> mod = FabricLoader.getInstance().getModContainer("fascinatedutils");
                if (mod.isEmpty()) return;

                Optional<Path> currentOpt = mod.get().getOrigin().getPaths().stream().findFirst();
                if (currentOpt.isEmpty()) return;
                Path current = currentOpt.get();

                Path backup = current.resolveSibling(current.getFileName().toString() + ".old");

                try {
                    Files.move(current, backup, StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception ex) {
                    // best-effort: ignore failures to rename
                }

                try {
                    Files.move(staged, current, StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception ex) {
                    // if we can't move staged into place, attempt a non-atomic fallback
                    try { Files.copy(staged, current, StandardCopyOption.REPLACE_EXISTING); } catch (Exception e) { }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "FascinatedUtils-Updater"));
    }
}
