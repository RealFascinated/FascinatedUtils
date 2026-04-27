package cc.fascinated.fascinatedutils.gui;

import cc.fascinated.fascinatedutils.systems.hud.HUDModuleWidgetsElement;
import cc.fascinated.fascinatedutils.systems.modules.impl.waypoint.WaypointLabelHudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;

public class ModUiClientEntry {
    /**
     * Registers HUD overlay wiring and Fabric HUD element hooks for the immediate-mode UI.
     */
    public static void register() {
        Identifier moduleHudWidgetsId = Identifier.fromNamespaceAndPath("fascinatedutils", "module_hud_widgets");
        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, moduleHudWidgetsId, HUDModuleWidgetsElement.INSTANCE);

        Identifier waypointLabelsId = Identifier.fromNamespaceAndPath("fascinatedutils", "waypoint_labels");
        HudElementRegistry.attachElementAfter(moduleHudWidgetsId, waypointLabelsId, WaypointLabelHudElement.INSTANCE);
    }
}
