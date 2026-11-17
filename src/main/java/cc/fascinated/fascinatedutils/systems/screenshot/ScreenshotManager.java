package cc.fascinated.fascinatedutils.systems.screenshot;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.systems.texture.TextureManager;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

public class ScreenshotManager {
    /**
     * Kept sorted newest first on insert, so readers never need to sort.
     */
    private static final List<Screenshot> SCREENSHOTS = new CopyOnWriteArrayList<>();
    private static final List<Screenshot> SCREENSHOTS_VIEW = Collections.unmodifiableList(SCREENSHOTS);

    public static void init() {
        Path screenshotPath = Paths.get(Minecraft.getInstance().gameDirectory.getAbsolutePath(), "screenshots");
        if (!Files.exists(screenshotPath)) {
            return;
        }

        File[] screenshotFiles = screenshotPath.toFile().listFiles();
        if (screenshotFiles == null) {
            return;
        }

        for (File file : screenshotFiles) {
            String lowerName = file.getName().toLowerCase(Locale.ROOT);
            if (!lowerName.endsWith(".png") && !lowerName.endsWith(".jpg") && !lowerName.endsWith(".jpeg")) {
                continue;
            }
            Path filePath = file.toPath();
            try {
                // The constructor reads the file attributes once, and fails for anything that is not a readable file.
                addScreenshot(new Screenshot(filePath));
            } catch (Exception exception) {
                Client.LOG.warn("Failed to load screenshot {}: {}", filePath, exception.toString());
            }
        }
    }

    /**
     * Adds a screenshot, keeping the list ordered newest first.
     *
     * @param screenshot the screenshot to add
     */
    public static synchronized void addScreenshot(Screenshot screenshot) {
        int low = 0;
        int high = SCREENSHOTS.size();
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (SCREENSHOTS.get(middle).getCreatedAt().isBefore(screenshot.getCreatedAt())) {
                high = middle;
            }
            else {
                low = middle + 1;
            }
        }
        SCREENSHOTS.add(low, screenshot);
    }

    public static void removeScreenshot(Screenshot screenshot) {
        SCREENSHOTS.remove(screenshot);
    }

    /**
     * Deletes the screenshot file, drops it from the list and frees its cached texture.
     *
     * @param screenshot the screenshot to delete
     */
    public static void delete(Screenshot screenshot) {
        try {
            Files.deleteIfExists(screenshot.getPath());
        } catch (IOException exception) {
            Client.LOG.warn("Failed to delete screenshot {}: {}", screenshot.getPath(), exception.toString());
            return;
        }
        removeScreenshot(screenshot);
        TextureManager.INSTANCE.invalidateLocal(screenshot.getPath());
    }

    /**
     * Lists the screenshots newest first.
     *
     * @return a live, unmodifiable view; no copy or sort happens per call
     */
    public static List<Screenshot> getScreenshots() {
        return SCREENSHOTS_VIEW;
    }
}
