package cc.fascinated.fascinatedutils.systems.modules;

import cc.fascinated.fascinatedutils.AlumiteMod;
import cc.fascinated.fascinatedutils.client.ModUiTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Map;

/**
 * Resolves the white glyph sprite for each module so the module menu has crisp, tintable icons at any size.
 */
public class ModuleIcons {
    public static final ModuleIcons INSTANCE = new ModuleIcons();

    private static final List<String> SLUGS = List.of(
            "armor_hud", "block_outline", "blur", "bossbar", "clock", "coordinates", "cps", "days_counter", "debug",
            "fog_customizer", "fps", "freelook", "fullbright", "hurtcam", "hypixel", "inventory_tweaks", "item_tooltip",
            "mcutils", "movement", "music", "particle_filter", "ping", "process_memory", "reach", "scoreboard",
            "sound_filter", "speed", "status_effects", "system_cpu_usage", "tab", "time_changer", "titles", "tps",
            "zoom", "waypoints", "world_size", "vibrancy", "hud", "general");

    private static final Map<String, String> SLUGS_BY_MODULE_KEY = Map.ofEntries(
            Map.entry("itemtooltip", "item_tooltip"),
            Map.entry("freelook", "freelook"),
            Map.entry("blockoutline", "block_outline"),
            Map.entry("zoom", "zoom"),
            Map.entry("blur", "blur"),
            Map.entry("fogcustomizer", "fog_customizer"),
            Map.entry("worldsize", "world_size"),
            Map.entry("fullbright", "fullbright"),
            Map.entry("hurtcam", "hurtcam"),
            Map.entry("inventorytweaks", "inventory_tweaks"),
            Map.entry("tab", "tab"),
            Map.entry("bossbar", "bossbar"),
            Map.entry("soundfilter", "sound_filter"),
            Map.entry("particlefilter", "particle_filter"),
            Map.entry("timechanger", "time_changer"),
            Map.entry("titles", "titles"),
            Map.entry("vibrancy", "vibrancy"),
            Map.entry("tpswidget", "tps"),
            Map.entry("speedwidget", "speed"),
            Map.entry("dayscounterwidget", "days_counter"),
            Map.entry("music", "music"),
            Map.entry("armor", "armor_hud"),
            Map.entry("mcutils", "mcutils"),
            Map.entry("statuseffects", "status_effects"),
            Map.entry("memorywidget", "process_memory"),
            Map.entry("coordinateswidget", "coordinates"),
            Map.entry("hypixel", "hypixel"),
            Map.entry("pingwidget", "ping"),
            Map.entry("cpswidget", "cps"),
            Map.entry("fpswidget", "fps"),
            Map.entry("movement", "movement"),
            Map.entry("reachwidget", "reach"),
            Map.entry("clockwidget", "clock"),
            Map.entry("systemcpuusagewidget", "system_cpu_usage"),
            Map.entry("scoreboard", "scoreboard"),
            Map.entry("waypoints", "waypoints"));

    /**
     * Registers every module icon sprite as a standalone texture.
     *
     * @param minecraft the active Minecraft instance
     */
    public void loadTextures(Minecraft minecraft) {
        for (String slug : SLUGS) {
            ModUiTextures.registerSprite(minecraft, spriteId(slug));
        }
    }

    /**
     * Finds the icon sprite for a module, falling back to its category icon.
     *
     * @param module the module to represent
     * @return the sprite identifier, never null
     */
    public Identifier iconFor(Module module) {
        String slug = SLUGS_BY_MODULE_KEY.get(module.getModuleKey());
        if (slug == null) {
            slug = module.getCategory() == ModuleCategory.HUD ? "hud" : "general";
        }
        return spriteId(slug);
    }

    private Identifier spriteId(String slug) {
        return Identifier.fromNamespaceAndPath(AlumiteMod.MOD_ID, "ui/module/" + slug);
    }
}
