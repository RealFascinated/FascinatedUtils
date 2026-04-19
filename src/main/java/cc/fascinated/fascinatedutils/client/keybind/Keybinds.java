package cc.fascinated.fascinatedutils.client.keybind;

import cc.fascinated.fascinatedutils.systems.hud.HUDManager;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class Keybinds {
    private static KeyMapping openMenuKeybind;

    public Keybinds() {
        openMenuKeybind = KeybindsWrapper.registerCallbackKeybind("key.fascinatedutils.open_menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, KeyMapping.Category.MISC, () -> HUDManager.INSTANCE.setEditMode(!HUDManager.INSTANCE.isEditMode()));
    }

    public static KeyMapping openMenuKeybind() {
        return openMenuKeybind;
    }
}
