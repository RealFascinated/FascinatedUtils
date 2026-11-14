package cc.fascinated.fascinatedutils.debug.perf.run;

import cc.fascinated.fascinatedutils.client.Client;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.InactivityFpsLimit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Puts the client in a state where frame times reflect the work done, not a limiter: VSync off, no frame rate cap,
 * no inactivity throttling and no pause when the window loses focus. The player's {@code options.txt} is restored
 * when the JVM exits, so a perf run never changes their settings.
 */
public class PerfClientOptions {
    private static final int UNLIMITED_FRAMERATE = 260;

    private boolean restoreHookInstalled;

    /**
     * Applies the benchmark settings. Safe to call more than once.
     *
     * @param minecraft the running client
     */
    public void apply(Minecraft minecraft) {
        installRestoreHook();
        Options options = minecraft.options;
        options.enableVsync().set(false);
        options.framerateLimit().set(UNLIMITED_FRAMERATE);
        options.inactivityFpsLimit().set(InactivityFpsLimit.MINIMIZED);
        options.pauseOnLostFocus = false;
    }

    /**
     * Snapshots {@code options.txt} now and writes it back at JVM shutdown, after the game's own final save.
     */
    private void installRestoreHook() {
        if (restoreHookInstalled) {
            return;
        }
        restoreHookInstalled = true;
        Path optionsFile = FabricLoader.getInstance().getGameDir().resolve("options.txt");
        byte[] original;
        try {
            original = Files.exists(optionsFile) ? Files.readAllBytes(optionsFile) : null;
        } catch (IOException exception) {
            Client.LOG.warn("Perf run could not snapshot options.txt: {}", exception.toString());
            return;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (original == null) {
                    Files.deleteIfExists(optionsFile);
                } else {
                    Files.write(optionsFile, original);
                }
            } catch (IOException exception) {
                System.err.println("Perf run could not restore options.txt: " + exception);
            }
        }, "alumite-perf-options-restore"));
    }
}
