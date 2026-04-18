package cc.fascinated.fascinatedutils.common;

import lombok.Getter;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum BiomeColors {
    BADLANDS("minecraft", "badlands", 0xFFD88A47), BAMBOO_JUNGLE("minecraft", "bamboo_jungle", 0xFF5FAF48), BASALT_DELTAS("minecraft", "basalt_deltas", 0xFF4C3E53), BEACH("minecraft", "beach", 0xFFE8DEB2), BIRCH_FOREST("minecraft", "birch_forest", 0xFF7FB35B), CHERRY_GROVE("minecraft", "cherry_grove", 0xFFF2A6C8), COLD_OCEAN("minecraft", "cold_ocean", 0xFF3C6FA8), CRIMSON_FOREST("minecraft", "crimson_forest", 0xFF8B2E4B), DARK_FOREST("minecraft", "dark_forest", 0xFF3E5B2D), DEEP_COLD_OCEAN("minecraft", "deep_cold_ocean", 0xFF355D91), DEEP_DARK("minecraft", "deep_dark", 0xFF263947), DEEP_FROZEN_OCEAN("minecraft", "deep_frozen_ocean", 0xFF6A89B8), DEEP_LUKEWARM_OCEAN("minecraft", "deep_lukewarm_ocean", 0xFF2F7FA8), DEEP_OCEAN("minecraft", "deep_ocean", 0xFF2C4F7A), DESERT("minecraft", "desert", 0xFFE5CF73), DRIPSTONE_CAVES("minecraft", "dripstone_caves", 0xFF886A4A), END_BARRENS("minecraft", "end_barrens", 0xFFCDBF8E), END_HIGHLANDS("minecraft", "end_highlands", 0xFFD6CA95), END_MIDLANDS("minecraft", "end_midlands", 0xFFCFC58F), ERODED_BADLANDS("minecraft", "eroded_badlands", 0xFFE09D62), FLOWER_FOREST("minecraft", "flower_forest", 0xFF82C868), FOREST("minecraft", "forest", 0xFF5E9A4A), FROZEN_OCEAN("minecraft", "frozen_ocean", 0xFF86A6CC), FROZEN_PEAKS("minecraft", "frozen_peaks", 0xFFE5EEF6), FROZEN_RIVER("minecraft", "frozen_river", 0xFF95B5D8), GROVE("minecraft", "grove", 0xFFB7D0C0), ICE_SPIKES("minecraft", "ice_spikes", 0xFFDCEFFC), JAGGED_PEAKS("minecraft", "jagged_peaks", 0xFFCDD8E4), JUNGLE("minecraft", "jungle", 0xFF4B8B3B), LUKEWARM_OCEAN("minecraft", "lukewarm_ocean", 0xFF3D97BC), LUSH_CAVES("minecraft", "lush_caves", 0xFF4F9E64), MANGROVE_SWAMP("minecraft", "mangrove_swamp", 0xFF5D7E53), MEADOW("minecraft", "meadow", 0xFF88C87A), MUSHROOM_FIELDS("minecraft", "mushroom_fields", 0xFF8A5FA8), NETHER_WASTES("minecraft", "nether_wastes", 0xFF7A3A2B), OCEAN("minecraft", "ocean", 0xFF346A9F), OLD_GROWTH_BIRCH_FOREST("minecraft", "old_growth_birch_forest", 0xFF6D9B56), OLD_GROWTH_PINE_TAIGA("minecraft", "old_growth_pine_taiga", 0xFF4E6F47), OLD_GROWTH_SPRUCE_TAIGA("minecraft", "old_growth_spruce_taiga", 0xFF456340), PLAINS("minecraft", "plains", 0xFF8DB360), RIVER("minecraft", "river", 0xFF3C7AB3), SAVANNA("minecraft", "savanna", 0xFFB8C56A), SAVANNA_PLATEAU("minecraft", "savanna_plateau", 0xFFC4CB72), SMALL_END_ISLANDS("minecraft", "small_end_islands", 0xFFD4C791), SNOWY_BEACH("minecraft", "snowy_beach", 0xFFD4E4ED), SNOWY_PLAINS("minecraft", "snowy_plains", 0xFFE8F0F6), SNOWY_SLOPES("minecraft", "snowy_slopes", 0xFFDCE9F4), SNOWY_TAIGA("minecraft", "snowy_taiga", 0xFFAAC3C8), SOUL_SAND_VALLEY("minecraft", "soul_sand_valley", 0xFF6D605B), SPARSE_JUNGLE("minecraft", "sparse_jungle", 0xFF5E9647), STONY_PEAKS("minecraft", "stony_peaks", 0xFF9BA2AB), STONY_SHORE("minecraft", "stony_shore", 0xFF9CA19A), SUNFLOWER_PLAINS("minecraft", "sunflower_plains", 0xFF9CC765), SWAMP("minecraft", "swamp", 0xFF6A7A42), TAIGA("minecraft", "taiga", 0xFF5A7B56), THE_END("minecraft", "the_end", 0xFFD6CB9A), THE_VOID("minecraft", "the_void", 0xFF151521), WARM_OCEAN("minecraft", "warm_ocean", 0xFF3AA8C6), WARPED_FOREST("minecraft", "warped_forest", 0xFF2F8E8D), WINDSWEPT_FOREST("minecraft", "windswept_forest", 0xFF708E73), WINDSWEPT_GRAVELLY_HILLS("minecraft", "windswept_gravelly_hills", 0xFF8A9186), WINDSWEPT_HILLS("minecraft", "windswept_hills", 0xFF7C9373), WINDSWEPT_SAVANNA("minecraft", "windswept_savanna", 0xFFA6B668), WOODED_BADLANDS("minecraft", "wooded_badlands", 0xFFB97A44);

    private static final Map<Identifier, BiomeColors> BY_BIOME_ID = new HashMap<>();
    private static final int UNKNOWN_BIOME_COLOR_ARGB = 0xFFFFFFFF;

    static {
        for (BiomeColors biomeColor : values()) {
            BY_BIOME_ID.put(biomeColor.biomeId, biomeColor);
        }
    }

    private final Identifier biomeId;
    private final int colorArgb;

    BiomeColors(String namespace, String path, int colorArgb) {
        this.biomeId = Identifier.fromNamespaceAndPath(namespace, path);
        this.colorArgb = colorArgb;
    }

    /**
     * Resolve the configured ARGB color for a biome id.
     */
    public static int colorForBiomeId(Identifier biomeId) {
        if (biomeId == null) {
            return UNKNOWN_BIOME_COLOR_ARGB;
        }
        BiomeColors biomeColors = BY_BIOME_ID.get(biomeId);
        return biomeColors == null ? UNKNOWN_BIOME_COLOR_ARGB : biomeColors.colorArgb;
    }
}
