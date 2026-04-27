package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.common.color.SettingColor;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.ColorSetting;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.mixin.bossbar.BossHealthOverlayEventsAccessor;
import cc.fascinated.fascinatedutils.systems.hud.HUDWidgetAnchor;
import cc.fascinated.fascinatedutils.systems.hud.HudDefaults;
import cc.fascinated.fascinatedutils.systems.hud.HudModule;
import cc.fascinated.fascinatedutils.systems.hud.HudWidgetAppearanceBuilders;
import cc.fascinated.fascinatedutils.systems.hud.content.HudContent;
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

public class BossbarModule extends HudModule {

    private static final float BAR_WIDTH = 182f;
    private static final float BAR_HEIGHT = 5f;

    // Vanilla draws the name at (yOffset - 9) and the bar at yOffset,
    // so the gap between name bottom and bar top is exactly 0 visually.
    // We model that as NAME_BAR_GAP = 2 (a small visual breathing room
    // that reproduces the same pixel positions when fontHeight = 9).
    private static final float NAME_BAR_GAP = 2f;

    // Vanilla stride per row is yOffset += 10 + 9 = 19px.
    // rowContentHeight = fontHeight(9) + NAME_BAR_GAP(2) + BAR_HEIGHT(5) = 16.
    // So ROW_GAP must be 19 - 16 = 3 to match vanilla exactly.
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

    private final BooleanSetting hideBar = BooleanSetting.builder().id("hide_bar").defaultValue(false).categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();
    private final BooleanSetting showBackground = HudWidgetAppearanceBuilders.showBackground().defaultValue(false).build();
    private final ColorSetting backgroundColor = HudWidgetAppearanceBuilders.backgroundColor().defaultValue(SettingColor.fromArgb(0x55000000)).build();
    private final BooleanSetting roundedCorners = HudWidgetAppearanceBuilders.roundedCorners().build();
    private final SliderSetting roundingRadius = HudWidgetAppearanceBuilders.roundingRadius().build();
    private final BooleanSetting showBorder = HudWidgetAppearanceBuilders.showBorder().build();
    private final SliderSetting borderThickness = HudWidgetAppearanceBuilders.borderThickness().build();
    private final ColorSetting borderColor = HudWidgetAppearanceBuilders.borderColor().build();

    public BossbarModule() {
        super("bossbar", "Bossbar", BAR_WIDTH, HudDefaults.builder()
            .defaultState(true)
            .defaultAnchor(HUDWidgetAnchor.TOP)
            .defaultXOffset(0)
            .defaultYOffset(5)
            .defaultPadding(0f)
            .build());
        addSetting(hideBar);
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
    public @Nullable Runnable prepareAndDraw(GuiRenderer glRenderer, float deltaSeconds, boolean editorMode) {
        List<BossRow> rows = buildRows(editorMode);
        if (rows == null) {
            return null;
        }

        boolean barHidden = hideBar.isEnabled();
        float fontHeight = glRenderer.getFontHeight();
        float contentWidth = BAR_WIDTH;
        for (BossRow row : rows) {
            contentWidth = Math.max(contentWidth, glRenderer.measureTextWidth(row.name()));
        }
        final float finalContentWidth = contentWidth;
        // Height of a single row's content: name text + optional gap + bar.
        float rowContentHeight = barHidden ? fontHeight : fontHeight + NAME_BAR_GAP + BAR_HEIGHT;
        float padding = getPadding();
        float layoutWidth = padding * 2f + finalContentWidth;
        float layoutHeight = padding * 2f + rows.size() * rowContentHeight + Math.max(0f, rows.size() - 1f) * ROW_GAP;

        getHudState().setLastLayoutWidth(layoutWidth);
        getHudState().setLastLayoutHeight(layoutHeight);
        getHudState().setCommittedLayoutWidth(layoutWidth);
        getHudState().setCommittedLayoutHeight(layoutHeight);

        List<BossRow> rowsCopy = List.copyOf(rows);
        return () -> {
            drawHUDPanelBackground(glRenderer, layoutWidth, layoutHeight);
            glRenderer.endRenderSegment();

            float barX = padding + (finalContentWidth - BAR_WIDTH) * 0.5f;
            float rowTop = padding;

            for (BossRow row : rowsCopy) {
                // Center the name over the bar, matching vanilla's (screenWidth/2 - textWidth/2) centering.
                int nameWidth = glRenderer.measureTextWidth(row.name());
                float nameX = padding + (finalContentWidth - nameWidth) * 0.5f;
                glRenderer.drawComponentText(row.name(), nameX, rowTop, 0xFFFFFFFF, true);

                if (!barHidden) {
                    float barY = rowTop + fontHeight + NAME_BAR_GAP;

                    glRenderer.drawSprite(BAR_BACKGROUND_SPRITES[row.colorOrdinal()], barX, barY, BAR_WIDTH, BAR_HEIGHT, 0xFFFFFFFF);
                    if (row.overlayOrdinal() > 0) {
                        glRenderer.drawSprite(OVERLAY_BACKGROUND_SPRITES[row.overlayOrdinal() - 1], barX, barY, BAR_WIDTH, BAR_HEIGHT, 0xFFFFFFFF);
                    }

                    int progressWidth = Mth.lerpDiscrete(row.progress(), 0, (int) BAR_WIDTH);
                    if (progressWidth > 0f) {
                        glRenderer.drawSprite(BAR_PROGRESS_SPRITES[row.colorOrdinal()], (int) BAR_WIDTH, (int) BAR_HEIGHT, 0, 0, barX, barY, progressWidth, BAR_HEIGHT);
                        if (row.overlayOrdinal() > 0) {
                            glRenderer.drawSprite(OVERLAY_PROGRESS_SPRITES[row.overlayOrdinal() - 1], (int) BAR_WIDTH, (int) BAR_HEIGHT, 0, 0, barX, barY, progressWidth, BAR_HEIGHT);
                        }
                    }
                }

                // Advance to the next row. Total stride = rowContentHeight + ROW_GAP = 19px,
                // which matches vanilla's yOffset += 10 + 9.
                rowTop += rowContentHeight + ROW_GAP;
            }
        };
    }

    @Override
    protected @Nullable HudContent produceContent(float deltaSeconds, boolean editorMode) {
        return null;
    }

    private @Nullable List<BossRow> buildRows(boolean editorMode) {
        if (editorMode) {
            return List.of(new BossRow(PREVIEW_NAME, PREVIEW_PROGRESS,
                BossEvent.BossBarColor.PURPLE.ordinal(),
                BossEvent.BossBarOverlay.PROGRESS.ordinal()));
        }

        Map<UUID, BossEvent> events = ((BossHealthOverlayEventsAccessor) Minecraft.getInstance().gui.getBossOverlay()).getEvents();
        if (events.isEmpty()) {
            recordHudContentSkipped();
            return null;
        }

        // Vanilla stops rendering boss bars once yOffset reaches guiHeight / 3,
        // preventing bars from consuming more than the top third of the screen.
        int guiHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int maxY = guiHeight / 3;
        // Use the font's actual line height (9px) for the cutoff calculation,
        // not glRenderer.getFontHeight() which isn't available here.
        float fontHeight = Minecraft.getInstance().font.lineHeight;
        float stride = fontHeight + NAME_BAR_GAP + BAR_HEIGHT + ROW_GAP; // = 19px

        List<BossRow> rows = new ArrayList<>(events.size());
        float yOffset = 12f; // matches vanilla's initial yOffset value
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