package cc.fascinated.fascinatedutils.oldgui.waypoints.components;

import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.oldgui.core.Align;
import cc.fascinated.fascinatedutils.oldgui.core.TextOverflow;
import cc.fascinated.fascinatedutils.oldgui.theme.UITheme;
import cc.fascinated.fascinatedutils.oldgui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.oldgui.widgets.*;
import cc.fascinated.fascinatedutils.systems.config.impl.waypoint.Waypoint;

/**
 * Renders one waypoint row: accent color bar, stacked name/coords/dimension labels, and three
 * icon action buttons.
 */
public class WaypointRowComponent {
    public static final float ROW_HEIGHT = 52f;
    private static final float COLOR_BAR_WIDTH = 6f;
    private static final float CONTENT_PAD = 8f;
    private static final float VERT_PAD = 6f;
    private static final float ACTION_BUTTON_SIZE = 20f;
    private static final float BUTTON_GAP = 3f;

    public static FWidget build(Props props) {
        Waypoint waypoint = props.waypoint();

        FIconActionButton editButton = new FIconActionButton();
        editButton.setIcon(ModUiTextures.EDIT);
        editButton.setTints(UITheme.COLOR_TEXT_DISABLED, UITheme.COLOR_TEXT_PRIMARY);
        editButton.setOnClick(props.onEdit());

        FIconActionButton visibilityButton = new FIconActionButton();
        visibilityButton.setTints(UITheme.COLOR_TEXT_DISABLED, UITheme.COLOR_TEXT_PRIMARY);
        visibilityButton.setOnClick(props.onToggleVisible());
        visibilityButton.setIconSupplier(() -> waypoint.isVisible() ? ModUiTextures.VISIBILITY : ModUiTextures.VISIBILITY_OFF);

        FIconActionButton deleteButton = new FIconActionButton();
        deleteButton.setIcon(ModUiTextures.TRASH);
        deleteButton.setTints(UITheme.COLOR_TEXT_DISABLED, 0xFFFF5555);
        deleteButton.setOnClick(props.onRequestDelete());

        boolean visible = waypoint.isVisible();
        int nameColorArgb = visible ? FascinatedGuiTheme.INSTANCE.textPrimary() : UITheme.COLOR_TEXT_DISABLED;
        int subColorArgb = visible ? FascinatedGuiTheme.INSTANCE.textMuted() : UITheme.COLOR_TEXT_DISABLED;
        String nameDisplay = waypoint.getName().isBlank() ? "(unnamed)" : waypoint.getName();
        String coordsDisplay = (int) waypoint.getX() + ", " + (int) waypoint.getY() + ", " + (int) waypoint.getZ();

        FRectWidget colorBar = new FRectWidget();
        colorBar.setFillColorArgb(waypoint.getColor().getResolvedArgb());
        colorBar.setCornerRadius(3f);

        FLabelWidget nameLabel = new FLabelWidget();
        nameLabel.setText(nameDisplay);
        nameLabel.setColorArgb(nameColorArgb);
        nameLabel.setTextBold(true);
        nameLabel.setOverflow(TextOverflow.ELLIPSIS);
        nameLabel.setAlignX(Align.START);

        FLabelWidget coordsLabel = new FLabelWidget();
        coordsLabel.setText(coordsDisplay);
        coordsLabel.setColorArgb(subColorArgb);
        coordsLabel.setOverflow(TextOverflow.ELLIPSIS);
        coordsLabel.setAlignX(Align.START);

        FLabelWidget dimensionLabel = new FLabelWidget();
        dimensionLabel.setText(props.dimensionLabelText());
        dimensionLabel.setColorArgb(subColorArgb);
        dimensionLabel.setOverflow(TextOverflow.ELLIPSIS);
        dimensionLabel.setAlignX(Align.START);

        FColumnWidget labelsColumn = new FColumnWidget(2f, Align.START);
        labelsColumn.addChild(nameLabel);
        labelsColumn.addChild(coordsLabel);
        labelsColumn.addChild(dimensionLabel);

        FCellConstraints colorBarConstraints = new FCellConstraints()
                .setMinWidth(COLOR_BAR_WIDTH).setMaxWidth(COLOR_BAR_WIDTH)
                .setMarginTop(2f).setMarginBottom(2f);
        FCellConstraints labelsConstraints = new FCellConstraints()
                .setGrowWeight(1f).setExpandHorizontal(true)
                .setMarginStart(CONTENT_PAD).setMarginEnd(CONTENT_PAD)
                .setMarginTop(VERT_PAD).setMarginBottom(VERT_PAD);
        FCellConstraints iconConstraints = new FCellConstraints()
                .setMinWidth(ACTION_BUTTON_SIZE).setMaxWidth(ACTION_BUTTON_SIZE)
                .setMinHeight(ACTION_BUTTON_SIZE).setMaxHeight(ACTION_BUTTON_SIZE)
                .setAlignVertical(Align.CENTER);
        FCellConstraints iconWithGap = new FCellConstraints()
                .setMinWidth(ACTION_BUTTON_SIZE).setMaxWidth(ACTION_BUTTON_SIZE)
                .setMinHeight(ACTION_BUTTON_SIZE).setMaxHeight(ACTION_BUTTON_SIZE)
                .setAlignVertical(Align.CENTER).setMarginStart(BUTTON_GAP);

        FRowWidget contentRow = new FRowWidget(0f, Align.CENTER);
        contentRow.addChild(colorBar, colorBarConstraints);
        contentRow.addChild(labelsColumn, labelsConstraints);
        contentRow.addChild(editButton, iconConstraints);
        contentRow.addChild(visibilityButton, iconWithGap);
        contentRow.addChild(deleteButton, iconWithGap);

        FRectWidget background = new FRectWidget();
        background.setFillColorArgb(UITheme.COLOR_BACKGROUND);
        background.setCornerRadius(5f);

        FAbsoluteStackWidget stack = new FAbsoluteStackWidget();
        stack.addChild(background);
        stack.addChild(contentRow);
        return stack;
    }

    /**
     * Props for {@link WaypointRowComponent}.
     *
     * @param waypoint           model row; read on every render for live visibility/color
     * @param onEdit             invoked when the edit icon is clicked
     * @param onToggleVisible    invoked when the visibility icon is clicked
     * @param onRequestDelete    invoked when the delete icon is clicked
     * @param dimensionLabelText pre-formatted dimension label text
     */
    public record Props(Waypoint waypoint, Runnable onEdit, Runnable onToggleVisible, Runnable onRequestDelete,
                        String dimensionLabelText) {}
}
