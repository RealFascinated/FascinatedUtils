package cc.fascinated.fascinatedutils.gui.screens.impl.waypoint;

import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.common.util.PlayerUtils;
import cc.fascinated.fascinatedutils.gui.components.CloseButton;
import cc.fascinated.fascinatedutils.gui.components.InputHost;
import cc.fascinated.fascinatedutils.gui.components.PanelCard;
import cc.fascinated.fascinatedutils.gui.components.ScrollHost;
import cc.fascinated.fascinatedutils.gui.components.SurfaceCard;
import cc.fascinated.fascinatedutils.gui.components.UiTranslations;
import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.layout.SpacerNode;
import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.gui.core.state.UiState;
import cc.fascinated.fascinatedutils.gui.node.control.ButtonNode;
import cc.fascinated.fascinatedutils.gui.node.display.CardNode;
import cc.fascinated.fascinatedutils.gui.node.popup.ConfirmPopupNode;
import cc.fascinated.fascinatedutils.gui.node.display.RectNode;
import cc.fascinated.fascinatedutils.gui.node.layout.ScrollColumnNode;
import cc.fascinated.fascinatedutils.gui.node.display.TextNode;
import cc.fascinated.fascinatedutils.gui.node.input.TextInputNode;
import cc.fascinated.fascinatedutils.gui.node.input.TextParser;
import cc.fascinated.fascinatedutils.gui.screens.RootScreen;
import cc.fascinated.fascinatedutils.gui.theme.UiMetrics;
import cc.fascinated.fascinatedutils.gui.theme.UiTheme;
import cc.fascinated.fascinatedutils.systems.waypoint.Waypoint;
import cc.fascinated.fascinatedutils.systems.waypoint.WaypointRepository;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Lists the waypoints of the current world and dimension with search, sorting and per-waypoint actions.
 */
public class WaypointsScreen extends RootScreen {

    private static final int WIDTH = 500;
    private static final int HEIGHT = 320;
    private static final int EMPTY_STATE_TOP = 90;
    private static final int ROW_HEIGHT = UiMetrics.LIST_ROW_HEIGHT;
    private static final int ROW_BUTTON_HEIGHT = UiMetrics.CONTROL_HEIGHT;
    private static final int SEARCH_WIDTH = 220;
    private static final int SORT_BUTTON_WIDTH = 120;
    private static final int LIST_HEIGHT = HEIGHT - 2 * UiMetrics.HEADER_HEIGHT - 3 * UiMetrics.SPACE_SM - UiMetrics.CONTROL_HEIGHT;
    private static final String SEARCH_NODE_ID = "waypoints.search";

    public WaypointsScreen() {
        super(Component.translatable("alumite.waypoints.title"));
    }

    @Override
    public void tick() {
        if (Minecraft.getInstance().player == null) {
            Minecraft.getInstance().setScreen(null);
        }
    }

    @Override
    protected void onEscapePressed() {
        UiState<Waypoint> confirmDelete = stateStore.state("waypoints.confirmDelete", null);
        UiState<Waypoint> confirmTeleport = stateStore.state("waypoints.confirmTeleport", null);
        if (confirmDelete.get() != null || confirmTeleport.get() != null) {
            confirmDelete.set(null);
            confirmTeleport.set(null);
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

        UiState<Waypoint> confirmDelete = stateStore.state("waypoints.confirmDelete", null);
        UiState<Waypoint> confirmTeleport = stateStore.state("waypoints.confirmTeleport", null);
        UiState<String> search = stateStore.state("waypoints.search", "");
        UiState<Boolean> sortByDistance = stateStore.state("waypoints.sortByDistance", false);
        TextInputNode<String> searchInput = new TextInputNode<>(TextParser.STRING)
                .setPlaceholder(UiTranslations.INSTANCE.get("alumite.waypoints.search"))
                .setOnChange(search::set)
                .setValue(search.get());
        searchInput.setNodeId(SEARCH_NODE_ID);

        String dimension = player.level().dimension().identifier().toString();
        List<Waypoint> allWaypoints = WaypointRepository.getForCurrentWorldKey().stream()
                .filter(waypoint -> waypoint.getDimension().equals(dimension))
                .toList();
        String searchLower = search.get().trim().toLowerCase(Locale.ROOT);
        List<Waypoint> waypoints = allWaypoints.stream()
                .filter(waypoint -> searchLower.isEmpty() || waypoint.getName().toLowerCase(Locale.ROOT).contains(searchLower))
                .sorted(sortByDistance.get()
                        ? Comparator.comparingDouble(Waypoint::distanceTo)
                        : Comparator.comparing(Waypoint::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        CardNode card = new PanelCard()
                .setTitleHeader(() -> Component.translatable("alumite.waypoints.count", allWaypoints.size()).getString(),
                        header -> header.addChild(new CloseButton(() -> minecraft.setScreen(null))))
                .width(WIDTH)
                .height(HEIGHT)
                .setFooter(footer -> {
                    footer.right(UiMetrics.SPACE_XS);
                    footer.addChild(new SpacerNode());
                    footer.addChild(new ButtonNode()
                            .setLabel(UiTranslations.INSTANCE.get("alumite.waypoints.new_waypoint"))
                            .setLeftIcon(ModUiTextures.ADD.getId())
                            .setVariant(ButtonNode.ButtonVariant.PRIMARY)
                            .setOnPress(() -> minecraft.setScreen(new CreateEditWaypointScreen(CreateEditWaypointScreen.Type.CREATE, null, this))));
                })
                .setContents(contents -> {
                    contents.margin(UiMetrics.SPACE_SM);
                    contents.columnGap(UiMetrics.SPACE_SM);
                    PositionedNode<?> toolbar = new PositionedNode<>().fullWidth().height(UiMetrics.CONTROL_HEIGHT).rowGap(UiMetrics.SPACE_SM);
                    toolbar.addChild(new InputHost(searchInput, SEARCH_WIDTH));
                    toolbar.addChild(new SpacerNode());
                    toolbar.addChild(new ButtonNode(() -> UiTranslations.INSTANCE.get(sortByDistance.get()
                            ? "alumite.waypoints.sort.distance" : "alumite.waypoints.sort.name"))
                            .width(SORT_BUTTON_WIDTH)
                            .height(UiMetrics.CONTROL_HEIGHT)
                            .setOnPress(() -> sortByDistance.set(!sortByDistance.get())));
                    contents.addChild(toolbar);
                    ScrollColumnNode list = new ScrollColumnNode()
                            .setGap(UiMetrics.SPACE_XS)
                            .persistScroll("waypoints.list");
                    if (waypoints.isEmpty() && !allWaypoints.isEmpty()) {
                        list.addChild(new PositionedNode<>().fullWidth().height(EMPTY_STATE_TOP / 2));
                        list.addChild(new TextNode(() -> UiTranslations.INSTANCE.get("alumite.waypoints.no_results"))
                                .setColorResolver(UiTheme::textMuted).setTextAlign(0.5f, 0.5f).fullWidth().height(24));
                    }
                    else if (!waypoints.isEmpty()) {
                        for (Waypoint waypoint : waypoints) {
                            list.addChild(waypointRow(waypoint, confirmTeleport, confirmDelete));
                        }
                    }
                    else {
                        list.addChild(new PositionedNode<>().fullWidth().height(EMPTY_STATE_TOP));
                        list.addChild(new TextNode(() -> UiTranslations.INSTANCE.get("alumite.waypoints.empty"))
                                .setBold(true).setTextAlign(0.5f, 0.5f).fullWidth().height(24));
                        list.addChild(new TextNode(() -> UiTranslations.INSTANCE.get("alumite.waypoints.empty_hint"))
                                .setColorResolver(UiTheme::textMuted).setTextAlign(0.5f, 0.5f).fullWidth().height(16));
                    }
                    contents.addChild(new ScrollHost(list, LIST_HEIGHT));
                })
                .center();

        PositionedNode<?> root = new PositionedNode<>().full();
        root.addChild(card);

        Waypoint pendingTeleport = confirmTeleport.get();
        if (pendingTeleport != null) {
            root.addChild(new ConfirmPopupNode()
                    .setTitle(UiTranslations.INSTANCE.get("alumite.waypoints.teleport_popup.title"))
                    .setDescription(Component.translatable("alumite.waypoints.teleport_popup.message", pendingTeleport.getName()).getString())
                    .setConfirmLabel(UiTranslations.INSTANCE.get("alumite.waypoints.teleport"))
                    .setOnCancel(() -> confirmTeleport.set(null))
                    .setOnConfirm(() -> {
                        confirmTeleport.set(null);
                        PlayerUtils.runCommand("tp %s %s %s".formatted(pendingTeleport.blockX(), pendingTeleport.blockY(), pendingTeleport.blockZ()));
                        minecraft.setScreen(null);
                    }));
        }

        Waypoint pendingDelete = confirmDelete.get();
        if (pendingDelete != null) {
            root.addChild(new ConfirmPopupNode()
                    .setTitle(UiTranslations.INSTANCE.get("alumite.waypoints.remove_popup.title"))
                    .setDescription(Component.translatable("alumite.waypoints.delete_popup.message", pendingDelete.getName()).getString())
                    .setConfirmLabel(UiTranslations.INSTANCE.get("alumite.waypoints.remove"))
                    .setConfirmLabelColorResolver(UiTheme::danger)
                    .setOnCancel(() -> confirmDelete.set(null))
                    .setOnConfirm(() -> {
                        confirmDelete.set(null);
                        WaypointRepository.delete(pendingDelete.getId());
                    }));
        }

        return root;
    }

    private CardNode waypointRow(Waypoint waypoint, UiState<Waypoint> confirmTeleport, UiState<Waypoint> confirmDelete) {
        Minecraft minecraft = Minecraft.getInstance();
        CardNode waypointCard = new SurfaceCard().height(ROW_HEIGHT).fullWidth();
        waypointCard.setContents(row -> {
            row.rowGap(UiMetrics.SPACE_XS);
            row.left(UiMetrics.SPACE_MD).right((ROW_HEIGHT - ROW_BUTTON_HEIGHT) / 2);

            row.addChild(new RectNode()
                    .size(10, 10)
                    .setCornerRadius(5)
                    .setFillSupplier(() -> waypoint.getColor().getResolvedArgb())
                    .alignY(0.5f));
            row.addChild(new TextNode(() -> Component.translatable("alumite.waypoints.distance_fmt", (int) Math.round(waypoint.distanceTo())).getString()).width(40).alignY(0.5f));
            row.addChild(new TextNode(waypoint::getName).setColorResolver(theme -> waypoint.isVisible() ? theme.textPrimary() : theme.textMuted()).alignY(0.5f));
            row.addChild(new SpacerNode());

            row.addChild(new ButtonNode()
                    .setVariant(ButtonNode.ButtonVariant.GHOST)
                    .alignY(0.5f)
                    .height(ROW_BUTTON_HEIGHT)
                    .width(28)
                    .setLabel(() -> UiTranslations.INSTANCE.get(waypoint.isVisible() ? "alumite.waypoints.toggle.on" : "alumite.waypoints.toggle.off"))
                    .setOnPress(() -> {
                        waypoint.setVisible(!waypoint.isVisible());
                        WaypointRepository.save();
                    }));
            row.addChild(rowButton("alumite.waypoints.teleport", () -> confirmTeleport.set(waypoint)));
            row.addChild(rowButton("alumite.waypoints.chat", () -> {
                PlayerUtils.sendMessage(Component.translatable("alumite.waypoints.chat_message", waypoint.getName(), waypoint.blockX(), waypoint.blockY(), waypoint.blockZ()).getString());
                minecraft.setScreen(null);
            }));
            row.addChild(rowButton("alumite.waypoints.edit_action",
                    () -> minecraft.setScreen(new CreateEditWaypointScreen(CreateEditWaypointScreen.Type.EDIT, waypoint, this))));
            row.addChild(rowButton("alumite.waypoints.remove", () -> confirmDelete.set(waypoint)));
        });
        return waypointCard;
    }

    private ButtonNode rowButton(String labelKey, Runnable onPress) {
        return new ButtonNode()
                .setVariant(ButtonNode.ButtonVariant.GHOST)
                .alignY(0.5f)
                .height(ROW_BUTTON_HEIGHT)
                .setLabel(UiTranslations.INSTANCE.get(labelKey))
                .setOnPress(onPress);
    }
}
