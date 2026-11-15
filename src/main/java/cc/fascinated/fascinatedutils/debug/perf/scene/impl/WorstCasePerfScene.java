package cc.fascinated.fascinatedutils.debug.perf.scene.impl;

import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfScene;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfSceneContext;
import cc.fascinated.fascinatedutils.systems.modules.impl.bossbar.BossbarModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.statuseffects.StatusEffectsModule;

import java.util.List;

/**
 * Everything heavy at once: rain, 50 named sheep and 50 block displays in view, flame bursts every 4 ticks, a boss bar
 * and status effects. Each piece reuses the commands of the scene it comes from.
 */
public class WorstCasePerfScene extends PerfScene {
    public static final String ID = "worst_case";
    private static final int SHEEP_COLUMNS = 25;
    private static final int DISPLAY_COLUMNS = 25;
    private static final double COLUMN_SPACING = 1.5;
    private static final int SHEEP_ROW_Z = 10;
    private static final int DISPLAY_ROW_Z = 16;
    private static final int BURST_Z = 12;
    private static final int BURST_EVERY_TICKS = 4;

    public WorstCasePerfScene() {
        super(ID, "Rain, 50 named sheep, 50 block displays, flame bursts, a boss bar and status effects together");
    }

    @Override
    public void setup(PerfSceneContext context) {
        context.command("weather rain 1200");
        for (int column = 0; column < SHEEP_COLUMNS; column++) {
            int offsetX = (int) ((column - SHEEP_COLUMNS / 2) * COLUMN_SPACING);
            context.surfaceCommand(offsetX, SHEEP_ROW_Z, "summon sheep ~ ~ ~ {NoAI:1b,Silent:1b,PersistenceRequired:1b,CustomName:\"perf\",CustomNameVisible:1b}");
            context.surfaceCommand(offsetX, SHEEP_ROW_Z + 2, "summon sheep ~ ~ ~ {NoAI:1b,Silent:1b,PersistenceRequired:1b,CustomName:\"perf\",CustomNameVisible:1b}");
        }
        for (int column = 0; column < DISPLAY_COLUMNS; column++) {
            int offsetX = (int) ((column - DISPLAY_COLUMNS / 2) * COLUMN_SPACING);
            context.surfaceCommand(offsetX, DISPLAY_ROW_Z, "summon block_display ~ ~1 ~ {block_state:{Name:\"minecraft:stone\"}}");
            context.surfaceCommand(offsetX, DISPLAY_ROW_Z + 2, "summon block_display ~ ~1 ~ {block_state:{Name:\"minecraft:stone\"}}");
        }
        context.command("bossbar add alumite:worst_case \"Worst case\"");
        context.command("bossbar set alumite:worst_case players @s");
        context.command("effect give @s speed infinite 1 true");
        context.command("effect give @s night_vision infinite 0 true");
    }

    @Override
    public void onTick(PerfSceneContext context, int tick) {
        if (tick % BURST_EVERY_TICKS == 0) {
            context.surfaceCommand(0, BURST_Z, "particle minecraft:flame ~ ~4 ~ 3 2 3 0.02 150 force");
        }
    }

    @Override
    public void teardown(PerfSceneContext context) {
        context.command("weather clear");
        context.command("kill @e[type=!player]");
        context.command("bossbar remove alumite:worst_case");
        context.command("effect clear @s");
    }

    @Override
    public List<String> covers() {
        return List.of(PerfCoverage.mixin("turboweather.WeatherEffectRendererMixin"), PerfCoverage.system("turboweather"), PerfCoverage.system("turboentities"), PerfCoverage.mixin("turboentities.EntityRendererMixin"), PerfCoverage.mixin("turboentities.LevelRendererMixin"), PerfCoverage.system("turboparticles"), PerfCoverage.mixin("turboparticles.ParticleEngineMixin"), PerfCoverage.mixin("bossbar.BossHealthOverlayMixin"), PerfCoverage.module(BossbarModule.class), PerfCoverage.mixin("statuseffects.GuiMixin"), PerfCoverage.module(StatusEffectsModule.class));
    }
}
