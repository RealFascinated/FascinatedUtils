package cc.fascinated.fascinatedutils.systems.modules.impl.scoreboard.hud;

import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.mixin.scoreboard.GuiScoreDisplayOrderAccessor;
import cc.fascinated.fascinatedutils.systems.hud.HudPanel;
import cc.fascinated.fascinatedutils.systems.hud.content.HudContent;
import cc.fascinated.fascinatedutils.systems.modules.impl.scoreboard.ScoreboardModule;
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

public class ScoreboardHudPanel extends HudPanel {

    private static final Component PREVIEW_TITLE = Component.literal("My Server");
    private static final List<ScoreRow> PREVIEW_ROWS = List.of(
            new ScoreRow(Component.literal("Player1"), Component.empty(), 0),
            new ScoreRow(Component.literal("Player2"), Component.empty(), 0),
            new ScoreRow(Component.literal("Player3"), Component.empty(), 0),
            new ScoreRow(Component.literal("Player4"), Component.empty(), 0));

    private final ScoreboardModule scoreboardModule;

    public ScoreboardHudPanel(ScoreboardModule scoreboardModule) {
        super(scoreboardModule, "scoreboard", 0f);
        this.scoreboardModule = scoreboardModule;
    }

    @Override
    protected @Nullable HudContent produceHudContent(float deltaSeconds, boolean editorMode) {
        return null;
    }

    @Override
    public @Nullable Runnable prepareAndDraw(GuiRenderer glRenderer, float deltaSeconds, boolean editorMode) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return null;
        }

        Scoreboard board = minecraft.level.getScoreboard();
        Objective objective = board.getDisplayObjective(DisplaySlot.SIDEBAR);

        float lineHeight = glRenderer.getFontHeight();
        if (objective == null) {
            if (editorMode) {
                return buildScoreboardDraw(glRenderer, lineHeight, PREVIEW_TITLE, PREVIEW_ROWS, editorMode);
            }
            recordHudContentSkipped();
            return null;
        }

        Font font = minecraft.font;
        NumberFormat objectiveScoreFormat = objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT);
        boolean hideScores = scoreboardModule.scoreboardHideScoreLines();
        List<ScoreRow> rows = board.listPlayerScores(objective).stream().filter(entry -> !entry.isHidden()).sorted(GuiScoreDisplayOrderAccessor.scoreDisplayOrder()).limit(15L).map(entry -> {
            PlayerTeam team = board.getPlayersTeam(entry.owner());
            Component ownerName = entry.ownerName();
            Component name = PlayerTeam.formatNameForTeam(team, ownerName);
            Component scoreText = hideScores ? Component.empty() : entry.formatValue(objectiveScoreFormat);
            int scoreWidth = hideScores ? 0 : font.width(scoreText);
            return new ScoreRow(name, scoreText, scoreWidth);
        }).toList();

        return buildScoreboardDraw(glRenderer, lineHeight, objective.getDisplayName(), rows, editorMode);
    }

    private Runnable buildScoreboardDraw(GuiRenderer glRenderer, float lineHeight, Component title, List<ScoreRow> rows, boolean editorMode) {
        Font font = Minecraft.getInstance().font;
        int titleWidth = font.width(title);
        int spacerWidth = font.width(Component.literal(": "));
        int widest = titleWidth;
        for (ScoreRow row : rows) {
            int rowWidth = font.width(row.name) + (row.scoreWidth > 0 ? spacerWidth + row.scoreWidth : 0);
            widest = Math.max(widest, rowWidth);
        }
        float padX = hudHostModule().getPadding();
        float padY = hudHostModule().getPadding();
        float layoutWidth = Math.max(getMinWidth(), (float) widest + 2f * padX);
        float layoutHeight = Math.max(1f, (rows.size() + 1) * lineHeight + 2f * padY);
        getHudState().setLastLayoutWidth(layoutWidth);
        getHudState().setLastLayoutHeight(layoutHeight);
        getHudState().setCommittedLayoutWidth(layoutWidth);
        getHudState().setCommittedLayoutHeight(layoutHeight);

        int textColor = 0xFFFFFFFF;
        List<ScoreRow> rowsCopy = List.copyOf(rows);
        boolean textShadow = hudHostModule().isTextShadowEnabled();
        return () -> {
            hudHostModule().drawHUDPanelBackground(glRenderer, layoutWidth, layoutHeight, editorMode);
            glRenderer.endRenderSegment();
            float contentRight = layoutWidth - padX;
            float titleX = padX + (contentRight - padX - titleWidth) * 0.5f;
            glRenderer.drawComponentText(title, titleX, padY, textColor, textShadow);
            for (int index = 0; index < rowsCopy.size(); index++) {
                ScoreRow row = rowsCopy.get(index);
                float rowY = padY + (index + 1) * lineHeight;
                glRenderer.drawComponentText(row.name, padX, rowY, textColor, textShadow);
                if (row.scoreWidth > 0) {
                    glRenderer.drawComponentText(row.score, contentRight - row.scoreWidth, rowY, textColor, textShadow);
                }
            }
        };
    }

    private record ScoreRow(Component name, Component score, int scoreWidth) {}
}
