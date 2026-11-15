package cc.fascinated.fascinatedutils.systems.modules.impl.scoreboard.hud;

import cc.fascinated.fascinatedutils.mixin.scoreboard.GuiScoreDisplayOrderAccessor;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
import cc.fascinated.fascinatedutils.systems.hud.HudPanel;
import cc.fascinated.fascinatedutils.systems.hud.content.HudContent;
import cc.fascinated.fascinatedutils.systems.modules.impl.scoreboard.ScoreboardModule;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
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
import java.util.Objects;

public class ScoreboardHudPanel extends HudPanel {

    private static final List<Component> PREVIEW_NAMES = List.of(Component.literal("Player1"), Component.literal("Player2"), Component.literal("Player3"), Component.literal("Player4"));
    private static final Component PREVIEW_TITLE = Component.literal("My Server");
    private static final Component VALUE_SEPARATOR = Component.literal(": ");
    private static final int MAX_ROWS = 15;

    private final ScoreboardModule scoreboardModule;

    private List<RowInput> cachedInputs = List.of();
    private Component cachedTitle;
    private NumberFormat cachedObjectiveFormat;
    private boolean cachedHideScores;
    private Font cachedFont;
    private Snapshot cachedSnapshot;

    public ScoreboardHudPanel(ScoreboardModule scoreboardModule) {
        super(scoreboardModule, "scoreboard", 0f);
        this.scoreboardModule = scoreboardModule;
    }

    @Override
    protected @Nullable HudContent produceHudContent(float deltaSeconds, boolean editorMode) {
        return null;
    }

    @Override
    public @Nullable Runnable prepareAndDraw(AlumiteGuiRenderer glRenderer, float deltaSeconds, boolean editorMode) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        if (minecraft.level == null) {
            if (editorMode) {
                return buildScoreboardDraw(glRenderer, glRenderer.fontHeight(), previewSnapshot(font), editorMode);
            }
            return null;
        }

        Scoreboard board = minecraft.level.getScoreboard();
        Objective objective = board.getDisplayObjective(DisplaySlot.SIDEBAR);

        float lineHeight = glRenderer.fontHeight();
        if (objective == null) {
            if (editorMode) {
                return buildScoreboardDraw(glRenderer, lineHeight, previewSnapshot(font), editorMode);
            }
            recordHudContentSkipped();
            return null;
        }

        NumberFormat objectiveScoreFormat = objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT);
        boolean hideScores = scoreboardModule.scoreboardHideScoreLines();
        Component title = objective.getDisplayName();
        List<RowInput> inputs = collectRowInputs(board, objective);

        if (cachedSnapshot == null || hideScores != cachedHideScores || font != cachedFont || !Objects.equals(cachedTitle, title) || !Objects.equals(cachedObjectiveFormat, objectiveScoreFormat) || !inputs.equals(cachedInputs)) {
            cachedSnapshot = buildSnapshot(font, title, inputs, objectiveScoreFormat, hideScores);
            cachedTitle = title;
            cachedObjectiveFormat = objectiveScoreFormat;
            cachedHideScores = hideScores;
            cachedFont = font;
            cachedInputs = inputs;
        }
        return buildScoreboardDraw(glRenderer, lineHeight, cachedSnapshot, editorMode);
    }

    /**
     * Collects the sidebar entries in display order, with the team state that affects how each row is drawn.
     * Only cheap reads happen here; text and widths are built in {@link #buildSnapshot} when the inputs change.
     *
     * @param board     scoreboard of the current level
     * @param objective sidebar objective
     * @return the visible rows, at most {@link #MAX_ROWS}
     */
    private static List<RowInput> collectRowInputs(Scoreboard board, Objective objective) {
        List<PlayerScoreEntry> entries = new ArrayList<>();
        for (PlayerScoreEntry entry : board.listPlayerScores(objective)) {
            if (!entry.isHidden()) {
                entries.add(entry);
            }
        }
        entries.sort(GuiScoreDisplayOrderAccessor.scoreDisplayOrder());

        int count = Math.min(entries.size(), MAX_ROWS);
        List<RowInput> inputs = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            PlayerScoreEntry entry = entries.get(index);
            PlayerTeam team = board.getPlayersTeam(entry.owner());
            if (team == null) {
                inputs.add(new RowInput(entry.ownerName(), entry.value(), entry.numberFormatOverride(), null, null, null, null));
            }
            else {
                inputs.add(new RowInput(entry.ownerName(), entry.value(), entry.numberFormatOverride(), team, team.getPlayerPrefix(), team.getPlayerSuffix(), team.getColor()));
            }
        }
        return inputs;
    }

    private static Snapshot buildSnapshot(Font font, Component title, List<RowInput> inputs, NumberFormat objectiveScoreFormat, boolean hideScores) {
        int spacerWidth = font.width(VALUE_SEPARATOR);
        List<ScoreRow> rows = new ArrayList<>(inputs.size());
        int widest = font.width(title);
        for (RowInput input : inputs) {
            Component name = PlayerTeam.formatNameForTeam(input.team(), input.ownerName());
            Component scoreText = Component.empty();
            int scoreWidth = 0;
            if (!hideScores) {
                NumberFormat format = input.numberFormat() == null ? objectiveScoreFormat : input.numberFormat();
                scoreText = format.format(input.value());
                scoreWidth = font.width(scoreText);
            }
            int rowWidth = font.width(name) + (scoreWidth > 0 ? spacerWidth + scoreWidth : 0);
            widest = Math.max(widest, rowWidth);
            rows.add(new ScoreRow(name, scoreText, scoreWidth));
        }
        return new Snapshot(title, font.width(title), widest, List.copyOf(rows));
    }

    private static Snapshot previewSnapshot(Font font) {
        List<ScoreRow> rows = new ArrayList<>(PREVIEW_NAMES.size());
        int widest = font.width(PREVIEW_TITLE);
        for (Component name : PREVIEW_NAMES) {
            int rowWidth = font.width(name);
            widest = Math.max(widest, rowWidth);
            rows.add(new ScoreRow(name, Component.empty(), 0));
        }
        return new Snapshot(PREVIEW_TITLE, font.width(PREVIEW_TITLE), widest, List.copyOf(rows));
    }

    private Runnable buildScoreboardDraw(AlumiteGuiRenderer glRenderer, float lineHeight, Snapshot snapshot, boolean editorMode) {
        float padX = hudHostModule().getPaddingValue();
        float padY = hudHostModule().getPaddingValue();
        float layoutWidth = Math.max(getMinWidth(), (float) snapshot.widest() + 2f * padX);
        List<ScoreRow> rows = snapshot.rows();
        float layoutHeight = Math.max(1f, (rows.size() + 1) * lineHeight + 2f * padY);
        commitLayout(layoutWidth, layoutHeight);

        int textColor = 0xFFFFFFFF;
        Component title = snapshot.title();
        int titleWidth = snapshot.titleWidth();
        boolean textShadow = hudHostModule().isTextShadowEnabled();
        return () -> {
            hudHostModule().drawHUDPanelBackground(glRenderer, layoutWidth, layoutHeight, editorMode);
            glRenderer.endRenderSegment();
            float contentRight = layoutWidth - padX;
            float titleX = padX + (contentRight - padX - titleWidth) * 0.5f;
            glRenderer.drawComponentText(title, titleX, padY, textColor, textShadow);
            for (int index = 0; index < rows.size(); index++) {
                ScoreRow row = rows.get(index);
                float rowY = padY + (index + 1) * lineHeight;
                glRenderer.drawComponentText(row.name(), padX, rowY, textColor, textShadow);
                if (row.scoreWidth() > 0) {
                    glRenderer.drawComponentText(row.score(), contentRight - row.scoreWidth(), rowY, textColor, textShadow);
                }
            }
        };
    }

    private record RowInput(Component ownerName, int value, @Nullable NumberFormat numberFormat, @Nullable PlayerTeam team, @Nullable Component teamPrefix, @Nullable Component teamSuffix, @Nullable ChatFormatting teamColor) {}

    private record ScoreRow(Component name, Component score, int scoreWidth) {}

    private record Snapshot(Component title, int titleWidth, int widest, List<ScoreRow> rows) {}
}
