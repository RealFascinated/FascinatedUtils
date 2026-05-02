package cc.fascinated.fascinatedutils.gui.waypoints.components;

import cc.fascinated.fascinatedutils.gui.core.Align;
import cc.fascinated.fascinatedutils.gui.core.Ref;
import cc.fascinated.fascinatedutils.gui.core.TextOverflow;
import cc.fascinated.fascinatedutils.gui.declare.Ui;
import cc.fascinated.fascinatedutils.gui.declare.UiComponent;
import cc.fascinated.fascinatedutils.gui.declare.UiSlot;
import cc.fascinated.fascinatedutils.gui.declare.UiView;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.systems.config.impl.waypoint.Waypoint;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Scrollable filtered list of waypoint rows. Filters by the current search query and renders an
 * empty-state label when there are no matches.
 */
public class WaypointsListComponent extends UiComponent<WaypointsListComponent.Props> {
    public static UiView view(Props props) {
        return Ui.component(WaypointsListComponent.class, WaypointsListComponent::new, props);
    }

    @Override
    public UiView render() {
        Props currentProps = props();
        String queryLower = currentProps.searchQuery().trim().toLowerCase(Locale.ROOT);
        List<Waypoint> filtered = currentProps.waypoints().stream()
                .filter(waypoint -> queryLower.isEmpty() || waypoint.getName().toLowerCase(Locale.ROOT).contains(queryLower))
                .toList();

        List<UiSlot> bodySlots = new ArrayList<>();
        if (filtered.isEmpty()) {
            String emptyText = queryLower.isEmpty()
                    ? Component.translatable("fascinatedutils.waypoints.empty").getString()
                    : Component.translatable("fascinatedutils.waypoints.no_results").getString();
            bodySlots.add(UiSlot.of(Ui.label(emptyText,
                    FascinatedGuiTheme.INSTANCE.textMuted(), false, TextOverflow.VISIBLE, Align.CENTER)));
        }
        else {
            for (Waypoint waypoint : filtered) {
                bodySlots.add(WaypointRowComponent.rowSlot(waypoint.getId().toString(),
                        new WaypointRowComponent.Props(
                                waypoint,
                                () -> currentProps.onEdit().accept(waypoint),
                                () -> currentProps.onToggleVisible().accept(waypoint),
                                () -> currentProps.onRequestDelete().accept(waypoint.getId()),
                                currentProps.dimensionLabelFormatter().format(waypoint.getDimension()))));
            }
        }

        return Ui.scrollTracked(3f, 5f, true,
                scrollOffset -> currentProps.scrollOffsetRef().setValue(scrollOffset),
                currentProps.scrollOffsetRef(),
                bodySlots);
    }

    /**
     * Formatter for dimension ids so the component does not depend on how the parent wants to
     * display them.
     */
    @FunctionalInterface
    public interface DimensionLabelFormatter {
        String format(String dimensionId);
    }

    /**
     * Props for {@link WaypointsListComponent}.
     *
     * @param waypoints                all waypoints for the current world
     * @param searchQuery              current search query (case-insensitive contains filter)
     * @param scrollOffsetRef          shared scroll position, reset to zero on search change
     * @param onEdit                   invoked with the waypoint when the edit icon is clicked
     * @param onToggleVisible          invoked with the waypoint when the visibility icon is clicked
     * @param onRequestDelete          invoked with the waypoint id when the delete icon is clicked
     * @param dimensionLabelFormatter  converts dimension ids to display labels
     */
    public record Props(List<Waypoint> waypoints,
                        String searchQuery,
                        Ref<Float> scrollOffsetRef,
                        Consumer<Waypoint> onEdit,
                        Consumer<Waypoint> onToggleVisible,
                        Consumer<UUID> onRequestDelete,
                        DimensionLabelFormatter dimensionLabelFormatter) {
    }
}
