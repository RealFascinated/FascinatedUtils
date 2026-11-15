package cc.fascinated.fascinatedutils.debug.perf.scene.impl;

import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfScene;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfSceneContext;
import cc.fascinated.fascinatedutils.systems.modules.impl.TabModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.TitlesModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.armor.ArmorModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.bossbar.BossbarModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.scoreboard.ScoreboardModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.statuseffects.StatusEffectsModule;

import java.util.List;

/**
 * Every vanilla overlay the mod restyles at once: sidebar scoreboard, boss bar, status effects, armor, titles, chat
 * lines and the held tab list.
 */
public class HudOverlaysPerfScene extends PerfScene {
    public static final String ID = "hud_overlays";
    private static final int CHAT_EVERY_TICKS = 10;
    private static final int TITLE_EVERY_TICKS = 40;

    public HudOverlaysPerfScene() {
        super(ID, "Scoreboard, boss bar, effects, armor, titles, chat and the tab list on screen together");
    }

    @Override
    public void setup(PerfSceneContext context) {
        context.command("scoreboard objectives add alumite_perf dummy");
        context.command("scoreboard objectives setdisplay sidebar alumite_perf");
        context.command("scoreboard players set @s alumite_perf 10");
        context.command("scoreboard players set perf_row alumite_perf 5");
        context.command("bossbar add alumite:perf \"Perf\"");
        context.command("bossbar set alumite:perf players @s");
        context.command("effect give @s speed infinite 1 true");
        context.command("effect give @s night_vision 600 0 true");
        context.command("item replace entity @s armor.head with diamond_helmet");
        context.command("item replace entity @s armor.chest with diamond_chestplate");
        context.holdKey(context.getMinecraft().options.keyPlayerList);
    }

    @Override
    public void onTick(PerfSceneContext context, int tick) {
        if (tick % CHAT_EVERY_TICKS == 0) {
            context.command("tellraw @s \"perf chat line\"");
        }
        if (tick % TITLE_EVERY_TICKS == 0) {
            context.command("title @s title \"Perf\"");
        }
    }

    @Override
    public void teardown(PerfSceneContext context) {
        context.command("scoreboard objectives remove alumite_perf");
        context.command("bossbar remove alumite:perf");
        context.command("effect clear @s");
        context.command("title @s clear");
        context.command("item replace entity @s armor.head with air");
        context.command("item replace entity @s armor.chest with air");
    }

    @Override
    public List<String> covers() {
        return List.of(PerfCoverage.mixin("ChatComponentMixin"), PerfCoverage.mixin("title.GuiMixin"), PerfCoverage.mixin("bossbar.BossHealthOverlayMixin"), PerfCoverage.mixin("bossbar.BossHealthOverlayEventsAccessor"), PerfCoverage.mixin("scoreboard.GuiMixin"), PerfCoverage.mixin("scoreboard.GuiScoreDisplayOrderAccessor"), PerfCoverage.mixin("statuseffects.GuiMixin"), PerfCoverage.mixin("tab.PlayerListHudMixin"), PerfCoverage.mixin("tab.PlayerTabOverlayMixin"), PerfCoverage.module(ScoreboardModule.class), PerfCoverage.module(BossbarModule.class), PerfCoverage.module(StatusEffectsModule.class), PerfCoverage.module(TitlesModule.class), PerfCoverage.module(TabModule.class), PerfCoverage.module(ArmorModule.class), PerfCoverage.common("DateUtils"));
    }
}
