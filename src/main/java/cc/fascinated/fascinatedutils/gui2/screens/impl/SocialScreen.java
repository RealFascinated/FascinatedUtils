package cc.fascinated.fascinatedutils.gui2.screens.impl;

import cc.fascinated.fascinatedutils.AlumiteMod;
import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.AlumiteApiException;
import cc.fascinated.fascinatedutils.api.channel.Channel;
import cc.fascinated.fascinatedutils.api.channel.DmChannel;
import cc.fascinated.fascinatedutils.api.friend.PendingFriendRequest;
import cc.fascinated.fascinatedutils.api.user.User;
import cc.fascinated.fascinatedutils.gui2.core.*;
import cc.fascinated.fascinatedutils.gui2.node.DividerNode;
import cc.fascinated.fascinatedutils.gui2.node.PanelNode;
import cc.fascinated.fascinatedutils.gui2.node.social.FriendsPanelNode;
import cc.fascinated.fascinatedutils.gui2.node.social.SocialNavNode;
import cc.fascinated.fascinatedutils.gui2.node.social.chat.ChatPanelNode;
import cc.fascinated.fascinatedutils.gui2.node.social.player.PlayerContextMenuHandler;
import cc.fascinated.fascinatedutils.gui2.node.social.player.PlayerContextMenuNode;
import cc.fascinated.fascinatedutils.gui2.node.social.player.SelfProfileNode;
import cc.fascinated.fascinatedutils.gui2.node.social.player.StatusMenuNode;
import cc.fascinated.fascinatedutils.gui2.screens.RootScreen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.BiConsumer;

public class SocialScreen extends RootScreen {

    private static final int LEFT_PANE_WIDTH = 170;
    private static final int DIVIDER_WIDTH = 1;
    private static final int PROFILE_PANE_HEIGHT = 52;

    public SocialScreen() {
        super(Component.translatable("alumite.social.title"));
    }

    @Override
    protected UiNode composeContent(UiStateStore stateStore) {
        UiState<String> selectedChannelId = stateStore.state("social.channel", null);
        UiState<Boolean> statusMenuOpen = stateStore.state("social.statusMenu.open", false);
        UiState<Float> statusMenuPositionX = stateStore.state("social.statusMenu.x", 0f);
        UiState<Float> statusMenuPositionY = stateStore.state("social.statusMenu.y", 0f);

        String activeChannelId = selectedChannelId.get();
        Channel activeChannel = activeChannelId != null && Alumite.INSTANCE != null
                ? Alumite.INSTANCE.channels().get(activeChannelId)
                : null;
        boolean friendsActive = activeChannel == null;

        PlayerContextMenuHandler contextMenuHandler = (user, pointerX, pointerY) -> {
            if (user != null) {
                GlobalContextMenu.open(() -> {
                    PlayerContextMenuNode menu = new PlayerContextMenuNode(
                            user, GlobalContextMenu::close, () -> removeFriend(user));
                    menu.setPreferredPosition(pointerX, pointerY);
                    return menu;
                });
            }
        };

        PositionedNode root = new PositionedNode().full();

        PanelNode background = new PanelNode();
        background.full();
        root.addChild(background);

        PositionedNode layoutRow = new PositionedNode().full();
        root.addChild(layoutRow);

        PositionedNode leftPane = new PositionedNode().width(LEFT_PANE_WIDTH).fullHeight();
        layoutRow.addChild(leftPane);

        DividerNode divider = new DividerNode();
        divider.left(LEFT_PANE_WIDTH).width(DIVIDER_WIDTH).fullHeight();
        layoutRow.addChild(divider);

        PositionedNode rightPane = new PositionedNode()
                .left(LEFT_PANE_WIDTH + DIVIDER_WIDTH)
                .right(0)
                .fullHeight();
        layoutRow.addChild(rightPane);

        buildLeftPane(leftPane, selectedChannelId, contextMenuHandler, friendsActive,
                (pointerX, pointerY) -> {
                    statusMenuPositionX.set(pointerX);
                    statusMenuPositionY.set(pointerY);
                    statusMenuOpen.set(!statusMenuOpen.get());
                });
        buildRightPane(rightPane, stateStore, activeChannel, contextMenuHandler, selectedChannelId);

        if (statusMenuOpen.get()) {
            StatusMenuNode statusMenu = new StatusMenuNode(() -> statusMenuOpen.set(false));
            statusMenu.setPreferredPosition(statusMenuPositionX.get(), statusMenuPositionY.get());
            root.addChild(statusMenu);
        }

        return root;
    }

    private void buildLeftPane(PositionedNode leftPane, UiState<String> selectedChannelId,
                                PlayerContextMenuHandler contextMenuHandler, boolean friendsActive,
                                BiConsumer<Float, Float> onStatusClick) {
        PositionedNode navArea = new PositionedNode()
                .fullWidth()
                .top(0)
                .bottom(PROFILE_PANE_HEIGHT);
        SocialNavNode navNode = new SocialNavNode(
                friendsActive,
                selectedChannelId.get(),
                incomingRequestCount(),
                () -> selectedChannelId.set(null),
                selectedChannelId::set,
                contextMenuHandler);
        navArea.addChild(navNode);
        leftPane.addChild(navArea);

        SelfProfileNode selfProfile = new SelfProfileNode();
        selfProfile.bottom(0);
        selfProfile.setOnStatusClick(onStatusClick);
        leftPane.addChild(selfProfile);

        leftPane.setNodeId("social.left-pane");
    }

    private void buildRightPane(PositionedNode rightPane, UiStateStore stateStore,
                                 Channel activeChannel, PlayerContextMenuHandler contextMenuHandler,
                                 UiState<String> selectedChannelId) {
        if (activeChannel != null) {
            rightPane.addChild(new ChatPanelNode(activeChannel, stateStore, contextMenuHandler));
        } else {
            rightPane.addChild(new FriendsPanelNode(contextMenuHandler,
                    user -> openDmWithUser(user, selectedChannelId)));
        }
    }

    private void openDmWithUser(User user, UiState<String> selectedChannelId) {
        if (user == null || Alumite.INSTANCE == null) {
            return;
        }
        // If a DM channel with this user already exists locally, jump straight to it.
        DmChannel existing = Alumite.INSTANCE.channels().all().stream()
                .filter(channel -> channel instanceof DmChannel dm && dm.recipient() != null
                        && dm.recipient().id().equals(user.id()))
                .map(channel -> (DmChannel) channel)
                .findFirst()
                .orElse(null);
        if (existing != null) {
            selectedChannelId.set(existing.id());
            return;
        }
        AlumiteMod.SCHEDULED_POOL.execute(() -> {
            try {
                DmChannel dm = Alumite.INSTANCE.channels().openDmAndCache(user.id());
                selectedChannelId.set(dm.id());
            } catch (AlumiteApiException ignored) {
            }
        });
    }

    private void removeFriend(User user) {
        if (user == null || Alumite.INSTANCE == null || !Alumite.INSTANCE.users().isFriend(user.id())) {
            return;
        }
        AlumiteMod.SCHEDULED_POOL.execute(() -> {
            try {
                Alumite.INSTANCE.removeFriend(user.id());
            } catch (Exception ignored) {
            }
        });
    }

    private int incomingRequestCount() {
        if (Alumite.INSTANCE == null) {
            return 0;
        }
        List<PendingFriendRequest> requests = Alumite.INSTANCE.users().incomingFriendRequests();
        return requests != null ? requests.size() : 0;
    }

}