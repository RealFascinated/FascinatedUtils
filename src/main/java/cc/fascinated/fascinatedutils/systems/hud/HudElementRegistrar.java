package cc.fascinated.fascinatedutils.systems.hud;

import cc.fascinated.fascinatedutils.systems.hud.HudModuleWidgetsElement;
import cc.fascinated.fascinatedutils.systems.modules.impl.waypoint.WaypointLabelHudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;

/**
 * Registers the mod's HUD elements with Fabric's HUD element registry.
 */
public class HudElementRegistrar {
    /**
     * Attaches the module HUD widgets element and the waypoint label element after the vanilla misc overlays.
     */
    public static void register() {
        Identifier moduleHudWidgetsId = Identifier.fromNamespaceAndPath("alumite", "module_hud_widgets");
        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, moduleHudWidgetsId, HudModuleWidgetsElement.INSTANCE);

        Identifier waypointLabelsId = Identifier.fromNamespaceAndPath("alumite", "waypoint_labels");
        HudElementRegistry.attachElementAfter(moduleHudWidgetsId, waypointLabelsId, WaypointLabelHudElement.INSTANCE);
    }
}
