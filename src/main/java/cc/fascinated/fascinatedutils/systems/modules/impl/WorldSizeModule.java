package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.common.ByteFormatterUtil;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleCategory;
import cc.fascinated.fascinatedutils.systems.modules.ModuleDefaults;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class WorldSizeModule extends Module {

    private final Map<String, String> worldSizes = new ConcurrentHashMap<>();

    public WorldSizeModule() {
        super("World Size", ModuleCategory.GENERAL, ModuleDefaults.builder().defaultState(true).build());
    }

    /**
     * Returns the cached formatted size for the given level ID, if available.
     *
     * @param levelId the save folder name
     * @return the formatted size string, or empty if not yet computed
     */
    public Optional<String> getFormattedSize(String levelId) {
        return Optional.ofNullable(worldSizes.get(levelId));
    }

    /**
     * Triggers an asynchronous refresh of all world sizes from the saves directory.
     * Does nothing if the module is disabled.
     */
    public void refreshSizes() {
        if (!isEnabled()) {
            return;
        }
        Path savesDir = Minecraft.getInstance().getLevelSource().getBaseDir();
        Thread.ofVirtual().start(() -> {
            Map<String, String> newSizes = new HashMap<>();
            try (Stream<Path> dirs = Files.list(savesDir)) {
                dirs.filter(Files::isDirectory).forEach(dir -> {
                    String levelId = dir.getFileName().toString();
                    try {
                        long bytes = computeSize(dir);
                        newSizes.put(levelId, ByteFormatterUtil.formatBytes(bytes, 1));
                    } catch (IOException ignored) {
                    }
                });
            } catch (IOException ignored) {
            }
            worldSizes.clear();
            worldSizes.putAll(newSizes);
        });
    }

    private long computeSize(Path dir) throws IOException {
        try (Stream<Path> stream = Files.walk(dir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .mapToLong(path -> path.toFile().length())
                    .sum();
        }
    }
}
