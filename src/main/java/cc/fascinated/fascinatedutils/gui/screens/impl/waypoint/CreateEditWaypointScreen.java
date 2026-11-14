package cc.fascinated.fascinatedutils.gui.screens.impl.waypoint;

import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.common.color.SettingColor;
import cc.fascinated.fascinatedutils.gui.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui.core.SpacerNode;
import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.gui.core.UiState;
import cc.fascinated.fascinatedutils.gui.node.ButtonNode;
import cc.fascinated.fascinatedutils.gui.node.CardNode;
import cc.fascinated.fascinatedutils.gui.node.ColorPickerPopupNode;
import cc.fascinated.fascinatedutils.gui.node.RectNode;
import cc.fascinated.fascinatedutils.gui.node.TextNode;
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
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

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

    private TextInputNode<String> nameInput;
    private TextInputNode<Double> xInput;
    private TextInputNode<Double> yInput;
    private TextInputNode<Double> zInput;
    private ColorPickerPopupNode colorPickerPopup;

    private final Type type;
    private final Waypoint editWaypoint;

    public CreateEditWaypointScreen(Type type, Waypoint waypoint) {
        super(Component.translatable(type.getTitleKey()));
        this.type = type;
        this.editWaypoint = waypoint;
    }

    @Override
    protected UiNode createRootNode() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        assert player != null;

        UiState<String> nameState = stateStore.state("waypoint.create.name", type == Type.CREATE ? "" : editWaypoint.getName());
        UiState<Double> xState = stateStore.state("waypoint.create.x", type == Type.CREATE ? Math.floor(player.getX()) : editWaypoint.getX());
        UiState<Double> yState = stateStore.state("waypoint.create.y", type == Type.CREATE ? Math.floor(player.getY()) : editWaypoint.getY());
        UiState<Double> zState = stateStore.state("waypoint.create.z", type == Type.CREATE ? Math.floor(player.getZ()) : editWaypoint.getZ());
        UiState<SettingColor> colorState = stateStore.state("waypoint.create.color", type == Type.CREATE ? new SettingColor() : editWaypoint.getColor().copy());

        UiState<Boolean> colorPickerOpen = stateStore.state("waypoint.colorpicker.open", false);

        if (nameInput == null) {
            nameInput = new TextInputNode<>(TextParser.STRING).setOnChange(nameState::set).setValue(nameState.get());
            xInput = new TextInputNode<>(TextParser.DOUBLE).setOnChange(xState::set).setValue(xState.get());
            yInput = new TextInputNode<>(TextParser.DOUBLE).setOnChange(yState::set).setValue(yState.get());
            zInput = new TextInputNode<>(TextParser.DOUBLE).setOnChange(zState::set).setValue(zState.get());
            if (type == Type.CREATE) {
                colorState.get().random(); // Randomize color for new waypoints to make them more distinguishable by default
            }
        }
        if (colorPickerPopup == null) {
            colorPickerPopup = new ColorPickerPopupNode(
                    colorState.get().copy(),
                    newColor -> { colorState.set(newColor); colorPickerOpen.set(false); },
                    () -> colorPickerOpen.set(false));
        }

        PositionedNode<?> colorRow = new PositionedNode<>().fullWidth().height(INPUT_HEIGHT);
        colorRow.addChild(new RectNode()
                .size(SWATCH_SIZE, SWATCH_SIZE)
                .setCornerRadius(3)
                .setFillSupplier(() -> colorState.get().getResolvedArgb())
                .setBorderResolver(theme -> theme.inputBorder())
                .alignY(0.5f));
        colorRow.addChild(new ButtonNode()
                .setLabel(() -> "#" + String.format("%06X", colorState.get().getPackedArgb() & 0xFFFFFF))
                .setRounded(true)
                .left(SWATCH_SIZE + UiMetrics.SPACE_XS)
                .right(0)
                .setOnPress(() -> {
                    colorPickerPopup.initFromColor(colorState.get());
                    colorPickerOpen.set(true);
                }));

        CardNode card = new CardNode()
                .width(WIDTH)
                .height(HEIGHT)
                .setFillResolver(UiTheme::panelFill)
                .setBorderResolver(UiTheme::borderSubtle)
                .setHeader(header -> {
                    header.addChild(new TextNode(() -> Component.translatable(type.getTitleKey()).getString()).setBold(true));

                    header.addChild(new SpacerNode());

                    header.right(UiMetrics.SPACE_XS);
                    header.addChild(new ButtonNode()
                            .setIconCenter(ModUiTextures.BACK.getId())
                            .setRounded(true)
                            .setVariant(ButtonNode.ButtonVariant.GHOST)
                            .setOnPress(() -> Minecraft.getInstance().setScreen(new WaypointsScreen())));
                })
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
                    footer.addChild(new SpacerNode());
                    footer.addChild(new ButtonNode()
                            .setLeftIcon(ModUiTextures.SAVE.getId())
                            .setLabel(Component.translatable("alumite.waypoints.save").getString())
                            .setRounded(true)
                            .setVariant(ButtonNode.ButtonVariant.PRIMARY)
                            .setDisabled(() -> nameState.get().isBlank())
                            .setOnPress(() -> {
                                if (type == Type.CREATE) {
                                    WaypointRepository.create(nameState.get(), WaypointType.NORMAL, xState.get(), yState.get(),
                                            zState.get(), player.level().dimension().identifier().toString(), colorState.get());
                                } else {
                                    editWaypoint.setName(nameState.get());
                                    editWaypoint.setX(xState.get());
                                    editWaypoint.setY(yState.get());
                                    editWaypoint.setZ(zState.get());
                                    editWaypoint.setColor(colorState.get().copy());
                                    WaypointRepository.save();
                                }
                                Minecraft.getInstance().setScreen(new WaypointsScreen());
                            }));
                })
                .center();

        PositionedNode<?> root = new PositionedNode<>().full();
        root.addChild(card);
        if (colorPickerOpen.get()) {
            root.addChild(colorPickerPopup);
        }
        return root;
    }

    private PositionedNode<?> field(String labelKey, PositionedNode<?> control, int width) {
        PositionedNode<?> column = new PositionedNode<>().width(width).height(FIELD_HEIGHT).columnGap(UiMetrics.SPACE_XS);
        column.addChild(new TextNode(() -> Component.translatable(labelKey).getString())
                .setColorResolver(UiTheme::textMuted)
                .setTextAlign(0f, 0.5f)
                .fullWidth().height(LABEL_HEIGHT));
        control.fullWidth().height(INPUT_HEIGHT);
        column.addChild(control);
        return column;
    }

    @Getter @AllArgsConstructor
    public enum Type {
        CREATE("alumite.waypoints.create.title"),
        EDIT("alumite.waypoints.edit.title");

        private final String titleKey;
    }
}
