package cc.fascinated.fascinatedutils.systems.modules;

import cc.fascinated.fascinatedutils.AlumiteMod;
import cc.fascinated.fascinatedutils.client.ModUiTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Locale;

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
        String slug = module.getDisplayName().toLowerCase(Locale.ROOT).replace(' ', '_');
        if (!SLUGS.contains(slug)) {
            slug = module.getCategory() == ModuleCategory.HUD ? "hud" : "general";
        }
        return spriteId(slug);
    }

    private Identifier spriteId(String slug) {
        return Identifier.fromNamespaceAndPath(AlumiteMod.MOD_ID, "ui/module/" + slug);
    }
}
