package cc.fascinated.fascinatedutils.systems.modules.impl.music;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleCategory;
import cc.fascinated.fascinatedutils.systems.modules.impl.music.feature.LegacyMusicFeature;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundSource;

@Getter
public class MusicModule extends Module {

    private final LegacyMusicFeature legacyMusicFeature;

    private final BooleanSetting continueMusicAfterLeaving = BooleanSetting.builder().id("continue_music_after_leaving")
            .defaultValue(false)
            .build();

    private volatile boolean disconnectMusicRetainActive;

    public MusicModule() {
        super("Music", ModuleCategory.GENERAL);
        legacyMusicFeature = new LegacyMusicFeature(this);
        addSetting(continueMusicAfterLeaving);
    }

    /**
     * Whether background music should keep playing when leaving a world or disconnecting from a server.
     *
     * @return {@code true} when this module and the continue-after-leaving setting are both enabled
     */
    public boolean shouldContinueMusicAfterLeavingSession() {
        return isEnabled() && continueMusicAfterLeaving.isEnabled();
    }

    /**
     * When {@code true}, {@link net.minecraft.client.sounds.MusicManager} should not stop the current background track
     * for situational changes while on the main menu after a teardown with retain mode active.
     *
     * @param minecraft the game client
     * @return {@code true} when there is no loaded level, retain mode is on, and the setting is active
     */
    public boolean shouldSuppressStoppingCurrentMusicTrack(Minecraft minecraft) {
        return minecraft.level == null
                && disconnectMusicRetainActive
                && shouldContinueMusicAfterLeavingSession();
    }

    public void activateDisconnectMusicRetain() {
        disconnectMusicRetainActive = true;
    }

    public void clearDisconnectMusicRetain() {
        disconnectMusicRetainActive = false;
    }

    /**
     * Handles {@code Minecraft.updateLevelInEngines} sound teardown: either vanilla {@link SoundManager#stop()} or
     * non-music-only stops when continuing background music across leaving or re-joining after retain.
     *
     * @param soundManager client sound manager
     * @param newLevel level being applied, or {@code null} when clearing the session
     * @param vanillaStopAll runs full {@link SoundManager#stop()}
     */
    public void applyUpdateLevelEnginesSoundStop(SoundManager soundManager, ClientLevel newLevel, Runnable vanillaStopAll) {
        if (newLevel != null) {
            boolean wasRetaining = disconnectMusicRetainActive;
            clearDisconnectMusicRetain();
            if (wasRetaining && shouldContinueMusicAfterLeavingSession()) {
                stopAllSoundsExceptMusic(soundManager);
            } else {
                vanillaStopAll.run();
            }
            return;
        }
        if (!shouldContinueMusicAfterLeavingSession()) {
            clearDisconnectMusicRetain();
            vanillaStopAll.run();
            return;
        }
        activateDisconnectMusicRetain();
        stopAllSoundsExceptMusic(soundManager);
    }

    /**
     * Stops every {@link SoundSource} except {@link SoundSource#MUSIC} (used instead of {@link SoundManager#stop()}
     * when leaving a session while keeping background music).
     *
     * @param soundManager client sound manager
     */
    public void stopAllSoundsExceptMusic(SoundManager soundManager) {
        for (SoundSource soundSource : SoundSource.values()) {
            if (soundSource == SoundSource.MUSIC) {
                continue;
            }
            soundManager.stop(null, soundSource);
        }
    }
}
