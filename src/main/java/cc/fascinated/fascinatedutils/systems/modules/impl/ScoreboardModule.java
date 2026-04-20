package cc.fascinated.fascinatedutils.systems.modules.impl;

import java.util.List;

import org.jspecify.annotations.Nullable;

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

public class ScoreboardModule extends HudModule {

    private record ScoreRow(Component name, Component score, int scoreWidth) {}

    private static final Component PREVIEW_TITLE = Component.literal("My Server");

    private static final List<ScoreRow> PREVIEW_ROWS = List.of(
        new ScoreRow(Component.literal("Player1"), Component.empty(), 0),
        new ScoreRow(Component.literal("Player2"), Component.empty(), 0),
        new ScoreRow(Component.literal("Player3"), Component.empty(), 0),
        new ScoreRow(Component.literal("Player4"), Component.empty(), 0)
    );

    private final BooleanSetting hideScoreboardLines = BooleanSetting.builder().id("hide_scoreboard_lines").defaultValue(true).translationKeyPath("fascinatedutils.module.scoreboard.hide_scoreboard_lines").build();
    
    public ScoreboardModule() {
        super("scoreboard", "Scoreboard", 0f, HudDefaults.builder()
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
        if (minecraft.level == null) {
          return null;
        }

        Scoreboard board = minecraft.level.getScoreboard();
        Objective objective = board.getDisplayObjective(DisplaySlot.SIDEBAR);
        
        float lineHeight = glRenderer.getFontHeight();
        if (objective == null) {
            if (editorMode) {
                return buildScoreboardDraw(glRenderer, lineHeight, PREVIEW_TITLE, PREVIEW_ROWS, minecraft);
            }
            recordHudContentSkipped();
            return null;
        }

        Font font = minecraft.font;
        NumberFormat objectiveScoreFormat = objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT);
        boolean hideScores = hideScoreboardLines.getValue();
        List<ScoreRow> rows = board.listPlayerScores(objective).stream().filter(entry -> !entry.isHidden()).sorted(GuiScoreDisplayOrderAccessor.scoreDisplayOrder()).limit(15L).map(entry -> {
            PlayerTeam team = board.getPlayersTeam(entry.owner());
            Component ownerName = entry.ownerName();
            Component name = PlayerTeam.formatNameForTeam(team, ownerName);
            Component scoreText = hideScores ? Component.empty() : entry.formatValue(objectiveScoreFormat);
            int scoreWidth = hideScores ? 0 : font.width(scoreText);
            return new ScoreRow(name, scoreText, scoreWidth);
        }).toList();

        return buildScoreboardDraw(glRenderer, lineHeight, objective.getDisplayName(), rows, minecraft);
    }

    @Override
    protected @Nullable HudContent produceContent(float deltaSeconds, boolean editorMode) {
        return null;
    }

    private Runnable buildScoreboardDraw(GuiRenderer glRenderer, float lineHeight, Component title, List<ScoreRow> rows, Minecraft minecraft) {
        Font font = minecraft.font;
        int titleWidth = font.width(title);
        int spacerWidth = font.width(Component.literal(": "));
        int widest = titleWidth;
        for (ScoreRow row : rows) {
            int rowWidth = font.width(row.name) + (row.scoreWidth > 0 ? spacerWidth + row.scoreWidth : 0);
            widest = Math.max(widest, rowWidth);
        }
        float layoutWidth = Math.max(getMinWidth(), (float) widest + 4f);
        float layoutHeight = Math.max(1f, (rows.size() + 1) * lineHeight);
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
            glRenderer.drawRect(innerLeft - 2f, 0f, innerRight - innerLeft + 4f, lineHeight, headerBackground);
            glRenderer.drawRect(innerLeft - 2f, lineHeight, innerRight - innerLeft + 4f, layoutHeight - lineHeight, bodyBackground);
            glRenderer.endRenderSegment();
            float titleX = innerLeft + (innerRight - innerLeft - titleWidth) * 0.5f;
            glRenderer.drawComponentText(title, titleX, 1f, textColor, false);
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
}
