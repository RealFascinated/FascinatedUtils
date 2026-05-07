package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.AlumiteMod;
import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.AlumiteApiException;
import cc.fascinated.fascinatedutils.api.channel.Channel;
import cc.fascinated.fascinatedutils.api.channel.DmChannel;
import cc.fascinated.fascinatedutils.api.friend.Friend;
import cc.fascinated.fascinatedutils.api.friend.PendingFriendRequest;
import cc.fascinated.fascinatedutils.api.user.User;
import cc.fascinated.fascinatedutils.api.user.UserStatus;
import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.gui.core.*;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.toast.Toast;
import cc.fascinated.fascinatedutils.gui.widgets.FOutlinedTextInputWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FRectWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FScrollColumnWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

public class SocialMainWorkspaceComponent extends FWidget {
    private static final float LEFT_PANEL_WIDTH = 250f;
    private static final float SPLIT_GAP = 10f;
    private static final float PAD = 10f;
    private static final float ROW_H = 36f;
    private static final float TAB_H = 26f;
    private static final float USER_STATUS_PICKER_H = 24f;

    private static String lastOpenedChannelId;
    // Node registry — preserves FState across re-renders
    private final FNodeRegistry nodes = new FNodeRegistry();
    private final FNodeWidget root;

    // Retained widgets (created once, identity preserved across re-renders)
    private final FOutlinedTextInputWidget addFriendInput;
    private final FOutlinedTextInputWidget dmMessageInput;
    private final FOutlinedTextInputWidget chatSearchInput = new FOutlinedTextInputWidget(40, 20f, () -> Component.translatable("alumite.social.search_placeholder").getString());
    private final SocialChatMessagesHandler chatMessages = new SocialChatMessagesHandler();
    private final SocialAttachButtonWidget attachButton = new SocialAttachButtonWidget();

    // FState fields — null until first render, then stable for lifetime of this component
    private FState<Tab> activeTab;
    private FState<String> selectedChannelId;
    private FState<String> selectedFriendUserId;
    private FState<Boolean> searchOpen;
    private FState<Boolean> sendMessagePending;
    private FState<Friend> pendingRemoveFriend;
    private FState<PendingFriendRequest> pendingCancelRequest;
    private FState<Boolean> preferredUserStatusMenuOpen;
    private FState<Boolean> preferredUserStatusUpdatePending;
    private FState<Float> scrollYRef;
    private FState<Float> messageScrollYRef;

    // Widget refs — set during each reactive rebuild, used by overlays in the same layout pass
    private FWidget panelRefWidget;
    private SocialUserStatusPickerWidget statusPickerWidget;

    private FState<User> userContextMenuUser;
    private FState<Float> userContextMenuX;
    private FState<Float> userContextMenuY;

    public SocialMainWorkspaceComponent(FOutlinedTextInputWidget addFriendInput, FOutlinedTextInputWidget dmMessageInput) {
        this.addFriendInput = addFriendInput;
        this.dmMessageInput = dmMessageInput;
        this.root = new FNodeWidget(nodes.get("social-main", this::buildRootWidget));
        addChild(root);
    }

    @Override
    public boolean fillsHorizontalInRow() {
        return true;
    }

    @Override
    public boolean fillsVerticalInColumn() {
        return true;
    }

    @Override
    public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
        setBounds(layoutX, layoutY, layoutWidth, layoutHeight);
        root.layout(measure, layoutX, layoutY, layoutWidth, layoutHeight);
        nodes.gc();
    }

    @Override
    public void render(GuiRenderer graphics, UiFrameContext frame, float deltaSeconds) {
        root.render(graphics, frame, deltaSeconds);
    }

    /** Returns whether the preferred user status dropdown is currently open. */
    public boolean isUserStatusMenuOpen() {
        return preferredUserStatusMenuOpen != null && preferredUserStatusMenuOpen.get();
    }

    /** Closes the preferred user status dropdown. */
    public void closeUserStatusMenu() {
        if (preferredUserStatusMenuOpen != null) {
            preferredUserStatusMenuOpen.set(false);
        }
    }

    /** Disposes all node state. Call when the owning screen closes. */
    public void dispose() {
        nodes.dispose();
    }

    private FWidget buildRootWidget(FWidgetNode.RenderContext ctx) {
        activeTab = ctx.useState(Tab.CHAT);
        selectedChannelId = ctx.useState(null);
        selectedFriendUserId = ctx.useState(null);
        searchOpen = ctx.useState(false);
        sendMessagePending = ctx.useState(false);
        pendingRemoveFriend = ctx.useState(null);
        pendingCancelRequest = ctx.useState(null);
        preferredUserStatusMenuOpen = ctx.useState(false);
        preferredUserStatusUpdatePending = ctx.useState(false);
        scrollYRef = ctx.useState(0f);
        messageScrollYRef = ctx.useState(SocialChatMessagesHandler.MESSAGE_SCROLL_ANCHOR_BOTTOM);
        userContextMenuUser = ctx.useState(null);
        userContextMenuX = ctx.useState(0f);
        userContextMenuY = ctx.useState(0f);

        if (selectedChannelId.get() == null && lastOpenedChannelId != null) {
            selectedChannelId.setQuiet(lastOpenedChannelId);
        }

        return new FWidget() {
            @Override
            public boolean fillsHorizontalInRow() {
                return true;
            }

            @Override
            public boolean fillsVerticalInColumn() {
                return true;
            }

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);

                if (pendingRemoveFriend.get() != null || pendingCancelRequest.get() != null || chatMessages.hasActiveOverlay()) {
                    preferredUserStatusMenuOpen.setQuiet(false);
                    chatMessages.suppressContextualOverlays();
                    userContextMenuUser.setQuiet(null);
                }

                clearChildren();

                float leftWidth = Math.min(LEFT_PANEL_WIDTH, lw);
                float rightWidth = Math.max(0f, lw - leftWidth - SPLIT_GAP);
                FWidget leftDock = buildPanelContent(leftWidth);
                FWidget rightDock = buildChatContent(rightWidth);
                addChild(leftDock);
                addChild(rightDock);
                leftDock.layout(measure, lx, ly, leftWidth, lh);
                rightDock.layout(measure, lx + leftWidth + SPLIT_GAP, ly, rightWidth, lh);

                if (pendingRemoveFriend.get() != null) {
                    Friend friend = pendingRemoveFriend.get();
                    FWidget overlay = SocialDestructiveFullscreenConfirmOverlay.create(new SocialDestructiveFullscreenConfirmOverlay.Props(
                            Component.translatable("alumite.social.confirm_remove_friend.title").getString(),
                            Component.translatable("alumite.social.confirm_remove_friend.message", friend.user().minecraftName()).getString(),
                            Component.translatable("alumite.social.confirm_remove_friend.confirm").getString(),
                            Component.translatable("alumite.social.confirm_remove_friend.deny").getString(),
                            () -> pendingRemoveFriend.set(null),
                            () -> {
                                pendingRemoveFriend.set(null);
                                AlumiteMod.SCHEDULED_POOL.execute(() -> {
                                    try {
                                        Alumite.INSTANCE.removeFriend(friend.user().id());
                                    } catch (AlumiteApiException exception) {
                                        Toast.show().message(SocialErrors.message(exception)).error();
                                    } catch (Exception exception) {
                                        Toast.show().message(Component.translatable("alumite.social.error.generic").getString()).error();
                                    }
                                });
                            }));
                    addChild(overlay);
                    overlay.layout(measure, lx, ly, lw, lh);
                }

                if (pendingCancelRequest.get() != null) {
                    PendingFriendRequest cancellableRequest = pendingCancelRequest.get();
                    FWidget overlay = SocialDestructiveFullscreenConfirmOverlay.create(new SocialDestructiveFullscreenConfirmOverlay.Props(
                            Component.translatable("alumite.social.confirm_cancel_request.title").getString(),
                            Component.translatable("alumite.social.confirm_cancel_request.message", cancellableRequest.user().minecraftName()).getString(),
                            Component.translatable("alumite.social.confirm_cancel_request.confirm").getString(),
                            Component.translatable("alumite.social.confirm_cancel_request.deny").getString(),
                            () -> pendingCancelRequest.set(null),
                            () -> {
                                pendingCancelRequest.set(null);
                                AlumiteMod.SCHEDULED_POOL.execute(() -> {
                                    try {
                                        Alumite.INSTANCE.cancelFriendRequest(cancellableRequest.requestId());
                                    } catch (AlumiteApiException exception) {
                                        Toast.show().message(SocialErrors.message(exception)).error();
                                    } catch (Exception exception) {
                                        Toast.show().message(Component.translatable("alumite.social.error.generic").getString()).error();
                                    }
                                });
                            }));
                    addChild(overlay);
                    overlay.layout(measure, lx, ly, lw, lh);
                }

                if (preferredUserStatusMenuOpen.get()) {
                    SocialUserStatusMenuOverlayWidget overlay = new SocialUserStatusMenuOverlayWidget(
                            statusPickerWidget, panelRefWidget, PAD,
                            new SocialUserStatusMenuWidget(status -> SocialMainWorkspaceComponent.this.updatePreferredUserStatus(status)),
                            () -> preferredUserStatusMenuOpen.set(false));
                    addChild(overlay);
                    overlay.layout(measure, lx, ly, lw, lh);
                }

                for (FWidget overlay : chatMessages.collectOverlayWidgets(selectedChannelId.get())) {
                    addChild(overlay);
                    overlay.layout(measure, lx, ly, lw, lh);
                }

                if (userContextMenuUser.get() != null) {
                    FWidget overlay = buildUserContextMenuOverlay();
                    addChild(overlay);
                    overlay.layout(measure, lx, ly, lw, lh);
                }
            }
        };
    }

    private FWidget buildPanelContent(float panelW) {
        FRectWidget panelBg = new FRectWidget();
        panelBg.setFillColorArgb(0xEE1A1E24);
        panelBg.setBorder(UITheme.COLOR_BORDER, 1f);
        FWidget pane = SocialLeftPaneWidget.build(new SocialLeftPaneWidget.Props(PAD, TAB_H, panelBg, buildHeader(), buildTabs(), buildLeftList(panelW), new SocialAddFriendFooterWidget(addFriendInput, this::sendFriendRequest), () -> activeTab.get() == Tab.FRIENDS, new SocialUserProfileFooterWidget(PAD)));
        FWidget wrapper = new FWidget() {
            {
                addChild(pane);
            }

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
                pane.layout(measure, lx, ly, lw, lh);
            }
        };
        panelRefWidget = wrapper;
        return wrapper;
    }

    private FWidget buildChatContent(float panelW) {
        FRectWidget panelBg = new FRectWidget();
        panelBg.setFillColorArgb(0xEE0F1318);
        panelBg.setBorder(UITheme.COLOR_BORDER, 1f);
        boolean showNoChannelSelected = activeTab.get() == Tab.CHAT && selectedChannelId.get() == null;
        FWidget body = activeTab.get() == Tab.CHAT ? showNoChannelSelected ? SocialNoChannelSelectedWidget.build() : chatMessages.buildMessagesBody(selectedChannelId.get(), panelW, messageScrollYRef) : buildFriendsProfilePane();
        SocialChatHeaderWidget chatHeader = new SocialChatHeaderWidget(() -> activeTab.get() == Tab.FRIENDS, this::selectedFriend, selectedChannelId::get);
        FWidget footer = activeTab.get() == Tab.CHAT && !showNoChannelSelected ? buildChatFooter() : new FWidget() {
            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
            }
        };
        return SocialRightPaneWidget.build(new SocialRightPaneWidget.Props(PAD, panelBg, chatHeader, body, footer));
    }

    private FWidget buildHeader() {
        FWidget compactHeader = SocialHeaderWidget.build(new SocialHeaderWidget.Props(this::handleAddFriendFromHeader, this::handleNewChatAction, () -> searchOpen.set(!searchOpen.get()), searchOpen::get));
        statusPickerWidget = new SocialUserStatusPickerWidget(preferredUserStatusMenuOpen::get, preferredUserStatusUpdatePending::get, () -> preferredUserStatusMenuOpen.set(!preferredUserStatusMenuOpen.get()));
        SocialUserStatusPickerWidget picker = statusPickerWidget;
        FOutlinedTextInputWidget searchInput = chatSearchInput;

        return new FWidget() {
            {
                addChild(compactHeader);
                addChild(searchInput);
                addChild(picker);
            }

            @Override
            public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
                float searchH = searchOpen.get() ? searchInput.intrinsicHeightForColumn(measure, widthBudget) + 6f : 0f;
                return 24f + 6f + searchH + USER_STATUS_PICKER_H;
            }

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
                compactHeader.layout(measure, lx, ly, lw, 24f);
                float cursorY = ly + 24f + 6f;
                if (searchOpen.get()) {
                    float inputH = searchInput.intrinsicHeightForColumn(measure, lw);
                    searchInput.layout(measure, lx, cursorY, lw, inputH);
                    searchInput.setVisible(true);
                    cursorY += inputH + 6f;
                } else {
                    searchInput.setVisible(false);
                    searchInput.layout(measure, lx, cursorY, lw, 0f);
                }
                picker.layout(measure, lx, cursorY, lw, USER_STATUS_PICKER_H);
            }
        };
    }

    private FWidget buildTabs() {
        final int friendCount = Alumite.INSTANCE.users().friends().size();
        final String chatLabel = Component.translatable("alumite.social.tab_chat").getString();
        final String friendsLabel = Component.translatable("alumite.social.tab_friends", String.valueOf(friendCount)).getString();
        return SocialModeTabsWidget.build(new SocialModeTabsWidget.Props(chatLabel, friendsLabel, Alumite.INSTANCE.users().incomingFriendRequests().size(), () -> activeTab.get() == Tab.CHAT, () -> {
            if (activeTab.get() != Tab.CHAT) {
                activeTab.set(Tab.CHAT);
                scrollYRef.set(0f);
            }
        }, () -> {
            if (activeTab.get() != Tab.FRIENDS) {
                activeTab.set(Tab.FRIENDS);
                scrollYRef.set(0f);
            }
        }));
    }

    private FScrollColumnWidget buildLeftList(float panelW) {
        float innerW = panelW - 2f * PAD;
        Float savedY = scrollYRef.get();
        float scrollY = savedY == null ? 0f : savedY;
        if (activeTab.get() == Tab.CHAT) {
            String searchQuery = chatSearchInput.value().trim().toLowerCase();
            List<Channel> channels = Alumite.INSTANCE.channels().all().stream().filter(channel -> searchQuery.isEmpty() || ChannelUtils.title(channel).toLowerCase().contains(searchQuery)).toList();
            return SocialChatListWidget.build(new SocialChatListWidget.Props(channels, scrollY, scrollYRef::set, channel -> buildChatChannelRow(channel, innerW), () -> new SocialEmptyStateWidget(Component.translatable("alumite.social.dm.no_chats").getString())));
        }
        return SocialFriendsListWidget.build(new SocialFriendsListWidget.Props(Alumite.INSTANCE.users().friends(), Alumite.INSTANCE.users().incomingFriendRequests(), Alumite.INSTANCE.users().outgoingFriendRequests(), scrollY, scrollYRef::set, friend -> buildFriendRow(friend, innerW), request -> SocialFriendRequestRowWidget.build(request, innerW, ROW_H), request -> SocialOutgoingRequestRowWidget.build(request, innerW, ROW_H, () -> pendingCancelRequest.set(request)), SocialSectionLabelWidget::new, Component.translatable("alumite.social.requests_incoming").getString(), Component.translatable("alumite.social.requests_outgoing").getString(), () -> new SocialEmptyStateWidget(Component.translatable("alumite.social.no_friends").getString()), () -> new SocialEmptyStateWidget(Component.translatable("alumite.social.no_friends").getString())));
    }

    private FWidget buildChatChannelRow(Channel channel, float innerW) {
        DmChannel dmChannel = channel.asDmChannel();
        User dmRecipient = dmChannel != null ? dmChannel.recipient() : null;
        BiConsumer<Float, Float> onContextMenu = dmRecipient != null ? (mx, my) -> {
            userContextMenuUser.set(dmRecipient);
            userContextMenuX.set(mx);
            userContextMenuY.set(my);
        } : null;
        return SocialChatRowWidget.build(new SocialChatRowWidget.Props(channel, Objects.equals(selectedChannelId.get(), channel.id()), ChannelUtils.hasUnread(channel), () -> selectChannel(channel.id(), false), dmChannel != null ? () -> closeDmChannel(channel.id()) : null, onContextMenu), innerW, ROW_H);
    }

    private FWidget buildFriendRow(Friend friend, float innerW) {
        User friendUser = friend.user();
        return SocialFriendRowWidget.build(new SocialFriendRowWidget.Props(friendUser, UserUtils.statusLine(friendUser), Objects.equals(selectedFriendUserId.get(), friend.user().id()), () -> selectedFriendUserId.set(friend.user().id()), () -> pendingRemoveFriend.set(friend), friendUser != null ? (mx, my) -> {
            userContextMenuUser.set(friendUser);
            userContextMenuX.set(mx);
            userContextMenuY.set(my);
        } : null), innerW, ROW_H);
    }

    private FWidget buildUserContextMenuOverlay() {
        User user = userContextMenuUser.get();
        float menuX = userContextMenuX.get();
        float menuY = userContextMenuY.get();
        Friend friend = Alumite.INSTANCE.users().friends().stream()
                .filter(f -> Objects.equals(f.user().id(), user.id())).findFirst().orElse(null);
        Runnable onRemove = friend != null ? () -> pendingRemoveFriend.set(friend) : null;
        FWidget menu = UserContextMenuWidget.build(menuX, menuY, user, () -> userContextMenuUser.set(null), onRemove);
        return new FWidget() {
            {
                addChild(menu);
            }

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
                menu.layout(measure, lx, ly, lw, lh);
            }

            @Override
            public PointerHitKind pointerHitKind() {
                return PointerHitKind.BLOCK;
            }

            @Override
            public void render(GuiRenderer graphics, UiFrameContext frame, float deltaSeconds) {
                if (!visible()) {
                    return;
                }
                graphics.absolutePost(() -> menu.render(graphics, frame, deltaSeconds));
            }

            @Override
            public boolean mouseDown(float pointerX, float pointerY, int button) {
                return menu.mouseDown(pointerX, pointerY, button);
            }

            @Override
            public boolean click(float pointerX, float pointerY, int button) {
                return menu.click(pointerX, pointerY, button);
            }
        };
    }

    private void updatePreferredUserStatus(UserStatus userStatus) {
        UserStatus currentUserStatus = Alumite.INSTANCE.users().selfUser().preferredUserStatus();
        preferredUserStatusMenuOpen.set(false);
        if (currentUserStatus == userStatus) {
            return;
        }
        preferredUserStatusUpdatePending.set(true);
        AlumiteMod.SCHEDULED_POOL.execute(() -> {
            try {
                Alumite.INSTANCE.users().selfUser().updatePreferredUserStatus(userStatus);
            } catch (AlumiteApiException exception) {
                Toast.show().message(SocialErrors.message(exception)).error();
            }
            Minecraft.getInstance().execute(() -> preferredUserStatusUpdatePending.set(false));
        });
    }

    private FWidget buildFriendsProfilePane() {
        Friend selectedFriend = selectedFriend();
        if (selectedFriend == null) {
            return new SocialEmptyStateWidget(Component.translatable("alumite.social.dm.pick_friend").getString());
        }
        User selectedFriendUser = selectedFriend.user();
        if (selectedFriendUser == null) {
            return new SocialEmptyStateWidget(Component.translatable("alumite.social.error.generic").getString());
        }
        return SocialFriendProfilePaneWidget.build(new SocialFriendProfilePaneWidget.Props(selectedFriendUser, () -> openDmForFriend(selectedFriend), () -> pendingRemoveFriend.set(selectedFriend)));
    }

    private Friend selectedFriend() {
        if (selectedFriendUserId.get() == null) {
            return null;
        }
        return Alumite.INSTANCE.users().friends().stream().filter(friend -> Objects.equals(friend.user().id(), selectedFriendUserId.get())).findFirst().orElse(null);
    }

    private FWidget buildChatFooter() {
        dmMessageInput.setPlaceholderSupplier(() -> {
            Channel footerChannel = selectedChannelId.get() == null ? null : Alumite.INSTANCE.channels().get(selectedChannelId.get());
            return "Message " + ChannelUtils.title(footerChannel);
        });
        return SocialChatComposerWidget.build(new SocialChatComposerWidget.Props(
                dmMessageInput,
                this::sendSelectedMessage,
                () -> chatMessages.startEditLastOwnMessage(selectedChannelId.get(), this::anchorMessageScrollToBottom),
                attachButton));
    }

    private void anchorMessageScrollToBottom() {
        messageScrollYRef.set(SocialChatMessagesHandler.MESSAGE_SCROLL_ANCHOR_BOTTOM);
    }

    private void selectChannel(String channelId, boolean switchToChat) {
        selectedChannelId.set(channelId);
        lastOpenedChannelId = channelId;
        anchorMessageScrollToBottom();
        attachButton.clear();
        if (switchToChat && activeTab.get() != Tab.CHAT) {
            activeTab.set(Tab.CHAT);
            scrollYRef.set(0f);
        }
    }

    private void sendSelectedMessage() {
        if (selectedChannelId.get() == null || sendMessagePending.get()) {
            return;
        }
        String content = dmMessageInput.value().trim();
        Path attachment = attachButton.pendingPath();
        if (content.isEmpty() && attachment == null) {
            return;
        }
        sendMessagePending.set(true);
        dmMessageInput.setValue("");
        attachButton.clear();
        Channel channel = Alumite.INSTANCE.channels().get(selectedChannelId.get());
        if (channel == null) {
            sendMessagePending.set(false);
            return;
        }
        AlumiteMod.SCHEDULED_POOL.execute(() -> {
            boolean sent;
            try {
                if (attachment != null) {
                    channel.sendMessage(content, attachment);
                } else {
                    channel.sendMessage(content);
                }
                sent = true;
            } catch (AlumiteApiException exception) {
                Client.LOG.warn("[Social] send message failed: {}", exception.getMessage());
                Toast.show().message(SocialErrors.message(exception)).error();
                sent = false;
            } catch (Exception exception) {
                Client.LOG.warn("[Social] send message unexpected error", exception);
                Toast.show().message(Component.translatable("alumite.social.error.generic").getString()).error();
                sent = false;
            }
            final boolean didSend = sent;
            Minecraft minecraftClient = Minecraft.getInstance();
            minecraftClient.execute(() -> {
                sendMessagePending.set(false);
                if (didSend) {
                    anchorMessageScrollToBottom();
                    minecraftClient.execute(this::anchorMessageScrollToBottom);
                }
            });
        });
    }

    private void handleNewChatAction() {
        activeTab.set(Tab.FRIENDS);
        scrollYRef.set(0f);
    }

    private void handleAddFriendFromHeader() {
        activeTab.set(Tab.FRIENDS);
        scrollYRef.set(0f);
        GuiFocusState.setFocusedId(addFriendInput.focusId());
    }

    private void openDmForFriend(Friend friend) {
        if (friend == null) {
            return;
        }
        AlumiteMod.SCHEDULED_POOL.execute(() -> {
            try {
                DmChannel channel = Alumite.INSTANCE.channels().openDmAndCache(friend.user().id());
                if (channel == null) {
                    return;
                }
                Minecraft.getInstance().execute(() -> selectChannel(channel.id(), true));
            } catch (Exception exception) {
                Toast.show().message(Component.translatable("alumite.social.error.generic").getString()).error();
            }
        });
    }

    private void closeDmChannel(String channelId) {
        AlumiteMod.SCHEDULED_POOL.execute(() -> {
            try {
                DmChannel channel = Alumite.INSTANCE.channels().get(channelId) instanceof DmChannel dmChannel ? dmChannel : null;
                if (channel == null) {
                    return;
                }
                channel.hide();
                Minecraft.getInstance().execute(() -> {
                    if (Objects.equals(selectedChannelId.get(), channelId)) {
                        selectedChannelId.set(null);
                    }
                    if (Objects.equals(lastOpenedChannelId, channelId)) {
                        lastOpenedChannelId = null;
                    }
                });
            } catch (AlumiteApiException exception) {
                Minecraft.getInstance().execute(() -> Toast.show().message(SocialErrors.message(exception)).error());
            } catch (Exception exception) {
                Minecraft.getInstance().execute(() -> Toast.show().message(Component.translatable("alumite.social.error.generic").getString()).error());
            }
        });
    }

    private void sendFriendRequest(String username) {
        AlumiteMod.SCHEDULED_POOL.execute(() -> {
            try {
                Alumite.INSTANCE.sendFriendRequest(username);
                Toast.show().message("Friend request sent!").success();
            } catch (AlumiteApiException exception) {
                Toast.show().message(SocialErrors.message(exception)).error();
            } catch (Exception exception) {
                Toast.show().message(Component.translatable("alumite.social.error.generic").getString()).error();
            }
        });
    }

    private enum Tab {CHAT, FRIENDS}

}
