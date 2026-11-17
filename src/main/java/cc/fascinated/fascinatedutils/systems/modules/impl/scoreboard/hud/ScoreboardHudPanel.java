package cc.fascinated.fascinatedutils.systems.modules.impl.scoreboard.hud;

import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.gui.node.display.RichTextNode;
import cc.fascinated.fascinatedutils.gui.node.layout.ColumnNode;
import cc.fascinated.fascinatedutils.gui.node.layout.SplitRowNode;
import cc.fascinated.fascinatedutils.mixin.scoreboard.GuiScoreDisplayOrderAccessor;
import cc.fascinated.fascinatedutils.systems.hud.panel.HudComposeContext;
import cc.fascinated.fascinatedutils.systems.hud.panel.HudPanel;
import cc.fascinated.fascinatedutils.systems.modules.impl.scoreboard.ScoreboardModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ScoreboardHudPanel extends HudPanel {

    private static final Component VALUE_SEPARATOR = Component.literal(": ");
    private static final int MAX_ROWS = 15;
    private static final int PREVIEW_PLAYER_COUNT = 4;

    private final ScoreboardModule scoreboardModule;

    public ScoreboardHudPanel(ScoreboardModule scoreboardModule) {
        super(scoreboardModule, "scoreboard", 0f);
        this.scoreboardModule = scoreboardModule;
    }

    @Override
    protected @Nullable UiNode compose(HudComposeContext context) {
        ClientLevel level = Minecraft.getInstance().level;
        Objective objective = level == null ? null : level.getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR);
        if (objective == null) {
            return context.editorMode() ? previewColumn(context) : null;
        }
        return sidebarColumn(context, level.getScoreboard(), objective);
    }

    /**
     * Builds the sidebar in display order: the centered title, then at most {@link #MAX_ROWS} rows with the name on the
     * left and the score on the right edge.
     */
    private ColumnNode sidebarColumn(HudComposeContext context, Scoreboard board, Objective objective) {
        boolean hideScores = scoreboardModule.scoreboardHideScoreLines();
        NumberFormat objectiveScoreFormat = objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT);
        List<PlayerScoreEntry> entries = new ArrayList<>();
        for (PlayerScoreEntry entry : board.listPlayerScores(objective)) {
            if (!entry.isHidden()) {
                entries.add(entry);
            }
        }
        entries.sort(GuiScoreDisplayOrderAccessor.scoreDisplayOrder());

        ColumnNode column = titledColumn(context, objective.getDisplayName());
        int separatorWidth = Minecraft.getInstance().font.width(VALUE_SEPARATOR);
        int count = Math.min(entries.size(), MAX_ROWS);
        for (int index = 0; index < count; index++) {
            PlayerScoreEntry entry = entries.get(index);
            RichTextNode name = RichTextNode.component(PlayerTeam.formatNameForTeam(board.getPlayersTeam(entry.owner()), entry.ownerName()), context.textShadow());
            RichTextNode score = null;
            if (!hideScores) {
                NumberFormat format = entry.numberFormatOverride() == null ? objectiveScoreFormat : entry.numberFormatOverride();
                Component scoreText = format.format(entry.value());
                if (Minecraft.getInstance().font.width(scoreText) > 0) {
                    score = RichTextNode.component(scoreText, context.textShadow());
                }
            }
            column.addChild(new SplitRowNode(name, score, separatorWidth));
        }
        return column;
    }

    private ColumnNode previewColumn(HudComposeContext context) {
        ColumnNode column = titledColumn(context, Component.literal(hudText("preview_title")));
        for (int playerNumber = 1; playerNumber <= PREVIEW_PLAYER_COUNT; playerNumber++) {
            column.addChild(RichTextNode.component(Component.literal(hudText("preview_player_" + playerNumber)), context.textShadow()));
        }
        return column;
    }

    private static ColumnNode titledColumn(HudComposeContext context, Component title) {
        ColumnNode column = new ColumnNode(0);
        column.addChild(RichTextNode.component(title, context.textShadow()).alignX(0.5f));
        return column;
    }
}
