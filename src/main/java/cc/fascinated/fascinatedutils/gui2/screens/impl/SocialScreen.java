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
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class SocialScreen extends RootScreen {

    private static final int LEFT_PANE_WIDTH = 170;
    private static final int DIVIDER_WIDTH = 1;
    private static final int PROFILE_PANE_HEIGHT = 52;

    private static String lastChannelId = null;

    public SocialScreen() {
        super(Component.translatable("alumite.social.title"));
    }

    @Override
    protected UiNode composeContent() {
        UiState<String> selectedChannelId = stateStore.state("social.channel", lastChannelId);
        Consumer<String> selectChannel = id -> { lastChannelId = id; selectedChannelId.set(id); };
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
                    PlayerContextMenuNode menu = new PlayerContextMenuNode(user, GlobalContextMenu::close, () -> removeFriend(user));
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

        buildLeftPane(leftPane, selectChannel, contextMenuHandler, friendsActive,
                (pointerX, pointerY) -> {
                    statusMenuPositionX.set(pointerX);
                    statusMenuPositionY.set(pointerY);
                    statusMenuOpen.set(!statusMenuOpen.get());
                });
        buildRightPane(rightPane, activeChannel, contextMenuHandler, selectChannel);

        if (statusMenuOpen.get()) {
            StatusMenuNode statusMenu = new StatusMenuNode(() -> statusMenuOpen.set(false));
            statusMenu.setPreferredPosition(statusMenuPositionX.get(), statusMenuPositionY.get());
            root.addChild(statusMenu);
        }

        return root;
    }

    private void buildLeftPane(PositionedNode leftPane, Consumer<String> selectChannel,
                                PlayerContextMenuHandler contextMenuHandler, boolean friendsActive,
                                BiConsumer<Float, Float> onStatusClick) {
        PositionedNode navArea = new PositionedNode()
                .fullWidth()
                .top(0)
                .bottom(PROFILE_PANE_HEIGHT);
        UiState<Integer> channelListScrollState = stateStore.state("social.channel-list.scroll", 0);
        SocialNavNode navNode = new SocialNavNode(
                friendsActive,
                lastChannelId,
                incomingRequestCount(),
                () -> selectChannel.accept(null),
                selectChannel,
                contextMenuHandler,
                channelListScrollState);
        navArea.addChild(navNode);
        leftPane.addChild(navArea);

        SelfProfileNode selfProfile = new SelfProfileNode();
        selfProfile.bottom(0);
        selfProfile.setOnStatusClick(onStatusClick);
        leftPane.addChild(selfProfile);

        leftPane.setNodeId("social.left-pane");
    }

    private void buildRightPane(PositionedNode rightPane,
                                 Channel activeChannel, PlayerContextMenuHandler contextMenuHandler,
                                 Consumer<String> selectChannel) {
        if (activeChannel != null) {
            rightPane.addChild(new ChatPanelNode(activeChannel, stateStore, contextMenuHandler));
        } else {
            UiState<Integer> friendsListScrollState = stateStore.state("social.friends-list.scroll", 0);
            rightPane.addChild(new FriendsPanelNode(contextMenuHandler,
                    user -> openDmWithUser(user, selectChannel),
                    friendsListScrollState));
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (super.keyPressed(event)) {
            return true;
        }
        if (stateStore == null) {
            return false;
        }
        int key = event.key();
        if (key != GLFW.GLFW_KEY_ENTER && key != GLFW.GLFW_KEY_KP_ENTER) {
            return false;
        }
        String channelId = stateStore.<String>state("social.channel", null).get();
        if (channelId == null) {
            return false;
        }
        stateStore.requestFocus("social.chat-composer");
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (super.charTyped(event)) {
            return true;
        }
        if (stateStore == null) {
            return false;
        }
        String channelId = stateStore.<String>state("social.channel", null).get();
        if (channelId == null) {
            return false;
        }
        int codepoint = event.codepoint();
        if (codepoint < 0x20 || codepoint > 0xFFFF) {
            return false;
        }
        char typed = (char) codepoint;
        UiState<String> draft = stateStore.state("social.composer.draft." + channelId, "");
        UiState<Integer> caretState = stateStore.state("social.composer.caret." + channelId, Integer.MAX_VALUE);
        UiState<Integer> selectionState = stateStore.state("social.composer.selection." + channelId, -1);
        String text = draft.get();
        int caretPos = caretState.get();
        int actualCaret = caretPos == Integer.MAX_VALUE ? text.length() : Math.min(caretPos, text.length());
        draft.set(text.substring(0, actualCaret) + typed + text.substring(actualCaret));
        caretState.set(actualCaret + 1);
        selectionState.set(-1);
        stateStore.requestFocus("social.chat-composer");
        return true;
    }

    private void openDmWithUser(User user, Consumer<String> selectChannel) {
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
            selectChannel.accept(existing.id());
            return;
        }
        AlumiteMod.SCHEDULED_POOL.execute(() -> {
            try {
                DmChannel dm = Alumite.INSTANCE.channels().openDm(user.id());
                selectChannel.accept(dm.id());
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

    public void openChannel(String channelId) {
        lastChannelId = channelId;
        if (stateStore != null) {
            stateStore.<String>state("social.channel", lastChannelId).set(channelId);
        }
    }

    private int incomingRequestCount() {
        if (Alumite.INSTANCE == null) {
            return 0;
        }
        List<PendingFriendRequest> requests = Alumite.INSTANCE.users().incomingFriendRequests();
        return requests != null ? requests.size() : 0;
    }

    
}