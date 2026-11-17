package cc.fascinated.fascinatedutils.systems.modules.impl.bossbar.hud;

import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.gui.node.display.RichTextNode;
import cc.fascinated.fascinatedutils.gui.node.layout.ColumnNode;
import cc.fascinated.fascinatedutils.gui.node.display.SpriteProgressBarNode;
import cc.fascinated.fascinatedutils.mixin.bossbar.BossHealthOverlayEventsAccessor;
import cc.fascinated.fascinatedutils.systems.hud.panel.HudComposeContext;
import cc.fascinated.fascinatedutils.systems.hud.layout.HudLayoutCanvas;
import cc.fascinated.fascinatedutils.systems.hud.panel.HudPanel;
import cc.fascinated.fascinatedutils.systems.modules.impl.bossbar.BossbarModule;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.BossEvent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BossbarHudPanel extends HudPanel {

    private static final int BAR_WIDTH = (int) BossbarModule.BOSS_BAR_WIDTH;
    private static final int BAR_HEIGHT = 5;
    private static final int NAME_BAR_GAP = 2;
    private static final int ROW_GAP = 3;

    private static final Identifier[] BAR_BACKGROUND_SPRITES = {Identifier.withDefaultNamespace("boss_bar/pink_background"), Identifier.withDefaultNamespace("boss_bar/blue_background"), Identifier.withDefaultNamespace("boss_bar/red_background"), Identifier.withDefaultNamespace("boss_bar/green_background"), Identifier.withDefaultNamespace("boss_bar/yellow_background"), Identifier.withDefaultNamespace("boss_bar/purple_background"), Identifier.withDefaultNamespace("boss_bar/white_background")};
    private static final Identifier[] BAR_PROGRESS_SPRITES = {Identifier.withDefaultNamespace("boss_bar/pink_progress"), Identifier.withDefaultNamespace("boss_bar/blue_progress"), Identifier.withDefaultNamespace("boss_bar/red_progress"), Identifier.withDefaultNamespace("boss_bar/green_progress"), Identifier.withDefaultNamespace("boss_bar/yellow_progress"), Identifier.withDefaultNamespace("boss_bar/purple_progress"), Identifier.withDefaultNamespace("boss_bar/white_progress")};
    private static final Identifier[] OVERLAY_BACKGROUND_SPRITES = {Identifier.withDefaultNamespace("boss_bar/notched_6_background"), Identifier.withDefaultNamespace("boss_bar/notched_10_background"), Identifier.withDefaultNamespace("boss_bar/notched_12_background"), Identifier.withDefaultNamespace("boss_bar/notched_20_background")};
    private static final Identifier[] OVERLAY_PROGRESS_SPRITES = {Identifier.withDefaultNamespace("boss_bar/notched_6_progress"), Identifier.withDefaultNamespace("boss_bar/notched_10_progress"), Identifier.withDefaultNamespace("boss_bar/notched_12_progress"), Identifier.withDefaultNamespace("boss_bar/notched_20_progress")};

    private static final float PREVIEW_PROGRESS = 0.6f;

    private final BossbarModule bossbarModule;

    public BossbarHudPanel(BossbarModule bossbarModule) {
        super(bossbarModule, "bossbar", BossbarModule.BOSS_BAR_WIDTH);
        this.bossbarModule = bossbarModule;
    }

    @Override
    protected @Nullable UiNode compose(HudComposeContext context) {
        List<BossRow> rows = resolveRows(context.editorMode());
        if (rows.isEmpty()) {
            return null;
        }
        boolean barHidden = bossbarModule.bossbarHudHideBarGraphic();
        ColumnNode column = new ColumnNode(ROW_GAP);
        for (BossRow row : rows) {
            ColumnNode rowColumn = new ColumnNode(NAME_BAR_GAP).alignX(0.5f);
            rowColumn.addChild(RichTextNode.component(row.name(), context.textShadow()).alignX(0.5f));
            if (!barHidden) {
                SpriteProgressBarNode bar = new SpriteProgressBarNode(() -> row.progress())
                        .addTrackSprite(BAR_BACKGROUND_SPRITES[row.colorOrdinal()])
                        .addFillSprite(BAR_PROGRESS_SPRITES[row.colorOrdinal()])
                        .alignX(0.5f);
                if (row.overlayOrdinal() > 0) {
                    bar.addTrackSprite(OVERLAY_BACKGROUND_SPRITES[row.overlayOrdinal() - 1]);
                    bar.addFillSprite(OVERLAY_PROGRESS_SPRITES[row.overlayOrdinal() - 1]);
                }
                rowColumn.addChild(bar.size(BAR_WIDTH, BAR_HEIGHT));
            }
            column.addChild(rowColumn);
        }
        return column;
    }

    private List<BossRow> resolveRows(boolean editorMode) {
        Minecraft minecraft = Minecraft.getInstance();
        Map<UUID, BossEvent> events = ((BossHealthOverlayEventsAccessor) minecraft.gui.getBossOverlay()).getEvents();
        if (events.isEmpty()) {
            if (editorMode) {
                return List.of(new BossRow(Component.literal(hudText("preview_name")), PREVIEW_PROGRESS, BossEvent.BossBarColor.PURPLE.ordinal(), BossEvent.BossBarOverlay.PROGRESS.ordinal()));
            }
            return List.of();
        }

        // Vanilla stops stacking bars a third of the way down the screen; measure that in HUD canvas pixels.
        float maxY = HudLayoutCanvas.height() / 3f;
        float stride = minecraft.font.lineHeight + NAME_BAR_GAP + BAR_HEIGHT + ROW_GAP;
        List<BossRow> newRows = new ArrayList<>();
        float yOffset = 12f;
        for (BossEvent event : events.values()) {
            if (yOffset >= maxY) {
                break;
            }
            newRows.add(new BossRow(event.getName(), event.getProgress(), event.getColor().ordinal(), event.getOverlay().ordinal()));
            yOffset += stride;
        }
        return newRows;
    }

    private record BossRow(Component name, float progress, int colorOrdinal, int overlayOrdinal) {}
}
