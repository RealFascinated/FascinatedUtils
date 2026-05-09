package cc.fascinated.fascinatedutils.gui2.screens.impl;

import cc.fascinated.fascinatedutils.AlumiteMod;
import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.AlumiteApiException;
import cc.fascinated.fascinatedutils.api.channel.Channel;
import cc.fascinated.fascinatedutils.api.channel.DmChannel;
import cc.fascinated.fascinatedutils.api.channel.Message;
import cc.fascinated.fascinatedutils.api.friend.PendingFriendRequest;
import cc.fascinated.fascinatedutils.api.user.User;
import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.core.ScrollColumnNode;
import cc.fascinated.fascinatedutils.gui2.core.UiNode;
import cc.fascinated.fascinatedutils.gui2.core.UiState;
import cc.fascinated.fascinatedutils.gui2.core.UiStateStore;
import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.gui2.node.ButtonNode;
import cc.fascinated.fascinatedutils.gui2.node.ConfirmPopupNode;
import cc.fascinated.fascinatedutils.gui2.node.DividerNode;
import cc.fascinated.fascinatedutils.gui2.node.PanelNode;
import cc.fascinated.fascinatedutils.gui2.node.TextNode;
import net.minecraft.client.Minecraft;
import cc.fascinated.fascinatedutils.gui2.node.social.SocialTabBarNode;
import cc.fascinated.fascinatedutils.gui2.node.social.channel.ChannelListNode;
import cc.fascinated.fascinatedutils.gui2.node.social.chat.ChatComposerNode;
import cc.fascinated.fascinatedutils.gui2.node.social.chat.ChatHeaderNode;
import cc.fascinated.fascinatedutils.gui2.node.social.chat.ChatMessagesNode;
import cc.fascinated.fascinatedutils.gui2.node.social.chat.MessageContextMenuNode;
import cc.fascinated.fascinatedutils.gui2.theme.UiThemeRepository;
import cc.fascinated.fascinatedutils.gui2.node.social.player.PlayerContextMenuNode;
import cc.fascinated.fascinatedutils.gui2.node.social.player.StatusMenuNode;
import cc.fascinated.fascinatedutils.gui2.node.social.player.FriendsListNode;
import cc.fascinated.fascinatedutils.gui2.node.social.player.SelfProfileNode;
import cc.fascinated.fascinatedutils.gui2.screens.RootScreen;
import cc.fascinated.fascinatedutils.oldgui.toast.Toast;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.BiConsumer;

public class SocialScreen extends RootScreen {

    private static final int LEFT_PANE_WIDTH = 200;
    private static final int DIVIDER_WIDTH = 1;
    private static final int PROFILE_PANE_HEIGHT = 36;
    private static final int TAB_BAR_HEIGHT = 30;
    private static final int CHAT_HEADER_HEIGHT = 40;
    private static final int COMPOSER_HEIGHT = 24;
    private static final int LEFT_LIST_PADDING = 6;
    private static final int CHAT_PADDING = 6;

    public SocialScreen() {
        super(Component.translatable("alumite.social.title"));
    }

    @Override
    protected UiNode composeRoot(UiStateStore stateStore) {
        UiState<SocialTabBarNode.Tab> activeTab = stateStore.state("social.tab", SocialTabBarNode.Tab.CHAT);
        UiState<String> selectedChannelId = stateStore.state("social.channel", null);
        UiState<User> contextMenuUser = stateStore.state("social.playerContextMenu.user", null);
        UiState<Float> contextMenuPositionX = stateStore.state("social.playerContextMenu.positionX", 0f);
        UiState<Float> contextMenuPositionY = stateStore.state("social.playerContextMenu.positionY", 0f);
        UiState<Boolean> statusMenuOpen = stateStore.state("social.statusMenu.open", false);
        UiState<Float> statusMenuPositionX = stateStore.state("social.statusMenu.x", 0f);
        UiState<Float> statusMenuPositionY = stateStore.state("social.statusMenu.y", 0f);

        PositionedNode root = new PositionedNode().full();

        PanelNode background = new PanelNode();
        background.full();
        root.addChild(background);

        PositionedNode layoutRow = new PositionedNode().full();
        root.addChild(layoutRow);

        PositionedNode leftPane = new PositionedNode()
            .width(LEFT_PANE_WIDTH)
                .fullHeight();
        layoutRow.addChild(leftPane);

        DividerNode divider = new DividerNode();
        divider.left(LEFT_PANE_WIDTH).width(DIVIDER_WIDTH).fullHeight();
        layoutRow.addChild(divider);

        PositionedNode rightPane = new PositionedNode()
            .left(LEFT_PANE_WIDTH + DIVIDER_WIDTH)
                .right(0)
                .fullHeight();
        layoutRow.addChild(rightPane);

        buildLeftPane(leftPane, stateStore, activeTab, selectedChannelId, contextMenuUser, contextMenuPositionX, contextMenuPositionY,
                (pointerX, pointerY) -> {
                    statusMenuPositionX.set(pointerX);
                    statusMenuPositionY.set(pointerY);
                    statusMenuOpen.set(!statusMenuOpen.get());
                });
        buildRightPane(rightPane, stateStore, activeTab, selectedChannelId, contextMenuUser, contextMenuPositionX, contextMenuPositionY);

        ButtonNode closeButton = new ButtonNode("");
        closeButton.size(20, 20).right(6).top(5);
        closeButton.setLeftIcon(ModUiTextures.CLOSE.getId());
        closeButton.setRounded(true);
        closeButton.setOnPress(() -> Minecraft.getInstance().setScreen(null));
        root.addChild(closeButton);

        if (statusMenuOpen.get()) {
            StatusMenuNode statusMenu = new StatusMenuNode(() -> statusMenuOpen.set(false));
            statusMenu.setPreferredPosition(statusMenuPositionX.get(), statusMenuPositionY.get());
            root.addChild(statusMenu);
        }

        if (contextMenuUser.get() != null) {
            PlayerContextMenuNode menu = new PlayerContextMenuNode(
                    contextMenuUser.get(),
                    () -> closePlayerContextMenu(contextMenuUser),
                    () -> removeFriend(contextMenuUser.get()));
            menu.setPreferredPosition(contextMenuPositionX.get(), contextMenuPositionY.get());
            root.addChild(menu);
        }

        return root;
    }

    private void buildLeftPane(PositionedNode leftPane, UiStateStore stateStore, UiState<SocialTabBarNode.Tab> activeTab,
                               UiState<String> selectedChannelId, UiState<User> contextMenuUser,
                               UiState<Float> contextMenuPositionX, UiState<Float> contextMenuPositionY,
                               BiConsumer<Float, Float> onStatusClick) {
        SocialTabBarNode tabBar = new SocialTabBarNode();
        tabBar.setActiveTab(activeTab.get());
        tabBar.setIncomingRequestBadgeCount(incomingRequestCount());
        tabBar.setOnChatSelected(() -> activeTab.set(SocialTabBarNode.Tab.CHAT));
        tabBar.setOnFriendsSelected(() -> activeTab.set(SocialTabBarNode.Tab.FRIENDS));
        leftPane.addChild(tabBar);

        PositionedNode listWrapper = new PositionedNode()
                .fullWidth()
                .top(TAB_BAR_HEIGHT)
                .bottom(PROFILE_PANE_HEIGHT);
        ScrollColumnNode listScroll = buildLeftList(stateStore, activeTab, selectedChannelId, contextMenuUser, contextMenuPositionX, contextMenuPositionY);
        listScroll.setNodeId("social.left-list");

        PositionedNode listContent = new PositionedNode()
            .left(LEFT_LIST_PADDING)
            .right(LEFT_LIST_PADDING)
            .top(LEFT_LIST_PADDING)
            .bottom(LEFT_LIST_PADDING);
        listContent.addChild(listScroll);
        listWrapper.addChild(listContent);
        leftPane.addChild(listWrapper);

        SelfProfileNode selfProfile = new SelfProfileNode();
        selfProfile.bottom(0);
        selfProfile.setOnStatusClick(onStatusClick);
        leftPane.addChild(selfProfile);

        leftPane.setNodeId("social.left-pane");
    }

    private ScrollColumnNode buildLeftList(UiStateStore stateStore, UiState<SocialTabBarNode.Tab> activeTab,
                                           UiState<String> selectedChannelId, UiState<User> contextMenuUser,
                                           UiState<Float> contextMenuPositionX, UiState<Float> contextMenuPositionY) {
        return switch (activeTab.get()) {
            case CHAT -> new ChannelListNode(
                    selectedChannelId.get(),
                    selectedChannelId::set,
                    (user, pointerX, pointerY) -> openPlayerContextMenu(user, pointerX, pointerY,
                            contextMenuUser, contextMenuPositionX, contextMenuPositionY));
            case FRIENDS -> new FriendsListNode((user, pointerX, pointerY) -> openPlayerContextMenu(
                    user, pointerX, pointerY, contextMenuUser, contextMenuPositionX, contextMenuPositionY));
        };
    }

    private void buildRightPane(PositionedNode rightPane, UiStateStore stateStore,
                                 UiState<SocialTabBarNode.Tab> activeTab, UiState<String> selectedChannelId,
                                 UiState<User> contextMenuUser, UiState<Float> contextMenuPositionX,
                                 UiState<Float> contextMenuPositionY) {
        String activeChannelId = selectedChannelId.get();
        Channel channel = Alumite.INSTANCE != null && activeChannelId != null
            ? Alumite.INSTANCE.channels().get(activeChannelId)
            : null;

        if (channel == null) {
            TextNode emptyState = new TextNode(
                    activeTab.get() == SocialTabBarNode.Tab.CHAT
                            ? "Select a conversation to start chatting"
                            : "Select a friend to view their profile")
                    .setColorArgb(UiThemeRepository.get().textSubtle());
            emptyState.center();
            rightPane.addChild(emptyState);
            return;
        }

        ChatHeaderNode header = new ChatHeaderNode(() -> channel);
        header.setOnSecondaryClick((pointerX, pointerY) -> {
            DmChannel dmChannel = channel.asDmChannel();
            User recipient = dmChannel != null ? dmChannel.recipient() : null;
            openPlayerContextMenu(recipient, pointerX, pointerY, contextMenuUser, contextMenuPositionX, contextMenuPositionY);
        });
        rightPane.addChild(header);

        PositionedNode messagesWrapper = new PositionedNode()
            .left(CHAT_PADDING)
            .right(CHAT_PADDING)
            .top(CHAT_HEADER_HEIGHT + CHAT_PADDING)
            .bottom(COMPOSER_HEIGHT + (CHAT_PADDING * 2));
        ChatMessagesNode messagesScroll = new ChatMessagesNode(channel, stateStore,
                (user, pointerX, pointerY) -> openPlayerContextMenu(user, pointerX, pointerY, contextMenuUser, contextMenuPositionX, contextMenuPositionY));
        messagesWrapper.addChild(messagesScroll);
        rightPane.addChild(messagesWrapper);

        ChatComposerNode composer = new ChatComposerNode(channel, stateStore, "Message " + channelTitle(channel) + "...");
        composer.height(COMPOSER_HEIGHT).left(CHAT_PADDING).right(CHAT_PADDING).bottom(CHAT_PADDING);
        rightPane.addChild(composer);

        // Message context menu overlay
        UiState<Message> msgContextMenuMessage = stateStore.state("social.chat.contextMenu.message", null);
        if (msgContextMenuMessage.get() != null) {
            rightPane.addChild(new MessageContextMenuNode(stateStore));
        }

        // Delete confirmation overlay
        UiState<Message> msgPendingDelete = stateStore.state("social.chat.delete.message", null);
        Message deleteMsg = msgPendingDelete.get();
        if (deleteMsg != null) {
            ConfirmPopupNode deleteConfirm = new ConfirmPopupNode()
                    .setTitle("Delete message?")
                    .setConfirmLabel("Delete")
                    .setConfirmLabelColorResolver(theme -> theme.danger())
                    .setOnCancel(() -> msgPendingDelete.set(null))
                    .setOnConfirm(() -> {
                        msgPendingDelete.set(null);
                        submitDeleteMessage(channel, deleteMsg);
                    });
            rightPane.addChild(deleteConfirm);
        }
    }

    private void submitDeleteMessage(Channel channel, Message message) {
        AlumiteMod.SCHEDULED_POOL.execute(() -> {
            try {
                channel.deleteMessage(message.id());
            } catch (AlumiteApiException exception) {
                Toast.show().message(Component.translatable("alumite.social.error.generic").getString()).error();
            } catch (Exception exception) {
                Toast.show().message(Component.translatable("alumite.social.error.generic").getString()).error();
            }
        });
    }

    private void openPlayerContextMenu(User user, float pointerX, float pointerY,
                                       UiState<User> contextMenuUser, UiState<Float> contextMenuPositionX,
                                       UiState<Float> contextMenuPositionY) {
        if (user == null) {
            return;
        }
        contextMenuUser.set(user);
        contextMenuPositionX.set(pointerX);
        contextMenuPositionY.set(pointerY);
    }

    private void closePlayerContextMenu(UiState<User> contextMenuUser) {
        contextMenuUser.set(null);
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

    private String channelTitle(Channel channel) {
        if (channel instanceof DmChannel dm && dm.recipient() != null) {
            return dm.recipient().minecraftName() != null ? dm.recipient().minecraftName() : "them";
        }
        return channel.name() != null ? channel.name() : "channel";
    }

    private int incomingRequestCount() {
        if (Alumite.INSTANCE == null) {
            return 0;
        }
        List<PendingFriendRequest> requests = Alumite.INSTANCE.users().incomingFriendRequests();
        return requests != null ? requests.size() : 0;
    }

}