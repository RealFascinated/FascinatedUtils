package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.common.color.SettingColor;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.ColorSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.mixin.scoreboard.GuiScoreDisplayOrderAccessor;
import cc.fascinated.fascinatedutils.systems.hud.HUDWidgetAnchor;
import cc.fascinated.fascinatedutils.systems.hud.HudDefaults;
import cc.fascinated.fascinatedutils.systems.hud.HudModule;
import cc.fascinated.fascinatedutils.systems.hud.HudWidgetAppearanceBuilders;
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

    private static final Component PREVIEW_TITLE = Component.literal("My Server");
    private static final List<ScoreRow> PREVIEW_ROWS = List.of(new ScoreRow(Component.literal("Player1"), Component.empty(), 0), new ScoreRow(Component.literal("Player2"), Component.empty(), 0), new ScoreRow(Component.literal("Player3"), Component.empty(), 0), new ScoreRow(Component.literal("Player4"), Component.empty(), 0));
    private final BooleanSetting hideScoreboardLines = BooleanSetting.builder().id("hide_scoreboard_lines").defaultValue(true).categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();
    private final BooleanSetting showBackground = HudWidgetAppearanceBuilders.showBackground().build();
    private final ColorSetting backgroundColor = HudWidgetAppearanceBuilders.backgroundColor().defaultValue(SettingColor.fromArgb(0x55000000)).build();
    private final BooleanSetting roundedCorners = HudWidgetAppearanceBuilders.roundedCorners().defaultValue(true).build();
    private final SliderSetting roundingRadius = HudWidgetAppearanceBuilders.roundingRadius().build();
    private final BooleanSetting showBorder = HudWidgetAppearanceBuilders.showBorder().build();
    private final SliderSetting borderThickness = HudWidgetAppearanceBuilders.borderThickness().build();
    private final ColorSetting borderColor = HudWidgetAppearanceBuilders.borderColor().build();
    public ScoreboardModule() {
        super("scoreboard", "Scoreboard", 0f, HudDefaults.builder().defaultState(true).defaultAnchor(HUDWidgetAnchor.RIGHT).defaultXOffset(0).defaultYOffset(0).build());
        addSetting(hideScoreboardLines);
        addSetting(showBackground);
        addSetting(roundedCorners);
        addSetting(showBorder);
        addSetting(roundingRadius);
        addSetting(borderThickness);
        addSetting(backgroundColor);
        addSetting(borderColor);
        showBackground.addSubSetting(backgroundColor);
        roundedCorners.addSubSetting(roundingRadius);
        showBorder.addSubSetting(borderThickness);
        showBorder.addSubSetting(borderColor);
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
                return buildScoreboardDraw(glRenderer, lineHeight, PREVIEW_TITLE, PREVIEW_ROWS);
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

        return buildScoreboardDraw(glRenderer, lineHeight, objective.getDisplayName(), rows);
    }

    @Override
    protected @Nullable HudContent produceContent(float deltaSeconds, boolean editorMode) {
        return null;
    }

    private Runnable buildScoreboardDraw(GuiRenderer glRenderer, float lineHeight, Component title, List<ScoreRow> rows) {
        Font font = Minecraft.getInstance().font;
        int titleWidth = font.width(title);
        int spacerWidth = font.width(Component.literal(": "));
        int widest = titleWidth;
        for (ScoreRow row : rows) {
            int rowWidth = font.width(row.name) + (row.scoreWidth > 0 ? spacerWidth + row.scoreWidth : 0);
            widest = Math.max(widest, rowWidth);
        }
        float padX = getPadding();
        float padY = getPadding();
        float layoutWidth = Math.max(getMinWidth(), (float) widest + 2f * padX);
        float layoutHeight = Math.max(1f, (rows.size() + 1) * lineHeight + 2f * padY);
        getHudState().setLastLayoutWidth(layoutWidth);
        getHudState().setLastLayoutHeight(layoutHeight);
        getHudState().setCommittedLayoutWidth(layoutWidth);
        getHudState().setCommittedLayoutHeight(layoutHeight);

        int textColor = 0xFFFFFFFF;
        List<ScoreRow> rowsCopy = List.copyOf(rows);
        boolean textShadow = isTextShadowEnabled();
        return () -> {
            drawHUDPanelBackground(glRenderer, layoutWidth, layoutHeight);
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
