package cc.fascinated.fascinatedutils.client.keybind;

import cc.fascinated.fascinatedutils.gui2.screens.impl.ScreenshotScreen;
import cc.fascinated.fascinatedutils.gui2.screens.impl.SocialScreen;
import cc.fascinated.fascinatedutils.systems.hud.HUDManager;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

public class Keybinds {
    private static KeyMapping openMenuKeybind;
    private static KeyMapping socialKeybind;
    private static KeyMapping screenshotKeybind;

    public Keybinds() {
        openMenuKeybind = KeybindsWrapper.registerCallbackKeybind(
                "key.alumite.open_menu",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                KeybindsWrapper.CATEGORY,
                () -> HUDManager.INSTANCE.setEditMode(!HUDManager.INSTANCE.isEditMode())
        );
        socialKeybind = KeybindsWrapper.registerCallbackKeybind("key.alumite.open_social", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_BACKSLASH, KeybindsWrapper.CATEGORY, () -> {
            Minecraft minecraftClient = Minecraft.getInstance();
            Screen currentScreen = minecraftClient.screen;
            if (currentScreen instanceof SocialScreen) {
                minecraftClient.setScreen(null);
            }
            else {
                minecraftClient.setScreen(new SocialScreen());
            }
        });
        screenshotKeybind = KeybindsWrapper.registerCallbackKeybind(
                "key.alumite.open_screenshots",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_U,
                KeybindsWrapper.CATEGORY,
                () -> Minecraft.getInstance().setScreen(new ScreenshotScreen())
        );
    }

    public static KeyMapping openMenuKeybind() {
        return openMenuKeybind;
    }

    public static KeyMapping socialKeybind() {
        return socialKeybind;
    }

    public static KeyMapping screenshotKeybind() {
        return screenshotKeybind;
    }
}
