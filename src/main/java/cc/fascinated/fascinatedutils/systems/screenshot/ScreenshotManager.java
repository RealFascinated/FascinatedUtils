package cc.fascinated.fascinatedutils.systems.screenshot;

import cc.fascinated.fascinatedutils.client.Client;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ScreenshotManager {
    private static final List<Screenshot> SCREENSHOTS = new ArrayList<>();

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
            Path filePath = file.toPath();
            if (!Files.isRegularFile(filePath)) {
                continue;
            }
            String lowerName = file.getName().toLowerCase(Locale.ROOT);
            if (!lowerName.endsWith(".png") && !lowerName.endsWith(".jpg") && !lowerName.endsWith(".jpeg")) {
                continue;
            }
            try {
                addScreenshot(new Screenshot(filePath));
            } catch (Exception exception) {
                Client.LOG.warn("Failed to load screenshot {}: {}", filePath, exception.toString());
            }
        }
    }

    public static void addScreenshot(Screenshot screenshot) {
        SCREENSHOTS.add(screenshot);
    }

    public static void removeScreenshot(Screenshot screenshot) {
        SCREENSHOTS.remove(screenshot);
    }

    public static void delete(Screenshot screenshot) {
        try {
            Files.deleteIfExists(screenshot.getPath());
        } catch (IOException exception) {
            Client.LOG.warn("Failed to delete screenshot {}: {}", screenshot.getPath(), exception.toString());
            return;
        }
        removeScreenshot(screenshot);
    }

    public static List<Screenshot> getScreenshots() {
        return SCREENSHOTS.stream()
                .sorted(Comparator.comparing((Screenshot s) -> s.getCreatedAt().toInstant()).reversed())
                .toList();
    }
}
