package cc.fascinated.fascinatedutils.gui2.screens.impl.waypoint;

import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.common.PlayerUtils;
import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.core.SpacerNode;
import cc.fascinated.fascinatedutils.gui2.core.UiNode;
import cc.fascinated.fascinatedutils.gui2.core.UiState;
import cc.fascinated.fascinatedutils.gui2.node.ButtonNode;
import cc.fascinated.fascinatedutils.gui2.node.CardNode;
import cc.fascinated.fascinatedutils.gui2.node.ConfirmPopupNode;
import cc.fascinated.fascinatedutils.gui2.node.RectNode;
import cc.fascinated.fascinatedutils.gui2.node.ScrollColumnNode;
import cc.fascinated.fascinatedutils.gui2.node.TextNode;
import cc.fascinated.fascinatedutils.gui2.screens.RootScreen;
import cc.fascinated.fascinatedutils.gui2.theme.UiTheme;
import cc.fascinated.fascinatedutils.systems.waypoint.Waypoint;
import cc.fascinated.fascinatedutils.systems.waypoint.WaypointRepository;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.UUID;

public class WaypointsScreen extends RootScreen {

    private static final int WIDTH = 500;
    private static final int HEIGHT = WIDTH * 10 / 16;

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
                .setHeader(header -> {
                    header.addChild(new TextNode(() -> Component.translatable("alumite.waypoints.count", waypoints.size()).getString()));

                    header.addChild(new SpacerNode());

                    header.right(4);
                    header.addChild(new ButtonNode()
                            .setIconCenter(ModUiTextures.CLOSE.getId())
                            .setRounded(true)
                            .setOnPress(() -> mc.setScreen(null))
                    );
                })
                .setFooter(footer -> {
                    footer.right(4);
                    footer.addChild(new SpacerNode());
                    footer.addChild(new ButtonNode()
                            .setLabel(Component.translatable("alumite.waypoints.new_waypoint").getString())
                            .setLeftIcon(ModUiTextures.ADD.getId())
                            .setRounded(true)
                            .setOnPress(() -> mc.setScreen(new CreateEditWaypointScreen(CreateEditWaypointScreen.Type.CREATE, null)))
                    );
                })
                .setContents(contents -> {
                    contents.margin(4);
                    ScrollColumnNode list = new ScrollColumnNode()
                            .setGap(4)
                            .bindScrollState(stateStore.state("waypoints.list.scroll", 0));
                    if (!waypoints.isEmpty()) {
                        for (Waypoint waypoint : waypoints) {
                            CardNode waypointCard = new CardNode().height(28).fullWidth();
                            waypointCard.setContents(row -> {
                                row.rowGap(4);
                                row.left(8).right(4);

                                row.addChild(new RectNode()
                                        .size(10, 10)
                                        .setCornerRadius(5)
                                        .setFillSupplier(() -> waypoint.getColor().getResolvedArgb())
                                        .alignY(0.5f));

                                row.addChild(new TextNode(() -> (int) Math.round(waypoint.distanceTo()) + "m").width(40).alignY(0.5f));

                                row.addChild(new TextNode(waypoint::getName).setColorResolver((theme) -> waypoint.isVisible() ? theme.textPrimary() : theme.textMuted()).alignY(0.5f));

                                row.addChild(new SpacerNode());

                                row.addChild(new ButtonNode()
                                        .setRounded(true)
                                        .alignY(0.5f)
                                        .width(28)
                                        .setLabel(() -> Component.translatable(waypoint.isVisible() ? "alumite.waypoints.toggle.on" : "alumite.waypoints.toggle.off").getString())
                                        .setOnPress(() -> waypoint.setVisible(!waypoint.isVisible())));
                                row.addChild(new ButtonNode()
                                        .setRounded(true)
                                        .alignY(0.5f)
                                        .setLabel(Component.translatable("alumite.waypoints.teleport").getString())
                                        .setOnPress(() -> {
                                            PlayerUtils.runCommand("tp %s %s %s".formatted(waypoint.blockX(), waypoint.blockY(), waypoint.blockZ()));
                                            mc.setScreen(null);
                                        }));
                                row.addChild(new ButtonNode()
                                        .setRounded(true)
                                        .alignY(0.5f)
                                        .setLabel(Component.translatable("alumite.waypoints.chat").getString())
                                        .setOnPress(() -> {
                                            PlayerUtils.sendMessage("Waypoint - %s (%s, %s, %s)".formatted(waypoint.getName(), waypoint.blockX(), waypoint.blockY(), waypoint.blockZ()));
                                            mc.setScreen(null);
                                        }));
                                row.addChild(new ButtonNode()
                                        .setRounded(true)
                                        .alignY(0.5f)
                                        .setLabel(Component.translatable("alumite.waypoints.edit_action").getString())
                                        .setOnPress(() -> mc.setScreen(new CreateEditWaypointScreen(CreateEditWaypointScreen.Type.EDIT, waypoint))));
                                row.addChild(new ButtonNode()
                                        .setRounded(true)
                                        .alignY(0.5f)
                                        .setLabel(Component.translatable("alumite.waypoints.remove").getString())
                                        .setOnPress(() -> confirmDeleteId.set(waypoint.getId())));
                            });
                            list.addChild(waypointCard);
                        }
                    } else {
                        list.addChild(new TextNode(() -> Component.translatable("alumite.waypoints.empty").getString()).alignX(0.5f));
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
