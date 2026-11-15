package cc.fascinated.fascinatedutils.debug.perf.scene.impl;

import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfScene;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfSceneContext;
import cc.fascinated.fascinatedutils.systems.modules.impl.ParticleFilterModule;
import cc.fascinated.fascinatedutils.systems.turboparticles.TurboParticles;

import java.util.List;

/**
 * Repeated flame and cloud bursts plus a block that is placed and destroyed, so particle creation, filtering, culling
 * and block-break effects all run.
 */
public class ParticlesPerfScene extends PerfScene {
    public static final String ID = "particles";
    private static final int BURST_EVERY_TICKS = 4;
    private static final int BREAK_EVERY_TICKS = 10;
    private static final int CLEAN_ITEMS_EVERY_TICKS = 100;
    private static final int BURST_Z = 12;
    private static final int WALL_Z = 8;
    private static final String BREAK_BLOCK_POSITION = "~2 ~-4 ~6";

    public ParticlesPerfScene() {
        super(ID, "Flame and cloud bursts every 4 ticks behind a wall 8 blocks out, and a block broken every 10 ticks");
    }

    @Override
    public void setup(PerfSceneContext context) {
        // Solid wall between the camera and the bursts, so the bursts stay in view but are occluded.
        context.surfaceCommand(0, WALL_Z, "fill ~-12 ~-3 ~ ~12 ~12 ~ bedrock keep");
    }

    @Override
    public void onTick(PerfSceneContext context, int tick) {
        if (tick % BURST_EVERY_TICKS == 0) {
            context.surfaceCommand(0, BURST_Z, "particle minecraft:flame ~ ~4 ~ 3 2 3 0.02 150 force");
            context.surfaceCommand(0, BURST_Z, "particle minecraft:cloud ~ ~4 ~ 3 2 3 0.02 100 force");
        }
        if (tick % BREAK_EVERY_TICKS == 0) {
            context.command("setblock " + BREAK_BLOCK_POSITION + " stone");
        } else if (tick % BREAK_EVERY_TICKS == BREAK_EVERY_TICKS / 2) {
            context.command("setblock " + BREAK_BLOCK_POSITION + " air destroy");
        }
        if (tick % CLEAN_ITEMS_EVERY_TICKS == 0) {
            context.command("kill @e[type=item]");
        }
    }

    @Override
    public void teardown(PerfSceneContext context) {
        context.command("setblock " + BREAK_BLOCK_POSITION + " air");
        context.command("kill @e[type=item]");
        context.command("fill -12 0 " + WALL_Z + " 12 319 " + WALL_Z + " air replace bedrock");
    }

    @Override
    public List<String> covers() {
        return List.of(PerfCoverage.mixin("turboparticles.ParticleEngineAccessor"), PerfCoverage.mixin("turboparticles.ParticleEngineMixin"), PerfCoverage.mixin("turboparticles.SingleQuadParticleMixin"), PerfCoverage.mixin("particlefilter.ParticleEngineMixin"), PerfCoverage.mixin("particlefilter.ClientLevelParticleMixin"), PerfCoverage.module(ParticleFilterModule.class), PerfCoverage.system("turboparticles"), PerfCoverage.listener(TurboParticles.class));
    }
}
