package cc.fascinated.fascinatedutils.systems.modules.impl.bossbar.hud;

import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.mixin.bossbar.BossHealthOverlayEventsAccessor;
import cc.fascinated.fascinatedutils.systems.hud.HudPanel;
import cc.fascinated.fascinatedutils.systems.hud.content.HudContent;
import cc.fascinated.fascinatedutils.systems.modules.impl.bossbar.BossbarModule;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BossbarHudPanel extends HudPanel {

    private static final float BAR_HEIGHT = 5f;
    private static final float NAME_BAR_GAP = 2f;
    private static final float ROW_GAP = 3f;

    private static final Identifier[] BAR_BACKGROUND_SPRITES = {
        Identifier.withDefaultNamespace("boss_bar/pink_background"),
        Identifier.withDefaultNamespace("boss_bar/blue_background"),
        Identifier.withDefaultNamespace("boss_bar/red_background"),
        Identifier.withDefaultNamespace("boss_bar/green_background"),
        Identifier.withDefaultNamespace("boss_bar/yellow_background"),
        Identifier.withDefaultNamespace("boss_bar/purple_background"),
        Identifier.withDefaultNamespace("boss_bar/white_background")
    };
    private static final Identifier[] BAR_PROGRESS_SPRITES = {
        Identifier.withDefaultNamespace("boss_bar/pink_progress"),
        Identifier.withDefaultNamespace("boss_bar/blue_progress"),
        Identifier.withDefaultNamespace("boss_bar/red_progress"),
        Identifier.withDefaultNamespace("boss_bar/green_progress"),
        Identifier.withDefaultNamespace("boss_bar/yellow_progress"),
        Identifier.withDefaultNamespace("boss_bar/purple_progress"),
        Identifier.withDefaultNamespace("boss_bar/white_progress")
    };
    private static final Identifier[] OVERLAY_BACKGROUND_SPRITES = {
        Identifier.withDefaultNamespace("boss_bar/notched_6_background"),
        Identifier.withDefaultNamespace("boss_bar/notched_10_background"),
        Identifier.withDefaultNamespace("boss_bar/notched_12_background"),
        Identifier.withDefaultNamespace("boss_bar/notched_20_background")
    };
    private static final Identifier[] OVERLAY_PROGRESS_SPRITES = {
        Identifier.withDefaultNamespace("boss_bar/notched_6_progress"),
        Identifier.withDefaultNamespace("boss_bar/notched_10_progress"),
        Identifier.withDefaultNamespace("boss_bar/notched_12_progress"),
        Identifier.withDefaultNamespace("boss_bar/notched_20_progress")
    };

    private static final Component PREVIEW_NAME = Component.literal("Wither");
    private static final float PREVIEW_PROGRESS = 0.6f;

    private final BossbarModule bossbarModule;

    public BossbarHudPanel(BossbarModule bossbarModule) {
        super(bossbarModule, "bossbar", BossbarModule.BOSS_BAR_WIDTH);
        this.bossbarModule = bossbarModule;
    }

    @Override
    protected @Nullable HudContent produceHudContent(float deltaSeconds, boolean editorMode) {
        return null;
    }

    @Override
    public @Nullable Runnable prepareAndDraw(GuiRenderer glRenderer, float deltaSeconds, boolean editorMode) {
        List<BossRow> rows = resolveRows(editorMode);
        if (rows == null) {
            return null;
        }

        boolean barHidden = bossbarModule.bossbarHudHideBarGraphic();
        float barWidth = BossbarModule.BOSS_BAR_WIDTH;
        float fontHeight = glRenderer.getFontHeight();
        float contentWidth = barWidth;
        for (BossRow row : rows) {
            contentWidth = Math.max(contentWidth, glRenderer.measureTextWidth(row.name()));
        }
        final float finalContentWidth = contentWidth;
        float rowContentHeight = barHidden ? fontHeight : fontHeight + NAME_BAR_GAP + BAR_HEIGHT;
        float padding = hudHostModule().getPadding();
        float layoutWidth = padding * 2f + finalContentWidth;
        float layoutHeight = padding * 2f + rows.size() * rowContentHeight + Math.max(0f, rows.size() - 1f) * ROW_GAP;

        getHudState().setLastLayoutWidth(layoutWidth);
        getHudState().setLastLayoutHeight(layoutHeight);
        getHudState().setCommittedLayoutWidth(layoutWidth);
        getHudState().setCommittedLayoutHeight(layoutHeight);

        List<BossRow> rowsCopy = List.copyOf(rows);
        boolean textShadow = hudHostModule().isTextShadowEnabled();
        return () -> {
            hudHostModule().drawHUDPanelBackground(glRenderer, layoutWidth, layoutHeight, editorMode);
            glRenderer.endRenderSegment();

            float barX = padding + (finalContentWidth - barWidth) * 0.5f;
            float rowTop = padding;

            for (BossRow row : rowsCopy) {
                int nameWidth = glRenderer.measureTextWidth(row.name());
                float nameX = padding + (finalContentWidth - nameWidth) * 0.5f;
                glRenderer.drawComponentText(row.name(), nameX, rowTop, 0xFFFFFFFF, textShadow);

                if (!barHidden) {
                    float barY = rowTop + fontHeight + NAME_BAR_GAP;

                    glRenderer.drawSprite(BAR_BACKGROUND_SPRITES[row.colorOrdinal()], barX, barY, barWidth, BAR_HEIGHT, 0xFFFFFFFF);
                    if (row.overlayOrdinal() > 0) {
                        glRenderer.drawSprite(OVERLAY_BACKGROUND_SPRITES[row.overlayOrdinal() - 1], barX, barY, barWidth, BAR_HEIGHT, 0xFFFFFFFF);
                    }

                    int progressWidth = Mth.lerpDiscrete(row.progress(), 0, (int) barWidth);
                    if (progressWidth > 0f) {
                        glRenderer.drawSprite(BAR_PROGRESS_SPRITES[row.colorOrdinal()], (int) barWidth, (int) BAR_HEIGHT, 0, 0, barX, barY, progressWidth, BAR_HEIGHT);
                        if (row.overlayOrdinal() > 0) {
                            glRenderer.drawSprite(OVERLAY_PROGRESS_SPRITES[row.overlayOrdinal() - 1], (int) barWidth, (int) BAR_HEIGHT, 0, 0, barX, barY, progressWidth, BAR_HEIGHT);
                        }
                    }
                }

                rowTop += rowContentHeight + ROW_GAP;
            }
        };
    }

    private @Nullable List<BossRow> resolveRows(boolean editorMode) {
        Map<UUID, BossEvent> events = ((BossHealthOverlayEventsAccessor) Minecraft.getInstance().gui.getBossOverlay()).getEvents();
        if (events.isEmpty()) {
            if (editorMode) {
                return List.of(new BossRow(PREVIEW_NAME, PREVIEW_PROGRESS,
                    BossEvent.BossBarColor.PURPLE.ordinal(),
                    BossEvent.BossBarOverlay.PROGRESS.ordinal()));
            }
            recordHudContentSkipped();
            return null;
        }

        int guiHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int maxY = guiHeight / 3;
        float fontHeight = Minecraft.getInstance().font.lineHeight;
        float stride = fontHeight + NAME_BAR_GAP + BAR_HEIGHT + ROW_GAP;

        List<BossRow> rows = new ArrayList<>(events.size());
        float yOffset = 12f;
        for (BossEvent event : events.values()) {
            if (yOffset >= maxY) break;
            rows.add(new BossRow(
                event.getName(),
                event.getProgress(),
                event.getColor().ordinal(),
                event.getOverlay().ordinal()
            ));
            yOffset += stride;
        }
        return rows.isEmpty() ? null : rows;
    }

    private record BossRow(Component name, float progress, int colorOrdinal, int overlayOrdinal) {}
}
