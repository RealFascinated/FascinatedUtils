package cc.fascinated.fascinatedutils.gui.waypoints.components;

import cc.fascinated.fascinatedutils.gui.core.Align;
import cc.fascinated.fascinatedutils.gui.core.FState;
import cc.fascinated.fascinatedutils.gui.core.TextOverflow;
import cc.fascinated.fascinatedutils.gui.widgets.FTheme;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FCellConstraints;
import cc.fascinated.fascinatedutils.gui.widgets.FColumnWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FLabelWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FScrollColumnWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import cc.fascinated.fascinatedutils.systems.config.impl.waypoint.Waypoint;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Builds a scrollable filtered list of waypoint rows. Filters by the current search query and
 * renders an empty-state label when there are no matches.
 */
public class WaypointsListComponent {

    public static FWidget build(List<Waypoint> waypoints, String searchQuery, FState<Float> scrollOffsetRef,
                                Consumer<Waypoint> onEdit, Consumer<Waypoint> onToggleVisible,
                                Consumer<UUID> onRequestDelete, DimensionLabelFormatter dimensionLabelFormatter) {
        String queryLower = searchQuery.trim().toLowerCase(Locale.ROOT);
        List<Waypoint> filtered = waypoints.stream()
                .filter(waypoint -> queryLower.isEmpty() || waypoint.getName().toLowerCase(Locale.ROOT).contains(queryLower))
                .toList();

        FColumnWidget body = new FColumnWidget(5f, Align.START);
        if (filtered.isEmpty()) {
            String emptyText = queryLower.isEmpty()
                    ? Component.translatable("fascinatedutils.waypoints.empty").getString()
                    : Component.translatable("fascinatedutils.waypoints.no_results").getString();
            FLabelWidget emptyLabel = new FLabelWidget();
            emptyLabel.setText(emptyText);
            emptyLabel.setColorArgb(FascinatedGuiTheme.INSTANCE.textMuted());
            emptyLabel.setOverflow(TextOverflow.VISIBLE);
            emptyLabel.setAlignX(Align.CENTER);
            body.addChild(emptyLabel);
        } else {
            for (Waypoint waypoint : filtered) {
                FWidget row = WaypointRowComponent.build(new WaypointRowComponent.Props(
                        waypoint,
                        () -> onEdit.accept(waypoint),
                        () -> onToggleVisible.accept(waypoint),
                        () -> onRequestDelete.accept(waypoint.getId()),
                        dimensionLabelFormatter.format(waypoint.getDimension())));
                body.addChild(row, new FCellConstraints()
                        .setMinHeight(WaypointRowComponent.ROW_HEIGHT)
                        .setMaxHeight(WaypointRowComponent.ROW_HEIGHT));
            }
        }

        FScrollColumnWidget scroll = FTheme.components().createScrollColumn(body, 3f);
        scroll.setFillVerticalInColumn(true);
        scroll.setScrollOffsetY(scrollOffsetRef.get());
        scroll.setScrollOffsetChangeListener(scrollOffsetRef::setQuiet);
        return scroll;
    }

    /**
     * Formatter for dimension ids so the component does not depend on how the parent wants to
     * display them.
     */
    @FunctionalInterface
    public interface DimensionLabelFormatter {
        String format(String dimensionId);
    }
}
