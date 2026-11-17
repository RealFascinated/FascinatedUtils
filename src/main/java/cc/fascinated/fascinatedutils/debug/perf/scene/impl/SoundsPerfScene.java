package cc.fascinated.fascinatedutils.debug.perf.scene.impl;

import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfScene;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfSceneContext;
import cc.fascinated.fascinatedutils.systems.modules.impl.soundfilter.SoundFilterModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.music.MusicModule;

import java.util.List;

/**
 * Sounds played at the player several times a second, so the sound engine hooks (filter and music) run per sound.
 */
public class SoundsPerfScene extends PerfScene {
    public static final String ID = "sounds";
    private static final int SOUND_EVERY_TICKS = 2;

    public SoundsPerfScene() {
        super(ID, "A note block and a step sound played at the player every 2 ticks");
    }

    @Override
    public void onTick(PerfSceneContext context, int tick) {
        if (tick % SOUND_EVERY_TICKS == 0) {
            context.command("playsound minecraft:block.note_block.harp master @s ~ ~ ~ 0.2");
            context.command("playsound minecraft:block.stone.step master @s ~ ~ ~ 0.2");
        }
    }

    @Override
    public List<String> covers() {
        return List.of(PerfCoverage.mixin("soundfilter.SoundEngineMixin"), PerfCoverage.mixin("music.MusicSoundEngineMixin"), PerfCoverage.module(SoundFilterModule.class), PerfCoverage.module(MusicModule.class));
    }
}
