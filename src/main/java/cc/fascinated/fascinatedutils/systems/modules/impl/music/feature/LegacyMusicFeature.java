package cc.fascinated.fascinatedutils.systems.modules.impl.music.feature;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.systems.modules.Feature;
import cc.fascinated.fascinatedutils.systems.modules.impl.music.MusicModule;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.util.Set;

public class LegacyMusicFeature extends Feature<MusicModule> {

    /**
     * Logical {@link Sound#getLocation()} paths allowed in legacy-only mode (Java 26.1.2 layout), not
     * not {@link Sound#getPath()} file ids (those use a {@code sounds/} prefix and {@code .ogg} suffix). Newer vanilla tracks are omitted; porting may extend this set.
     */
    private static final Set<String> LEGACY_VANILLA_MUSIC_STREAM_PATHS = Set.of(
            "music/game/clark",
            "music/game/creative/aria_math",
            "music/game/creative/biome_fest",
            "music/game/creative/blind_spots",
            "music/game/creative/dreiton",
            "music/game/creative/haunt_muskie",
            "music/game/creative/taswell",
            "music/game/danny",
            "music/game/dry_hands",
            "music/game/end/alpha",
            "music/game/end/boss",
            "music/game/end/the_end",
            "music/game/haggstrom",
            "music/game/key",
            "music/game/living_mice",
            "music/game/mice_on_venus",
            "music/game/minecraft",
            "music/game/nether/ballad_of_the_cats",
            "music/game/nether/concrete_halls",
            "music/game/nether/dead_voxel",
            "music/game/nether/warmth",
            "music/game/oxygene",
            "music/game/subwoofer_lullaby",
            "music/game/sweden",
            "music/game/water/axolotl",
            "music/game/water/dragon_fish",
            "music/game/water/shuniji",
            "music/game/wet_hands",
            "music/menu/beginning_2",
            "music/menu/floating_trees",
            "music/menu/moog_city_2",
            "music/menu/mutation"
    );

    private final BooleanSetting legacyMusicOnly = BooleanSetting.builder().id("legacy_music_only")
            .defaultValue(false)
            .build();

    public LegacyMusicFeature(MusicModule musicModule) {
        super(musicModule);
        musicModule.addSetting(legacyMusicOnly);
    }

    /**
     * Whether legacy-only filtering is active for background music.
     *
     * @return {@code true} when the host module and legacy-only setting are both enabled
     */
    public boolean isFilterActive() {
        return getModule().isEnabled() && legacyMusicOnly.isEnabled();
    }

    /**
     * Vanilla global music channels (menu, game, creative, credits, end, dragon fight, underwater),
     * as opposed to per-biome soundtrack events such as {@code music.overworld.*}.
     *
     * @param sound registered sound for the situational {@link Music} entry
     * @return {@code true} when the holder is one of those channels
     */
    public boolean isVanillaGlobalMusicChannel(Holder<SoundEvent> sound) {
        return sound.unwrapKey()
                .map(ResourceKey::identifier)
                .filter(identifier -> Identifier.DEFAULT_NAMESPACE.equals(identifier.getNamespace()))
                .map(Identifier::getPath)
                .filter(LegacyMusicFeature::isVanillaGlobalMusicSoundPath)
                .isPresent();
    }

    /**
     * Blocks starting a resolved music stream that is not on the legacy vanilla allowlist.
     * Only applies to {@link SoundSource#MUSIC}, {@code minecraft:} paths under {@code music/game/} or
     * {@code music/menu/}; other namespaces or prefixes are left alone for mods.
     *
     * @param instance sound about to play
     * @return {@code true} to veto playback ({@link net.minecraft.client.sounds.SoundEngine.PlayResult#NOT_STARTED})
     */
    public boolean blocksNonLegacyMusicPlayback(SoundInstance instance) {
        if (!isFilterActive()) {
            return false;
        }
        if (instance.getSource() != SoundSource.MUSIC) {
            return false;
        }
        Sound picked = instance.getSound();
        if (picked == SoundManager.EMPTY_SOUND || picked == SoundManager.INTENTIONALLY_EMPTY_SOUND) {
            return false;
        }
        Identifier location = picked.getLocation();
        if (!Identifier.DEFAULT_NAMESPACE.equals(location.getNamespace())) {
            return false;
        }
        String path = location.getPath();
        if (!path.startsWith("music/game/") && !path.startsWith("music/menu/")) {
            return false;
        }
        return !LEGACY_VANILLA_MUSIC_STREAM_PATHS.contains(path);
    }

    private static boolean isVanillaGlobalMusicSoundPath(String path) {
        return switch (path) {
            case "music.menu",
                    "music.game",
                    "music.creative",
                    "music.credits",
                    "music.end",
                    "music.dragon",
                    "music.under_water" -> true;
            default -> false;
        };
    }
}
