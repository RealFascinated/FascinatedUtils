package cc.fascinated.fascinatedutils.systems.modules.impl.wawla.hud;

import cc.fascinated.fascinatedutils.common.Colors;
import cc.fascinated.fascinatedutils.gui.hooks.FadeInAnim;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UiColor;
import cc.fascinated.fascinatedutils.systems.modules.impl.wawla.WawlaWidget;
import cc.fascinated.fascinatedutils.systems.modules.impl.wawla.WawlaWidget.CrosshairTarget;
import cc.fascinated.fascinatedutils.systems.hud.anchor.HudAnchorContentAlignment;
import cc.fascinated.fascinatedutils.systems.hud.anchor.HudAnchorLayout;
import cc.fascinated.fascinatedutils.systems.hud.HudPanel;
import cc.fascinated.fascinatedutils.systems.hud.content.HudContent;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class WawlaHudPanel extends HudPanel {

    private static final float ICON_SIZE = 16f;
    private static final float ICON_TEXT_GAP = 5f;
    private static final float LINE_GAP = 1f;
    private static final float BREAK_BAR_HEIGHT = 2f;
    private static final float BREAK_BAR_LERP_SPEED = 14f;

    private static final int TITLE_COLOR = UiColor.argb("#f2f6ff");
    private static final int SOURCE_COLOR = UiColor.argb("#4a5cd3");
    private static final int SUBTITLE_COLOR = UiColor.argb("#aaaaaa");
    private static final int BREAK_BAR_BACKGROUND = UiColor.argb("#44232a33");
    private static final int BREAK_BAR_FILL = UiColor.argb("#ffffffff");

    private final WawlaWidget wawlaWidget;
    @Nullable
    private CrosshairTarget lastCrosshairTarget;
    private float smoothedBreakProgress;
    private final FadeInAnim fadeAnim = new FadeInAnim(100f);
    private final FadeInAnim breakBarAnim = new FadeInAnim(150f);

    public WawlaHudPanel(WawlaWidget wawlaWidget) {
        super(wawlaWidget, "wawla", 0f);
        this.wawlaWidget = wawlaWidget;
    }

    @Override
    protected @Nullable HudContent produceHudContent(float deltaSeconds, boolean editorMode) {
        return null;
    }

    @Override
    public @Nullable Runnable prepareAndDraw(GuiRenderer glRenderer, float deltaSeconds, boolean editorMode) {
        CrosshairTarget displayTarget = wawlaWidget.resolveCrosshairTarget(editorMode);

        fadeAnim.tick(deltaSeconds);
        breakBarAnim.tick(deltaSeconds);
        if (displayTarget != null) {
            lastCrosshairTarget = displayTarget;
            fadeAnim.show();
        } else {
            fadeAnim.hide();
        }
        if (!fadeAnim.isVisible()) {
            return null;
        }

        CrosshairTarget target = lastCrosshairTarget;
        if (target == null) {
            return null;
        }

        float lineHeight = glRenderer.getFontHeight();
        List<String> subtitleLines = target.subtitleLines();
        int extraLines = subtitleLines.size();
        int totalLines = 2 + extraLines;
        float textBlockHeight = lineHeight * totalLines + LINE_GAP * (totalLines - 1);
        float contentHeight = Math.max(ICON_SIZE, textBlockHeight);

        String rawDisplayName = target.displayName();
        String strippedDisplayName = rawDisplayName == null ? "" : rawDisplayName.replaceAll("§.", "").trim();
        String titleMini = "<color:" + Colors.rgbHex(TITLE_COLOR) + ">" + (strippedDisplayName.isEmpty() ? target.entityName() : rawDisplayName) + "</color>";
        List<String> subtitleMinis = subtitleLines.stream()
                .map(line -> "<color:" + Colors.rgbHex(SUBTITLE_COLOR) + ">" + line + "</color>")
                .toList();
        float[] subtitleWidths = new float[subtitleMinis.size()];
        float subtitleMaxWidth = 0f;
        for (int subtitleIndex = 0; subtitleIndex < subtitleMinis.size(); subtitleIndex++) {
            subtitleWidths[subtitleIndex] = glRenderer.measureMiniMessageTextWidth(subtitleMinis.get(subtitleIndex));
            subtitleMaxWidth = Math.max(subtitleMaxWidth, subtitleWidths[subtitleIndex]);
        }
        String sourceMini = "<i><color:" + Colors.rgbHex(SOURCE_COLOR) + ">" + target.sourceName() + "</color></i>";

        float titleWidth = glRenderer.measureMiniMessageTextWidth(titleMini);
        float sourceWidth = glRenderer.measureMiniMessageTextWidth(sourceMini);
        float textBlockWidth = Math.max(Math.max(titleWidth, subtitleMaxWidth), sourceWidth);
        float panelPadding = hudHostModule().getPadding();

        if (target.showBreakBar()) {
            float targetBreakProgress = Mth.clamp(target.breakProgress(), 0f, 1f);
            float lerpFactor = targetBreakProgress >= smoothedBreakProgress
                ? Mth.clamp(deltaSeconds * 20f, 0f, 1f)
                : Mth.clamp(deltaSeconds * BREAK_BAR_LERP_SPEED, 0f, 1f);
            smoothedBreakProgress = Mth.lerp(lerpFactor, smoothedBreakProgress, targetBreakProgress);
            breakBarAnim.show();
        } else {
            breakBarAnim.hide();
            if (!breakBarAnim.isVisible()) {
                smoothedBreakProgress = 0f;
            }
        }

        boolean renderBreakBar = breakBarAnim.isVisible();
        float layoutWidth = Math.max(getMinWidth(), panelPadding * 2f + ICON_SIZE + ICON_TEXT_GAP + textBlockWidth);
        float layoutHeight = panelPadding * 2f + contentHeight;
        getHudState().setLastLayoutWidth(layoutWidth);
        getHudState().setLastLayoutHeight(layoutHeight);
        getHudState().setCommittedLayoutWidth(layoutWidth);
        getHudState().setCommittedLayoutHeight(layoutHeight);

        float fadeAlpha = fadeAnim.progress().value();
        float breakBarAlpha = breakBarAnim.progress().value();
        float capturedBreakProgress = smoothedBreakProgress;
        boolean textShadow = hudHostModule().isTextShadowEnabled();
        return () -> {
            glRenderer.setMultiplyAlpha(fadeAlpha);
            hudHostModule().drawHUDPanelBackground(glRenderer, layoutWidth, layoutHeight, editorMode);
            float iconY = panelPadding + HudAnchorLayout.verticalOffsetInInnerBand(contentHeight, ICON_SIZE, hudContentVerticalAlignment());
            float textBlockY = panelPadding + HudAnchorLayout.verticalOffsetInInnerBand(contentHeight, textBlockHeight, hudContentVerticalAlignment());
            boolean rightAligned = hudContentHorizontalAlignment() == HudAnchorContentAlignment.Horizontal.RIGHT;

            if (!rightAligned) {
                glRenderer.drawGuiItem(target.iconStack(), panelPadding, iconY);
                float textX = panelPadding + ICON_SIZE + ICON_TEXT_GAP;
                glRenderer.drawMiniMessageText(titleMini, textX, textBlockY, textShadow);
                for (int si = 0; si < subtitleMinis.size(); si++) {
                    glRenderer.drawMiniMessageText(subtitleMinis.get(si), textX, textBlockY + (lineHeight + LINE_GAP) * (si + 1), textShadow);
                }
                glRenderer.drawMiniMessageText(sourceMini, textX, textBlockY + (lineHeight + LINE_GAP) * (extraLines + 1), textShadow);
            } else {
                float iconX = layoutWidth - panelPadding - ICON_SIZE;
                glRenderer.drawGuiItem(target.iconStack(), iconX, iconY);
                float textRightEdge = iconX - ICON_TEXT_GAP;
                glRenderer.drawMiniMessageText(titleMini, textRightEdge - titleWidth, textBlockY, textShadow);
                for (int si = 0; si < subtitleMinis.size(); si++) {
                    glRenderer.drawMiniMessageText(subtitleMinis.get(si), textRightEdge - subtitleWidths[si], textBlockY + (lineHeight + LINE_GAP) * (si + 1), textShadow);
                }
                glRenderer.drawMiniMessageText(sourceMini, textRightEdge - sourceWidth, textBlockY + (lineHeight + LINE_GAP) * (extraLines + 1), textShadow);
            }

            if (renderBreakBar) {
                float barY = layoutHeight - BREAK_BAR_HEIGHT;
                glRenderer.setMultiplyAlpha(fadeAlpha * breakBarAlpha);
                glRenderer.drawRect(0, barY, layoutWidth, BREAK_BAR_HEIGHT, BREAK_BAR_BACKGROUND);
                glRenderer.drawRect(0, barY, layoutWidth * capturedBreakProgress, BREAK_BAR_HEIGHT, BREAK_BAR_FILL);
                glRenderer.setMultiplyAlpha(fadeAlpha);
            }

            glRenderer.resetMultiplyAlpha();
        };
    }
}
