package cc.fascinated.fascinatedutils.gui;

import cc.fascinated.fascinatedutils.systems.hud.HUDModuleWidgetsElement;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class ModUiClientEntry {
    /**
     * Registers HUD overlay wiring, keybinds, and Fabric HUD element hooks for the immediate-mode UI.
     */
    public static void register() {
        KeyMapping hudEditorKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.fascinatedutils.hud_editor", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_CONTROL, KeyMapping.Category.MISC));
        ModUiClientHudTick.INSTANCE.setHudEditorKey(hudEditorKey);

        Identifier moduleHudWidgetsId = Identifier.fromNamespaceAndPath("fascinatedutils", "module_hud_widgets");
        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, moduleHudWidgetsId, HUDModuleWidgetsElement.INSTANCE);
    }
}
