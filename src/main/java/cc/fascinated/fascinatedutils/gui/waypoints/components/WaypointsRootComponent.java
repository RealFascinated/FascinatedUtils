package cc.fascinated.fascinatedutils.gui.waypoints.components;

import cc.fascinated.fascinatedutils.gui.core.Align;
import cc.fascinated.fascinatedutils.gui.core.GuiFocusState;
import cc.fascinated.fascinatedutils.gui.core.Ref;
import cc.fascinated.fascinatedutils.gui.declare.Ui;
import cc.fascinated.fascinatedutils.gui.declare.UiComponent;
import cc.fascinated.fascinatedutils.gui.declare.UiSlot;
import cc.fascinated.fascinatedutils.gui.declare.UiView;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.widgets.FCellConstraints;
import cc.fascinated.fascinatedutils.gui.widgets.FOutlinedTextInputWidget;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import cc.fascinated.fascinatedutils.systems.config.impl.waypoint.Waypoint;
import net.minecraft.network.chat.Component;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Root component for the waypoints screen: owns the persisted search input, the scroll offset,
 * and the pending-delete state. Renders the overlay/card stack and (optionally) the delete popup.
 */
public class WaypointsRootComponent extends UiComponent<WaypointsRootComponent.Props> {
    private static final int FOCUS_SEARCH = 6220;
    private static final float CARD_PAD = 12f;

    private final Ref<Float> scrollOffsetRef = Ref.of(0f);
    private final FOutlinedTextInputWidget searchInput;
    private @Nullable UUID pendingDeleteId;

    public WaypointsRootComponent() {
        this.searchInput = new FOutlinedTextInputWidget(FOCUS_SEARCH, 64, 24f,
                () -> Component.translatable("fascinatedutils.waypoints.search").getString());
        this.searchInput.setExternalFocusIdSupplier(GuiFocusState::getFocusedId);
        this.searchInput.setOnChange(value -> scrollOffsetRef.setValue(0f));
    }

    @Override
    public UiView render() {
        Props currentProps = props();
        float viewportWidth = currentProps.viewportWidth();
        float viewportHeight = currentProps.viewportHeight();

        float cardWidth = Math.min(viewportWidth - 60f, 540f);
        float cardHeight = Math.min(viewportHeight - 60f, 520f);
        FCellConstraints horizontalCardPad = new FCellConstraints().setMarginStart(CARD_PAD).setMarginEnd(CARD_PAD);

        List<UiSlot> deckLayers = new ArrayList<>();
        deckLayers.add(UiSlot.of(Ui.rectPlain(0xB0000000)));
        deckLayers.add(UiSlot.of(Ui.centerMax(30f, 30f, cardWidth, cardHeight, cardStack(cardWidth, horizontalCardPad, currentProps))));

        if (pendingDeleteId != null) {
            UUID idForDelete = pendingDeleteId;
            Optional<Waypoint> waypointOptional = ModConfig.waypoints().findById(idForDelete);
            if (waypointOptional.isPresent()) {
                deckLayers.add(UiSlot.keyed("waypoints.delete_popup",
                        WaypointDeletePopupComponent.view(new WaypointDeletePopupComponent.Props(
                                waypointOptional.get().getName(),
                                () -> pendingDeleteId = null,
                                () -> {
                                    ModConfig.waypoints().delete(idForDelete);
                                    pendingDeleteId = null;
                                }))));
            }
            else {
                pendingDeleteId = null;
            }
        }

        return Ui.stackLayers(deckLayers);
    }

    private UiView cardStack(float cardWidth, FCellConstraints horizontalCardPad, Props currentProps) {
        UiView columnBody = Ui.column(0f, Align.START, List.of(
                Ui.slot(new FCellConstraints().setMargins(0f, CARD_PAD), Ui.spacer(cardWidth, 0f)),
                Ui.slot(horizontalCardPad, WaypointsHeaderComponent.view(
                        new WaypointsHeaderComponent.Props(currentProps.onRequestAdd(), currentProps.onClose()))),
                Ui.slot(horizontalCardPad, Ui.spacer(cardWidth, 6f)),
                Ui.slot(horizontalCardPad, Ui.outlinedPinned(searchInput, null, value -> scrollOffsetRef.setValue(0f))),
                Ui.slot(horizontalCardPad, Ui.spacer(cardWidth, 8f)),
                Ui.slot(new FCellConstraints().setExpandVertical(true),
                        WaypointsListComponent.view(new WaypointsListComponent.Props(
                                currentProps.worldWaypoints(),
                                searchInput.value(),
                                scrollOffsetRef,
                                currentProps.onEditWaypoint(),
                                currentProps.onToggleVisibility(),
                                waypointId -> pendingDeleteId = waypointId,
                                currentProps.dimensionLabelFormatter())))
        ));
        return Ui.stackLayers(
                UiSlot.of(Ui.rectDecorated(UITheme.COLOR_SURFACE, 8f, UITheme.COLOR_BORDER, 1f)),
                UiSlot.of(columnBody));
    }

    @Override
    protected void onUnmount() {
        pendingDeleteId = null;
    }

    /**
     * Props for {@link WaypointsRootComponent}.
     */
    public record Props(float viewportWidth,
                        float viewportHeight,
                        List<Waypoint> worldWaypoints,
                        Runnable onRequestAdd,
                        Runnable onClose,
                        java.util.function.Consumer<Waypoint> onEditWaypoint,
                        java.util.function.Consumer<Waypoint> onToggleVisibility,
                        WaypointsListComponent.DimensionLabelFormatter dimensionLabelFormatter) {
    }
}
