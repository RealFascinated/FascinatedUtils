package cc.fascinated.fascinatedutils.gui2.screens.impl.waypoint;

import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.common.color.SettingColor;
import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.core.SpacerNode;
import cc.fascinated.fascinatedutils.gui2.core.UiNode;
import cc.fascinated.fascinatedutils.gui2.core.UiState;
import cc.fascinated.fascinatedutils.gui2.node.ButtonNode;
import cc.fascinated.fascinatedutils.gui2.node.CardNode;
import cc.fascinated.fascinatedutils.gui2.node.ColorPickerPopupNode;
import cc.fascinated.fascinatedutils.gui2.node.RectNode;
import cc.fascinated.fascinatedutils.gui2.node.TextNode;
import cc.fascinated.fascinatedutils.gui2.node.input.TextInputNode;
import cc.fascinated.fascinatedutils.gui2.node.input.TextParser;
import cc.fascinated.fascinatedutils.gui2.screens.RootScreen;
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
    private static final int HEIGHT = WIDTH * 10 / 16;

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
        UiState<Double> xState = stateStore.state("waypoint.create.x", type == Type.CREATE ? (double) (int) player.getX() : editWaypoint.getX());
        UiState<Double> yState = stateStore.state("waypoint.create.y", type == Type.CREATE ? (double) (int) player.getY() : editWaypoint.getY());
        UiState<Double> zState = stateStore.state("waypoint.create.z", type == Type.CREATE ? (double) (int) player.getZ() : editWaypoint.getZ());
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

        PositionedNode<?> colorRow = new PositionedNode<>().fullWidth().height(20);
        colorRow.addChild(new RectNode()
                .size(12, 12)
                .setCornerRadius(3)
                .setFillSupplier(() -> colorState.get().getResolvedArgb())
                .setBorderResolver(theme -> theme.inputBorder())
                .alignY(0.5f));
        colorRow.addChild(new ButtonNode()
                .setLabel(() -> "#" + String.format("%06X", colorState.get().getPackedArgb() & 0xFFFFFF))
                .setRounded(true)
                .left(16)
                .right(0)
                .setOnPress(() -> {
                    colorPickerPopup.initFromColor(colorState.get());
                    colorPickerOpen.set(true);
                }));

        CardNode card = new CardNode()
                .width(WIDTH)
                .height(HEIGHT)
                .setHeader(header -> {
                    header.addChild(new TextNode(() -> Component.translatable(type.getTitleKey()).getString()));

                    header.addChild(new SpacerNode());

                    header.right(4);
                    header.addChild(new ButtonNode()
                            .setIconCenter(ModUiTextures.BACK.getId())
                            .setRounded(true)
                            .setOnPress(() -> Minecraft.getInstance().setScreen(new WaypointsScreen())));
                })
                .setContents(content -> {
                    content.margin(5);
                    content.columnGap(5);

                    content.addChild(new PositionedNode<>()
                            .fullWidth().columnGap(3)
                            .addChild(new TextNode(() -> Component.translatable("alumite.waypoints.create.name").getString()))
                            .addChild(nameInput));
                    content.addChild(new PositionedNode<>()
                            .fullWidth().columnGap(3)
                            .addChild(new TextNode(() -> Component.translatable("alumite.waypoints.coord.x").getString()))
                            .addChild(xInput));
                    content.addChild(new PositionedNode<>()
                            .fullWidth().columnGap(3)
                            .addChild(new TextNode(() -> Component.translatable("alumite.waypoints.coord.y").getString()))
                            .addChild(yInput));
                    content.addChild(new PositionedNode<>()
                            .fullWidth().columnGap(3)
                            .addChild(new TextNode(() -> Component.translatable("alumite.waypoints.coord.z").getString()))
                            .addChild(zInput));
                    content.addChild(new PositionedNode<>()
                            .fullWidth().columnGap(3)
                            .addChild(new TextNode(() -> Component.translatable("alumite.waypoints.color").getString()))
                            .addChild(colorRow));
                })
                .setFooter(footer -> {
                    footer.right(4);
                    footer.addChild(new SpacerNode());
                    footer.addChild(new ButtonNode()
                            .setLeftIcon(ModUiTextures.SAVE.getId())
                            .setLabel(Component.translatable("alumite.waypoints.save").getString())
                            .setRounded(true)
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
                                    editWaypoint.setColor(colorState.get());
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

    @Getter @AllArgsConstructor
    public enum Type {
        CREATE("alumite.waypoints.create.title"),
        EDIT("alumite.waypoints.edit.title");

        private final String titleKey;
    }
}
