package cc.fascinated.fascinatedutils.client.keybind;

import cc.fascinated.fascinatedutils.client.ModBranding;
import cc.fascinated.fascinatedutils.gui.core.UiFocusIds;
import cc.fascinated.fascinatedutils.gui.screens.ModSettingsScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class Keybinds {
    private static KeyMapping openMenuKeybind;

    private int modSettingsFocusId = UiFocusIds.NO_FOCUS_ID;

    public Keybinds() {
        openMenuKeybind = KeybindsWrapper.registerCallbackKeybind("key.fascinatedutils.open_menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, KeyMapping.Category.MISC, () -> Minecraft.getInstance().setScreen(new ModSettingsScreen(ModBranding.modSettingsScreenTitle(), () -> modSettingsFocusId, id -> modSettingsFocusId = id)));
    }

    public static KeyMapping openMenuKeybind() {
        return openMenuKeybind;
    }
}
