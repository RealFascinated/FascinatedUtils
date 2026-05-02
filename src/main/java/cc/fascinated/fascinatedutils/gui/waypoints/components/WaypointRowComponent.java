package cc.fascinated.fascinatedutils.gui.waypoints.components;

import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.gui.core.Align;
import cc.fascinated.fascinatedutils.gui.core.TextOverflow;
import cc.fascinated.fascinatedutils.gui.declare.Ui;
import cc.fascinated.fascinatedutils.gui.declare.UiComponent;
import cc.fascinated.fascinatedutils.gui.declare.UiSlot;
import cc.fascinated.fascinatedutils.gui.declare.UiView;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FCellConstraints;
import cc.fascinated.fascinatedutils.gui.widgets.FIconActionButton;
import cc.fascinated.fascinatedutils.systems.config.impl.waypoint.Waypoint;

import java.util.List;

/**
 * Renders one waypoint row: accent color bar, stacked name/coords/dimension labels, and three
 * icon action buttons.
 */
public class WaypointRowComponent extends UiComponent<WaypointRowComponent.Props> {
    public static final float ROW_HEIGHT = 52f;
    private static final float COLOR_BAR_WIDTH = 6f;
    private static final float CONTENT_PAD = 8f;
    private static final float VERT_PAD = 6f;
    private static final float ACTION_BUTTON_SIZE = 20f;
    private static final float BUTTON_GAP = 3f;

    private final FIconActionButton editButton = new FIconActionButton();
    private final FIconActionButton visibilityButton = new FIconActionButton();
    private final FIconActionButton deleteButton = new FIconActionButton();

    public WaypointRowComponent() {
        editButton.setIcon(ModUiTextures.EDIT);
        editButton.setTints(UITheme.COLOR_TEXT_DISABLED, UITheme.COLOR_TEXT_PRIMARY);
        deleteButton.setIcon(ModUiTextures.TRASH);
        deleteButton.setTints(UITheme.COLOR_TEXT_DISABLED, 0xFFFF5555);
        visibilityButton.setTints(UITheme.COLOR_TEXT_DISABLED, UITheme.COLOR_TEXT_PRIMARY);
    }

    /**
     * Creates a keyed slot (cell pinned to {@link #ROW_HEIGHT}) to insert into a column; the
     * component itself renders a stack whose intrinsic height is zero, so callers rely on this
     * factory to size the cell correctly.
     *
     * @param slotKey stable key (typically the waypoint UUID)
     * @param props   waypoint + callbacks
     * @return keyed, height-constrained slot
     */
    public static UiSlot rowSlot(String slotKey, Props props) {
        return UiSlot.keyed(slotKey,
                new FCellConstraints().setMinHeight(ROW_HEIGHT).setMaxHeight(ROW_HEIGHT),
                view(props));
    }

    /**
     * Plain view node for the waypoint row; caller supplies their own slot wrapping.
     *
     * @param props waypoint + callbacks
     * @return view node
     */
    public static UiView view(Props props) {
        return Ui.component(WaypointRowComponent.class, WaypointRowComponent::new, props);
    }

    @Override
    public UiView render() {
        Props currentProps = props();
        Waypoint waypoint = currentProps.waypoint();

        editButton.setOnClick(currentProps.onEdit());
        visibilityButton.setOnClick(currentProps.onToggleVisible());
        deleteButton.setOnClick(currentProps.onRequestDelete());
        visibilityButton.setIconSupplier(() -> waypoint.isVisible() ? ModUiTextures.VISIBILITY : ModUiTextures.VISIBILITY_OFF);

        boolean visible = waypoint.isVisible();
        int nameColorArgb = visible ? FascinatedGuiTheme.INSTANCE.textPrimary() : UITheme.COLOR_TEXT_DISABLED;
        int subColorArgb = visible ? FascinatedGuiTheme.INSTANCE.textMuted() : UITheme.COLOR_TEXT_DISABLED;
        String nameDisplay = waypoint.getName().isBlank() ? "(unnamed)" : waypoint.getName();
        String coordsDisplay = (int) waypoint.getX() + ", " + (int) waypoint.getY() + ", " + (int) waypoint.getZ();

        UiView background = Ui.rectDecorated(UITheme.COLOR_BACKGROUND, 5f, null, null);

        UiView contentRow = Ui.row(0f, Align.CENTER, List.of(
                Ui.slot(fixedColumnConstraints(COLOR_BAR_WIDTH).setMarginTop(2f).setMarginBottom(2f),
                        Ui.rectDecorated(waypoint.getColor().getResolvedArgb(), 3f, null, null)),
                Ui.slot(new FCellConstraints()
                                .setGrowWeight(1f)
                                .setMarginStart(CONTENT_PAD)
                                .setMarginEnd(CONTENT_PAD)
                                .setMarginTop(VERT_PAD)
                                .setMarginBottom(VERT_PAD),
                        Ui.column(2f, Align.START, List.of(
                                UiSlot.of(Ui.label(nameDisplay, nameColorArgb, true, TextOverflow.ELLIPSIS, Align.START)),
                                UiSlot.of(Ui.label(coordsDisplay, subColorArgb, false, TextOverflow.ELLIPSIS, Align.START)),
                                UiSlot.of(Ui.label(currentProps.dimensionLabelText(), subColorArgb, false, TextOverflow.ELLIPSIS, Align.START))
                        ))),
                Ui.slot(iconButtonConstraints(), Ui.widgetSlot("waypoint-row.edit", editButton)),
                Ui.slot(iconButtonConstraints().setMarginStart(BUTTON_GAP), Ui.widgetSlot("waypoint-row.visibility", visibilityButton)),
                Ui.slot(iconButtonConstraints().setMarginStart(BUTTON_GAP), Ui.widgetSlot("waypoint-row.delete", deleteButton))
        ));

        return Ui.stackLayers(
                UiSlot.of(background),
                UiSlot.of(contentRow)
        );
    }

    private static FCellConstraints fixedColumnConstraints(float width) {
        return new FCellConstraints().setMinWidth(width).setMaxWidth(width);
    }

    private static FCellConstraints iconButtonConstraints() {
        return new FCellConstraints()
                .setMinWidth(ACTION_BUTTON_SIZE)
                .setMaxWidth(ACTION_BUTTON_SIZE)
                .setMinHeight(ACTION_BUTTON_SIZE)
                .setMaxHeight(ACTION_BUTTON_SIZE)
                .setAlignVertical(Align.CENTER);
    }

    /**
     * Props for {@link WaypointRowComponent}.
     *
     * @param waypoint            model row; read on every render for live visibility/color
     * @param onEdit              invoked when the edit icon is clicked
     * @param onToggleVisible     invoked when the visibility icon is clicked
     * @param onRequestDelete     invoked when the delete icon is clicked
     * @param dimensionLabelText  pre-formatted dimension label text
     */
    public record Props(Waypoint waypoint,
                        Runnable onEdit,
                        Runnable onToggleVisible,
                        Runnable onRequestDelete,
                        String dimensionLabelText) {
    }
}
