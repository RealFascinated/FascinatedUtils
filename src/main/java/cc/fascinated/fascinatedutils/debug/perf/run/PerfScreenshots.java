package cc.fascinated.fascinatedutils.debug.perf.run;

import cc.fascinated.fascinatedutils.debug.perf.PerfOut;
import cc.fascinated.fascinatedutils.debug.perf.PerfProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Saves one PNG of the game framebuffer per scene so the UI can be reviewed without a person at the keyboard.
 *
 * <p>JVM properties:
 * <ul>
 *   <li>{@code alumite.perf.shots=<dir>} turns the world run into a screenshot run: each scene is set up and settled,
 *       then saved as {@code <dir>/<scene id>.png} instead of being measured</li>
 *   <li>{@code alumite.perf.shots.size=<width>x<height>} (optional) sets the window size, e.g. {@code 1920x1080}</li>
 *   <li>{@code alumite.perf.shots.guiScale=<n>} (optional) forces the GUI scale so shots are comparable between runs</li>
 * </ul>
 */
public class PerfScreenshots {
    public static final PerfScreenshots INSTANCE = new PerfScreenshots();

    /**
     * Reports whether the run was asked to take screenshots.
     *
     * @return true when {@code alumite.perf.shots} is set
     */
    public boolean isEnabled() {
        return System.getProperty(PerfProperties.SHOTS) != null;
    }

    /**
     * Applies {@code alumite.perf.shots.size}. Runs well before {@link #applyGuiScale}, because Minecraft only allows a
     * GUI scale that fits the window and the new size takes effect on a later frame.
     *
     * @param minecraft the running client
     */
    public void applyWindowSize(Minecraft minecraft) {
        String size = System.getProperty(PerfProperties.SHOTS_SIZE);
        if (size != null) {
            String[] parts = size.split("x");
            minecraft.getWindow().setWindowed(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }
    }

    /**
     * Applies {@code alumite.perf.shots.guiScale} when it is set.
     *
     * @param minecraft the running client
     */
    public void applyGuiScale(Minecraft minecraft) {
        String guiScale = System.getProperty(PerfProperties.SHOTS_GUI_SCALE);
        if (guiScale == null) {
            return;
        }
        minecraft.options.guiScale().set(Integer.parseInt(guiScale));
        minecraft.resizeGui();
    }

    /**
     * Grabs the last rendered frame and writes it to {@code <dir>/<sceneId>.png}.
     *
     * @param minecraft the running client
     * @param sceneId   the scene being captured, used as the file name
     * @param onSaved   called once the file is written
     * @param onFailure called with the reason when the file could not be written
     */
    public void capture(Minecraft minecraft, String sceneId, Runnable onSaved, Consumer<String> onFailure) {
        Path file = Path.of(System.getProperty(PerfProperties.SHOTS)).toAbsolutePath().resolve(sceneId + ".png");
        Screenshot.takeScreenshot(minecraft.getMainRenderTarget(), image -> {
            try (image) {
                Files.createDirectories(file.getParent());
                image.writeToFile(file);
            } catch (IOException exception) {
                onFailure.accept("could not write " + file + ": " + exception);
                return;
            }
            PerfOut.INSTANCE.print("shot " + file + " window=" + minecraft.getWindow().getWidth() + "x" + minecraft.getWindow().getHeight() + " guiScale=" + minecraft.getWindow().getGuiScale() + " option=" + minecraft.options.guiScale().get());
            onSaved.run();
        });
    }
}
