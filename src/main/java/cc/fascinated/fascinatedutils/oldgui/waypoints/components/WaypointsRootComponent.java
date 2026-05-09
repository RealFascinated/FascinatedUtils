package cc.fascinated.fascinatedutils.oldgui.waypoints.components;

import cc.fascinated.fascinatedutils.oldgui.core.*;
import cc.fascinated.fascinatedutils.oldgui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.oldgui.theme.UITheme;
import cc.fascinated.fascinatedutils.oldgui.waypoints.WaypointDeletePopupWidget;
import cc.fascinated.fascinatedutils.oldgui.widgets.*;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import cc.fascinated.fascinatedutils.systems.config.impl.waypoint.Waypoint;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Root widget for the waypoints overlay: owns the persistent search input, scroll offset, and
 * pending-delete state. Renders the backdrop, card stack, and (optionally) the delete popup.
 */
public class WaypointsRootComponent extends FWidget {
    private static final float CARD_PAD = 12f;

    private final FNodeRegistry nodes = new FNodeRegistry();
    private final FNodeWidget root;
    private final FOutlinedTextInputWidget searchInput;

    private final String worldKey;
    private final Runnable onRequestAdd;
    private final Runnable onClose;
    private final Consumer<Waypoint> onEditWaypoint;
    private final Consumer<Waypoint> onToggleVisibility;
    private final WaypointsListComponent.DimensionLabelFormatter dimensionLabelFormatter;

    private FState<Float> scrollOffsetRef;
    private FState<@Nullable UUID> pendingDeleteId;

    public WaypointsRootComponent(String worldKey, Runnable onRequestAdd, Runnable onClose,
                                  Consumer<Waypoint> onEditWaypoint, Consumer<Waypoint> onToggleVisibility,
                                  WaypointsListComponent.DimensionLabelFormatter dimensionLabelFormatter) {
        this.worldKey = worldKey;
        this.onRequestAdd = onRequestAdd;
        this.onClose = onClose;
        this.onEditWaypoint = onEditWaypoint;
        this.onToggleVisibility = onToggleVisibility;
        this.dimensionLabelFormatter = dimensionLabelFormatter;

        this.searchInput = new FOutlinedTextInputWidget(64, 24f,
                () -> Component.translatable("alumite.waypoints.search").getString());
        this.searchInput.setOnChange(value -> {
            if (scrollOffsetRef != null) scrollOffsetRef.set(0f);
        });

        this.root = new FNodeWidget(nodes.get("waypoints-root", this::buildRootWidget));
        addChild(root);
    }

    @Override
    public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
        root.layout(measure, lx, ly, lw, lh);
        nodes.gc();
    }

    public void dispose() {
        nodes.dispose();
    }

    private FWidget buildRootWidget(FWidgetNode.RenderContext ctx) {
        scrollOffsetRef = ctx.useState(0f);
        pendingDeleteId = ctx.useState(null);

        return new FWidget() {
            float lastWidth = Float.NaN;
            float lastHeight = Float.NaN;

            @Override
            public boolean fillsHorizontalInRow() {
                return true;
            }

            @Override
            public boolean fillsVerticalInColumn() {
                return true;
            }

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
                if (Math.abs(lw - lastWidth) > 0.5f || Math.abs(lh - lastHeight) > 0.5f || childrenView().isEmpty()) {
                    lastWidth = lw;
                    lastHeight = lh;
                    clearChildren();
                    addChild(buildSurface(lw, lh));
                }
                for (FWidget child : childrenView()) {
                    child.layout(measure, lx, ly, lw, lh);
                }
            }
        };
    }

    private FWidget buildSurface(float width, float height) {
        float cardWidth = Math.min(width - 60f, 540f);
        float cardHeight = Math.min(height - 60f, 520f);
        FCellConstraints horizontalCardPad = new FCellConstraints().setMarginStart(CARD_PAD).setMarginEnd(CARD_PAD);

        List<Waypoint> worldWaypoints = ModConfig.waypoints().getForWorld(worldKey);

        FAbsoluteStackWidget deckStack = new FAbsoluteStackWidget();

        FRectWidget backdrop = new FRectWidget();
        backdrop.setFillColorArgb(0xB0000000);
        deckStack.addChild(backdrop);

        FWidget card = buildCard(cardWidth, horizontalCardPad, worldWaypoints);
        deckStack.addChild(new FMaxCenterInsetsWidget(30f, 30f, cardWidth, cardHeight, card));

        UUID deletingId = pendingDeleteId.get();
        if (deletingId != null) {
            if (ModConfig.waypoints().findById(deletingId).isPresent()) {
                Waypoint waypointToDelete = ModConfig.waypoints().findById(deletingId).get();
                deckStack.addChild(new WaypointDeletePopupWidget(
                        waypointToDelete.getName(),
                        () -> pendingDeleteId.set(null),
                        () -> {
                            ModConfig.waypoints().delete(deletingId);
                            pendingDeleteId.set(null);
                        }));
            } else {
                pendingDeleteId.setQuiet(null);
            }
        }

        return deckStack;
    }

    private FWidget buildCard(float cardWidth, FCellConstraints horizontalCardPad, List<Waypoint> worldWaypoints) {
        FWidget list = WaypointsListComponent.build(worldWaypoints, searchInput.value(), scrollOffsetRef,
                onEditWaypoint, onToggleVisibility, waypointId -> pendingDeleteId.set(waypointId),
                dimensionLabelFormatter);

        FColumnWidget columnBody = new FColumnWidget(0f, Align.START);
        columnBody.addChild(new FSpacerWidget(cardWidth, 0f), new FCellConstraints().setMargins(0f, CARD_PAD));
        columnBody.addChild(WaypointsHeaderComponent.build(onRequestAdd, onClose),
                new FCellConstraints().setMarginStart(CARD_PAD).setMarginEnd(CARD_PAD));
        columnBody.addChild(new FSpacerWidget(cardWidth, 6f),
                new FCellConstraints().setMarginStart(CARD_PAD).setMarginEnd(CARD_PAD));
        columnBody.addChild(searchInput,
                new FCellConstraints().setMarginStart(CARD_PAD).setMarginEnd(CARD_PAD));
        columnBody.addChild(new FSpacerWidget(cardWidth, 8f),
                new FCellConstraints().setMarginStart(CARD_PAD).setMarginEnd(CARD_PAD));
        columnBody.addChild(list, new FCellConstraints().setExpandVertical(true));

        FRectWidget cardBg = new FRectWidget();
        cardBg.setFillColorArgb(UITheme.COLOR_SURFACE);
        cardBg.setCornerRadius(8f);
        cardBg.setBorder(UITheme.COLOR_BORDER, 1f);

        FAbsoluteStackWidget cardStack = new FAbsoluteStackWidget();
        cardStack.addChild(cardBg);
        cardStack.addChild(columnBody);
        return cardStack;
    }
}
