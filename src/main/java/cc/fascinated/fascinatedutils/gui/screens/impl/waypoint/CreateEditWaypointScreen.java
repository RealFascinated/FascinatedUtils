package cc.fascinated.fascinatedutils.gui.screens.impl.waypoint;

import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.common.color.Colors;
import cc.fascinated.fascinatedutils.common.color.SettingColor;
import cc.fascinated.fascinatedutils.gui.components.BackButton;
import cc.fascinated.fascinatedutils.gui.components.ColorPickerController;
import cc.fascinated.fascinatedutils.gui.components.PanelCard;
import cc.fascinated.fascinatedutils.gui.components.UiTranslations;
import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.layout.SpacerNode;
import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.gui.core.state.UiState;
import cc.fascinated.fascinatedutils.gui.node.control.ButtonNode;
import cc.fascinated.fascinatedutils.gui.node.display.CardNode;
import cc.fascinated.fascinatedutils.gui.node.display.RectNode;
import cc.fascinated.fascinatedutils.gui.node.display.TextNode;
import cc.fascinated.fascinatedutils.gui.node.input.TextInputNode;
import cc.fascinated.fascinatedutils.gui.node.input.TextParser;
import cc.fascinated.fascinatedutils.gui.screens.RootScreen;
import cc.fascinated.fascinatedutils.gui.theme.UiMetrics;
import cc.fascinated.fascinatedutils.gui.theme.UiTheme;
import cc.fascinated.fascinatedutils.systems.waypoint.Waypoint;
import cc.fascinated.fascinatedutils.systems.waypoint.WaypointRepository;
import cc.fascinated.fascinatedutils.systems.waypoint.WaypointType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

/**
 * Form for creating a waypoint at the player's position or editing an existing one. Escape and the back button return
 * to the screen that opened the form.
 */
public class CreateEditWaypointScreen extends RootScreen {

    private static final int WIDTH = 500;
    private static final int CONTENT_PADDING = UiMetrics.SPACE_MD;
    private static final int FIELD_GAP = UiMetrics.SPACE_SM;
    private static final int LABEL_HEIGHT = 12;
    private static final int INPUT_HEIGHT = UiMetrics.CONTROL_HEIGHT;
    private static final int SWATCH_SIZE = 12;
    private static final int FIELD_HEIGHT = LABEL_HEIGHT + UiMetrics.SPACE_XS + INPUT_HEIGHT;
    private static final int HEIGHT = 2 * UiMetrics.HEADER_HEIGHT + 2 * CONTENT_PADDING + 2 * FIELD_HEIGHT + FIELD_GAP;
    private static final int INNER_WIDTH = WIDTH - 2 * CONTENT_PADDING;
    private static final int COLUMN_WIDTH = (INNER_WIDTH - 3 * FIELD_GAP) / 4;
    private static final int MAX_NAME_LENGTH = 32;

    private final Type type;
    private final Waypoint editWaypoint;
    private final String initialName;
    private final double initialX;
    private final double initialY;
    private final double initialZ;
    private final SettingColor initialColor;
    private final ColorPickerController colorPicker = new ColorPickerController(stateStore, "waypoint.colorpicker.open");

    /**
     * Creates the form with no screen to return to; escape closes it.
     *
     * @param type     whether a waypoint is created or edited
     * @param waypoint the waypoint to edit, ignored when creating
     */
    public CreateEditWaypointScreen(Type type, @Nullable Waypoint waypoint) {
        this(type, waypoint, null);
    }

    /**
     * Creates the form.
     *
     * @param type     whether a waypoint is created or edited
     * @param waypoint the waypoint to edit, ignored when creating
     * @param parent   the screen to return to, or null to close the screen
     */
    public CreateEditWaypointScreen(Type type, @Nullable Waypoint waypoint, @Nullable Screen parent) {
        super(Component.translatable(type.getTitleKey()), parent);
        this.type = type;
        this.editWaypoint = waypoint;
        LocalPlayer player = Minecraft.getInstance().player;
        if (type == Type.CREATE || waypoint == null) {
            this.initialName = "";
            this.initialX = player == null ? 0 : Math.floor(player.getX());
            this.initialY = player == null ? 0 : Math.floor(player.getY());
            this.initialZ = player == null ? 0 : Math.floor(player.getZ());
            this.initialColor = new SettingColor();
            this.initialColor.random(); // New waypoints start with a random color so they are distinguishable by default
        }
        else {
            this.initialName = waypoint.getName();
            this.initialX = waypoint.getX();
            this.initialY = waypoint.getY();
            this.initialZ = waypoint.getZ();
            this.initialColor = waypoint.getColor().copy();
        }
    }

    @Override
    public void tick() {
        if (Minecraft.getInstance().player == null) {
            Minecraft.getInstance().setScreen(null);
        }
    }

    @Override
    protected void onEscapePressed() {
        if (colorPicker.isOpen()) {
            colorPicker.close();
        }
        else {
            super.onEscapePressed();
        }
    }

    @Override
    protected UiNode composeContent() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return new PositionedNode<>().full();
        }
        int minY = player.level().getMinY();
        int maxY = player.level().getMaxY();

        UiState<String> nameState = stateStore.state("waypoint.create.name", initialName);
        UiState<Double> xState = stateStore.state("waypoint.create.x", initialX);
        UiState<Double> yState = stateStore.state("waypoint.create.y", initialY);
        UiState<Double> zState = stateStore.state("waypoint.create.z", initialZ);
        UiState<SettingColor> colorState = stateStore.state("waypoint.create.color", initialColor);

        TextInputNode<String> nameInput = new TextInputNode<>(TextParser.STRING).setMaxLength(MAX_NAME_LENGTH).setOnChange(nameState::set).setValue(nameState.get());
        TextInputNode<Double> xInput = new TextInputNode<>(TextParser.DOUBLE).setOnChange(xState::set).setValue(xState.get());
        TextInputNode<Double> yInput = new TextInputNode<>(TextParser.DOUBLE).setOnChange(yState::set).setValue(yState.get());
        TextInputNode<Double> zInput = new TextInputNode<>(TextParser.DOUBLE).setOnChange(zState::set).setValue(zState.get());

        PositionedNode<?> colorRow = new PositionedNode<>().fullWidth().height(INPUT_HEIGHT);
        colorRow.addChild(new RectNode()
                .size(SWATCH_SIZE, SWATCH_SIZE)
                .setCornerRadius(3)
                .setFillSupplier(() -> colorState.get().getResolvedArgb())
                .setBorderResolver(theme -> theme.inputBorder())
                .alignY(0.5f));
        colorRow.addChild(new ButtonNode()
                .setLabel(() -> Colors.rgbHex(colorState.get().getPackedArgb()))
                .left(SWATCH_SIZE + UiMetrics.SPACE_XS)
                .right(0)
                .setOnPress(() -> colorPicker.open(colorState.get(), colorState::set)));

        CardNode card = new PanelCard()
                .setTitleHeader(() -> UiTranslations.INSTANCE.get(type.getTitleKey()),
                        header -> header.addChild(new BackButton(this::returnToParent)))
                .width(WIDTH)
                .height(HEIGHT)
                .setContents(content -> {
                    content.margin(CONTENT_PADDING);
                    content.columnGap(FIELD_GAP);

                    content.addChild(field("alumite.waypoints.create.name", nameInput, INNER_WIDTH));
                    PositionedNode<?> coordinateRow = new PositionedNode<>().fullWidth().height(FIELD_HEIGHT).rowGap(FIELD_GAP);
                    coordinateRow.addChild(field("alumite.waypoints.coord.x", xInput, COLUMN_WIDTH));
                    coordinateRow.addChild(field("alumite.waypoints.coord.y", yInput, COLUMN_WIDTH));
                    coordinateRow.addChild(field("alumite.waypoints.coord.z", zInput, COLUMN_WIDTH));
                    coordinateRow.addChild(field("alumite.waypoints.color", colorRow, COLUMN_WIDTH));
                    content.addChild(coordinateRow);
                })
                .setFooter(footer -> {
                    footer.right(UiMetrics.SPACE_XS);
                    footer.addChild(new TextNode(() -> validationMessage(nameState.get(), xState.get(), yState.get(), zState.get(), minY, maxY))
                            .setColorResolver(UiTheme::danger)
                            .alignY(0.5f));
                    footer.addChild(new SpacerNode());
                    footer.addChild(new ButtonNode()
                            .setLeftIcon(ModUiTextures.SAVE.getId())
                            .setLabel(UiTranslations.INSTANCE.get("alumite.waypoints.save"))
                            .setVariant(ButtonNode.ButtonVariant.PRIMARY)
                            .setDisabled(() -> !isSavable(nameState.get(), xState.get(), zState.get()))
                            .setOnPress(() -> save(player, nameState.get().trim(), xState.get(), Mth.clamp(yState.get(), minY, maxY), zState.get(), colorState.get())));
                })
                .center();

        PositionedNode<?> root = new PositionedNode<>().full();
        root.addChild(card);
        UiNode colorPickerPopup = colorPicker.popupOrNull();
        if (colorPickerPopup != null) {
            root.addChild(colorPickerPopup);
        }
        return root;
    }

    private void returnToParent() {
        super.onEscapePressed();
    }

    private void save(LocalPlayer player, String name, double x, double y, double z, SettingColor color) {
        if (type == Type.CREATE || editWaypoint == null) {
            WaypointRepository.create(name, WaypointType.NORMAL, x, y, z, player.level().dimension().identifier().toString(), color);
        }
        else {
            editWaypoint.setName(name);
            editWaypoint.setX(x);
            editWaypoint.setY(y);
            editWaypoint.setZ(z);
            editWaypoint.setColor(color.copy());
            WaypointRepository.save();
        }
        returnToParent();
    }

    private static boolean isSavable(String name, double x, double z) {
        return !name.isBlank() && Double.isFinite(x) && Double.isFinite(z);
    }

    private static String validationMessage(String name, double x, double y, double z, int minY, int maxY) {
        if (name.isBlank()) {
            return UiTranslations.INSTANCE.get("alumite.waypoints.error.name_required");
        }
        if (!Double.isFinite(x) || !Double.isFinite(z)) {
            return UiTranslations.INSTANCE.get("alumite.waypoints.error.coordinates");
        }
        if (y < minY || y > maxY) {
            return Component.translatable("alumite.waypoints.error.y_range", minY, maxY).getString();
        }
        return "";
    }

    private PositionedNode<?> field(String labelKey, PositionedNode<?> control, int width) {
        PositionedNode<?> column = new PositionedNode<>().width(width).height(FIELD_HEIGHT).columnGap(UiMetrics.SPACE_XS);
        column.addChild(new TextNode(() -> UiTranslations.INSTANCE.get(labelKey))
                .setColorResolver(UiTheme::textMuted)
                .setTextAlign(0f, 0.5f)
                .fullWidth().height(LABEL_HEIGHT));
        control.fullWidth().height(INPUT_HEIGHT);
        column.addChild(control);
        return column;
    }

    @Getter
    @AllArgsConstructor
    public enum Type {
        CREATE("alumite.waypoints.create.title"),
        EDIT("alumite.waypoints.edit.title");

        private final String titleKey;
    }
}
