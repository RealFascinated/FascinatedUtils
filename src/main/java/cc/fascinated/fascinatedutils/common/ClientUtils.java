package cc.fascinated.fascinatedutils.common;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

@UtilityClass
public class ClientUtils {
    private final String OS_NAME = System.getProperty("os.name").toLowerCase();

    public static boolean isMacOS() {
        return OS_NAME.contains("mac");
    }

    /**
     * Copies text to the system clipboard using the game window.
     * Call from the Minecraft client thread (for example inside {@link Minecraft#execute(Runnable)}
     * when coming from a background thread).
     */
    public static void copyToClipboard(String text) {
        if (text == null) {
            return;
        }
        Minecraft minecraftClient = Minecraft.getInstance();
        GLFW.glfwSetClipboardString(minecraftClient.getWindow().handle(), text);
    }
}
