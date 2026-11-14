package cc.fascinated.fascinatedutils.gui.screens.impl.waypoint;

import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.common.PlayerUtils;
import cc.fascinated.fascinatedutils.gui.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui.core.SpacerNode;
import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.gui.core.UiState;
import cc.fascinated.fascinatedutils.gui.node.ButtonNode;
import cc.fascinated.fascinatedutils.gui.node.CardNode;
import cc.fascinated.fascinatedutils.gui.node.ConfirmPopupNode;
import cc.fascinated.fascinatedutils.gui.node.RectNode;
import cc.fascinated.fascinatedutils.gui.node.ScrollColumnNode;
import cc.fascinated.fascinatedutils.gui.node.TextNode;
import cc.fascinated.fascinatedutils.gui.screens.RootScreen;
import cc.fascinated.fascinatedutils.gui.theme.UiMetrics;
import cc.fascinated.fascinatedutils.gui.theme.UiTheme;
import cc.fascinated.fascinatedutils.systems.waypoint.Waypoint;
import cc.fascinated.fascinatedutils.systems.waypoint.WaypointRepository;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.UUID;

public class WaypointsScreen extends RootScreen {

    private static final int WIDTH = 500;
    private static final int HEIGHT = 320;
    private static final int EMPTY_STATE_TOP = 90;
    private static final int ROW_HEIGHT = UiMetrics.LIST_ROW_HEIGHT;
    private static final int ROW_BUTTON_HEIGHT = UiMetrics.CONTROL_HEIGHT;

    public WaypointsScreen() {
        super(Component.translatable("alumite.waypoints.title"));
    }

    @Override
    protected UiNode createRootNode() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        assert player != null;

        UiState<UUID> confirmDeleteId = stateStore.state("waypoints.confirmDelete", null);

        List<Waypoint> waypoints = WaypointRepository.getForCurrentWorldKey().stream().filter(waypoint ->
                waypoint.getDimension().equals(player.level().dimension().identifier().toString())).toList();

        CardNode card = new CardNode()
                .width(WIDTH)
                .height(HEIGHT)
                .setFillResolver(UiTheme::panelFill)
                .setBorderResolver(UiTheme::borderSubtle)
                .setHeader(header -> {
                    header.addChild(new TextNode(() -> Component.translatable("alumite.waypoints.count", waypoints.size()).getString()).setBold(true));

                    header.addChild(new SpacerNode());

                    header.right(UiMetrics.SPACE_XS);
                    header.addChild(new ButtonNode()
                            .setIconCenter(ModUiTextures.CLOSE.getId())
                            .setRounded(true)
                            .setVariant(ButtonNode.ButtonVariant.GHOST)
                            .setOnPress(() -> mc.setScreen(null))
                    );
                })
                .setFooter(footer -> {
                    footer.right(UiMetrics.SPACE_XS);
                    footer.addChild(new SpacerNode());
                    footer.addChild(new ButtonNode()
                            .setLabel(Component.translatable("alumite.waypoints.new_waypoint").getString())
                            .setLeftIcon(ModUiTextures.ADD.getId())
                            .setVariant(ButtonNode.ButtonVariant.PRIMARY)
                            .setOnPress(() -> mc.setScreen(new CreateEditWaypointScreen(CreateEditWaypointScreen.Type.CREATE, null)))
                    );
                })
                .setContents(contents -> {
                    contents.margin(UiMetrics.SPACE_SM);
                    ScrollColumnNode list = new ScrollColumnNode()
                            .setGap(UiMetrics.SPACE_XS)
                            .persistScroll("waypoints.list");
                    if (!waypoints.isEmpty()) {
                        for (Waypoint waypoint : waypoints) {
                            CardNode waypointCard = new CardNode().height(ROW_HEIGHT).fullWidth();
                            waypointCard.setFillResolver(UiTheme::surfaceCard);
                            waypointCard.setBorderResolver(UiTheme::borderSubtle);
                            waypointCard.setContents(row -> {
                                row.rowGap(UiMetrics.SPACE_XS);
                                row.left(UiMetrics.SPACE_MD).right((ROW_HEIGHT - ROW_BUTTON_HEIGHT) / 2);

                                row.addChild(new RectNode()
                                        .size(10, 10)
                                        .setCornerRadius(5)
                                        .setFillSupplier(() -> waypoint.getColor().getResolvedArgb())
                                        .alignY(0.5f));

                                row.addChild(new TextNode(() -> (int) Math.round(waypoint.distanceTo()) + "m").width(40).alignY(0.5f));

                                row.addChild(new TextNode(waypoint::getName).setColorResolver((theme) -> waypoint.isVisible() ? theme.textPrimary() : theme.textMuted()).alignY(0.5f));

                                row.addChild(new SpacerNode());

                                row.addChild(new ButtonNode()
                                        .setVariant(ButtonNode.ButtonVariant.GHOST)
                                        .alignY(0.5f)
                                        .height(ROW_BUTTON_HEIGHT)
                                        .width(28)
                                        .setLabel(() -> Component.translatable(waypoint.isVisible() ? "alumite.waypoints.toggle.on" : "alumite.waypoints.toggle.off").getString())
                                        .setOnPress(() -> {
                                            waypoint.setVisible(!waypoint.isVisible());
                                            WaypointRepository.save();
                                        }));
                                row.addChild(new ButtonNode()
                                        .setVariant(ButtonNode.ButtonVariant.GHOST)
                                        .alignY(0.5f)
                                        .height(ROW_BUTTON_HEIGHT)
                                        .setLabel(Component.translatable("alumite.waypoints.teleport").getString())
                                        .setOnPress(() -> {
                                            PlayerUtils.runCommand("tp %s %s %s".formatted(waypoint.blockX(), waypoint.blockY(), waypoint.blockZ()));
                                            mc.setScreen(null);
                                        }));
                                row.addChild(new ButtonNode()
                                        .setVariant(ButtonNode.ButtonVariant.GHOST)
                                        .alignY(0.5f)
                                        .height(ROW_BUTTON_HEIGHT)
                                        .setLabel(Component.translatable("alumite.waypoints.chat").getString())
                                        .setOnPress(() -> {
                                            PlayerUtils.sendMessage("Waypoint - %s (%s, %s, %s)".formatted(waypoint.getName(), waypoint.blockX(), waypoint.blockY(), waypoint.blockZ()));
                                            mc.setScreen(null);
                                        }));
                                row.addChild(new ButtonNode()
                                        .setVariant(ButtonNode.ButtonVariant.GHOST)
                                        .alignY(0.5f)
                                        .height(ROW_BUTTON_HEIGHT)
                                        .setLabel(Component.translatable("alumite.waypoints.edit_action").getString())
                                        .setOnPress(() -> mc.setScreen(new CreateEditWaypointScreen(CreateEditWaypointScreen.Type.EDIT, waypoint))));
                                row.addChild(new ButtonNode()
                                        .setVariant(ButtonNode.ButtonVariant.GHOST)
                                        .alignY(0.5f)
                                        .height(ROW_BUTTON_HEIGHT)
                                        .setLabel(Component.translatable("alumite.waypoints.remove").getString())
                                        .setOnPress(() -> confirmDeleteId.set(waypoint.getId())));
                            });
                            list.addChild(waypointCard);
                        }
                    } else {
                        list.addChild(new PositionedNode<>().fullWidth().height(EMPTY_STATE_TOP));
                        list.addChild(new TextNode(() -> Component.translatable("alumite.waypoints.empty").getString())
                                .setBold(true).setTextAlign(0.5f, 0.5f).fullWidth().height(24));
                        list.addChild(new TextNode(() -> Component.translatable("alumite.waypoints.empty_hint").getString())
                                .setColorResolver(UiTheme::textMuted).setTextAlign(0.5f, 0.5f).fullWidth().height(16));
                    }
                    contents.addChild(list);
                });
        card.center();

        PositionedNode<?> root = new PositionedNode<>().full();
        root.addChild(card);

        UUID pendingDelete = confirmDeleteId.get();
        if (pendingDelete != null) {
            Waypoint toDelete = waypoints.stream().filter(waypoint -> waypoint.getId().equals(pendingDelete)).findFirst().orElse(null);
            root.addChild(new ConfirmPopupNode()
                    .setTitle(Component.translatable("alumite.waypoints.remove_popup.title").getString())
                    .setDescription(toDelete != null ? Component.translatable("alumite.waypoints.delete_popup.message", toDelete.getName()).getString() : null)
                    .setConfirmLabel(Component.translatable("alumite.waypoints.remove").getString())
                    .setConfirmLabelColorResolver(UiTheme::danger)
                    .setOnCancel(() -> confirmDeleteId.set(null))
                    .setOnConfirm(() -> {
                        confirmDeleteId.set(null);
                        WaypointRepository.delete(pendingDelete);
                    }));
        }

        return root;
    }
}
