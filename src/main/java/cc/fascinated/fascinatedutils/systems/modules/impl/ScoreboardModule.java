package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.mixin.scoreboard.GuiScoreDisplayOrderAccessor;
import cc.fascinated.fascinatedutils.systems.hud.HUDWidgetAnchor;
import cc.fascinated.fascinatedutils.systems.hud.HudDefaults;
import cc.fascinated.fascinatedutils.systems.hud.HudModule;
import cc.fascinated.fascinatedutils.systems.hud.content.HudContent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class ScoreboardModule extends HudModule {

    private final BooleanSetting hideScoreboardLines = BooleanSetting.builder().id("hide_scoreboard_lines").defaultValue(true).translationKeyPath("fascinatedutils.module.scoreboard.hide_scoreboard_lines").build();

    public ScoreboardModule() {
        super("scoreboard", "Scoreboard", 40f, HudDefaults.builder()
                .defaultState(true)
                .defaultAnchor(HUDWidgetAnchor.RIGHT)
                .defaultXOffset(0)
                .defaultYOffset(0)
                .build()
        );
        addSetting(hideScoreboardLines);
    }

    @Override
    public Runnable prepareAndDraw(GuiRenderer glRenderer, float deltaSeconds, boolean editorMode) {
        Minecraft minecraft = Minecraft.getInstance();
        float lineHeight = glRenderer.getFontHeight();
        if (minecraft.level == null) {
            if (editorMode) {
                float layoutWidth = 120f;
                float layoutHeight = lineHeight * 2f;
                getHudState().setLastLayoutWidth(layoutWidth);
                getHudState().setLastLayoutHeight(layoutHeight);
                getHudState().setCommittedLayoutWidth(layoutWidth);
                getHudState().setCommittedLayoutHeight(layoutHeight);
                return () -> {
                    glRenderer.endRenderSegment();
                    glRenderer.drawComponentText(Component.literal("Scoreboard"), 0f, 0f, 0xFFFFFFFF, false);
                };
            }
            recordHudContentSkipped();
            return null;
        }
        Scoreboard board = minecraft.level.getScoreboard();
        Objective objective = board.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (objective == null) {
            recordHudContentSkipped();
            return null;
        }
        NumberFormat objectiveScoreFormat = objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT);
        Font font = minecraft.font;
        boolean hideScores = hideScoreboardLines.getValue();
        List<ScoreRow> rows = board.listPlayerScores(objective).stream().filter(entry -> !entry.isHidden()).sorted(GuiScoreDisplayOrderAccessor.scoreDisplayOrder()).limit(15L).map(entry -> {
            PlayerTeam team = board.getPlayersTeam(entry.owner());
            Component ownerName = entry.ownerName();
            Component name = PlayerTeam.formatNameForTeam(team, ownerName);
            Component scoreText = hideScores ? Component.empty() : entry.formatValue(objectiveScoreFormat);
            int scoreWidth = hideScores ? 0 : font.width(scoreText);
            return new ScoreRow(name, scoreText, scoreWidth);
        }).toList();
        Component objectiveTitle = objective.getDisplayName();
        int titleWidth = font.width(objectiveTitle);
        int spacerWidth = font.width(Component.literal(": "));
        int widest = titleWidth;
        for (ScoreRow row : rows) {
            int rowWidth = font.width(row.name) + (row.scoreWidth > 0 ? spacerWidth + row.scoreWidth : 0);
            widest = Math.max(widest, rowWidth);
        }
        float layoutWidth = Math.max(getMinWidth(), (float) widest + 4f);
        float layoutHeight = Math.max(1f, (rows.size() + 1) * lineHeight + 1f);
        getHudState().setLastLayoutWidth(layoutWidth);
        getHudState().setLastLayoutHeight(layoutHeight);
        getHudState().setCommittedLayoutWidth(layoutWidth);
        getHudState().setCommittedLayoutHeight(layoutHeight);

        int headerBackground = minecraft.options.getBackgroundColor(0.4F);
        int bodyBackground = minecraft.options.getBackgroundColor(0.3F);
        int textColor = 0xFFFFFFFF;
        List<ScoreRow> rowsCopy = List.copyOf(rows);
        return () -> {
            float innerLeft = 2f;
            float innerRight = layoutWidth - 2f;
            glRenderer.drawRect(innerLeft - 2f, 0f, innerRight - innerLeft + 4f, lineHeight + 1f, headerBackground);
            glRenderer.drawRect(innerLeft - 2f, lineHeight + 1f, innerRight - innerLeft + 4f, layoutHeight - lineHeight - 1f, bodyBackground);
            glRenderer.endRenderSegment();
            float titleX = innerLeft + (innerRight - innerLeft - titleWidth) * 0.5f;
            glRenderer.drawComponentText(objectiveTitle, titleX, 1f, textColor, false);
            for (int index = 0; index < rowsCopy.size(); index++) {
                ScoreRow row = rowsCopy.get(index);
                float rowY = lineHeight + 1f + index * lineHeight;
                glRenderer.drawComponentText(row.name, innerLeft, rowY, textColor, false);
                if (row.scoreWidth > 0) {
                    glRenderer.drawComponentText(row.score, innerRight - row.scoreWidth, rowY, textColor, false);
                }
            }
        };
    }

    @Override
    protected @Nullable HudContent produceContent(float deltaSeconds, boolean editorMode) {
        return null;
    }

    private record ScoreRow(Component name, Component score, int scoreWidth) {}
}
