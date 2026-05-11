package cc.fascinated.fascinatedutils.systems.screenshot;

import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
            addScreenshot(new Screenshot(file.toPath()));
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
        } catch (IOException ignored) {
        }
        removeScreenshot(screenshot);
    }

    public static List<Screenshot> getScreenshots() {
        return SCREENSHOTS.stream()
                .sorted(Comparator.comparing((Screenshot s) -> s.getCreatedAt().toInstant()).reversed())
                .toList();
    }
}
