package cc.fascinated.fascinatedutils.client.keybind;

import cc.fascinated.fascinatedutils.gui.screens.SocialScreen;
import cc.fascinated.fascinatedutils.systems.hud.HUDManager;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

public class Keybinds {
    private static KeyMapping openMenuKeybind;
    private static KeyMapping socialKeybind;

    public Keybinds() {
        openMenuKeybind = KeybindsWrapper.registerCallbackKeybind("key.fascinatedutils.open_menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, KeybindsWrapper.CATEGORY, () -> HUDManager.INSTANCE.setEditMode(!HUDManager.INSTANCE.isEditMode()));
        socialKeybind = KeybindsWrapper.registerCallbackKeybind("key.fascinatedutils.open_social", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_BACKSLASH, KeybindsWrapper.CATEGORY, () -> {
            Minecraft minecraftClient = Minecraft.getInstance();
            Screen currentScreen = minecraftClient.screen;
            if (currentScreen instanceof SocialScreen) {
                minecraftClient.setScreen(null);
            }
            else {
                minecraftClient.setScreen(new SocialScreen());
            }
        });
    }

    public static KeyMapping openMenuKeybind() {
        return openMenuKeybind;
    }

    public static KeyMapping socialKeybind() {
        return socialKeybind;
    }
}
