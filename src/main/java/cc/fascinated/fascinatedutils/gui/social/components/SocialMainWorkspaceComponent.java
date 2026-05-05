package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.FascinatedUtils;
import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.AlumiteApiException;
import cc.fascinated.fascinatedutils.api.channel.*;
import cc.fascinated.fascinatedutils.api.friend.Friend;
import cc.fascinated.fascinatedutils.api.friend.PendingFriendRequest;
import cc.fascinated.fascinatedutils.api.user.Presence;
import cc.fascinated.fascinatedutils.api.user.User;
import cc.fascinated.fascinatedutils.common.TimeUtils;
import cc.fascinated.fascinatedutils.gui.AvatarTextureCache;
import cc.fascinated.fascinatedutils.gui.core.*;
import cc.fascinated.fascinatedutils.gui.declare.Ui;
import cc.fascinated.fascinatedutils.gui.declare.UiComponent;
import cc.fascinated.fascinatedutils.gui.declare.UiSlot;
import cc.fascinated.fascinatedutils.gui.declare.UiView;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.toast.Toast;
import cc.fascinated.fascinatedutils.gui.widgets.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.*;
import java.util.function.Consumer;

public class SocialMainWorkspaceComponent extends UiComponent<SocialMainWorkspaceComponent.Props> {
    private static final float LEFT_PANEL_WIDTH = 250f;
    private static final float SPLIT_GAP = 10f;
    private static final float PAD = 10f;
    private static final float ROW_H = 44f;
    private static final float BTN_H = 20f;
    private static final float ADD_BTN_W = 36f;
    private static final float TAB_H = 26f;
    private static final float PRESENCE_PICKER_H = 24f;
    private static final float PRESENCE_PICKER_DOT = 8f;
    private static final float PRESENCE_MENU_MIN_W = 176f;
    private static final float PRESENCE_MENU_MAX_W = 220f;
    private static final float PRESENCE_MENU_PAD = 6f;
    private static final float PRESENCE_MENU_ROW_H = 30f;
    private static final float PRESENCE_MENU_ROW_GAP = 4f;
    private static final float CHAT_HEADER_AVATAR = 28f;
    private static final int FOCUS_CHAT_SEARCH = 7112;

    private static final Presence[] SELECTABLE_PREFERRED_PRESENCES = {Presence.ONLINE, Presence.AWAY, Presence.DO_NOT_DISTURB, Presence.INVISIBLE};
    private static Integer lastOpenedChannelId;
    private final Ref<Float> scrollYRef = Ref.of(0f);
    private final Ref<Float> messageScrollYRef = Ref.of(SocialChatMessagesHandler.MESSAGE_SCROLL_ANCHOR_BOTTOM);
    private final FOutlinedTextInputWidget chatSearchInput = new FOutlinedTextInputWidget(FOCUS_CHAT_SEARCH, 40, 20f, () -> Component.translatable("fascinatedutils.social.search_placeholder").getString());
    private final SocialChatMessagesHandler chatMessages = new SocialChatMessagesHandler();
    private Tab activeTab = Tab.CHAT;
    private Integer selectedChannelId;
    private Integer selectedFriendUserId;
    private boolean searchOpen;
    private boolean sendMessagePending;
    private Friend pendingRemoveFriend;
    private PendingFriendRequest pendingCancelRequest;
    private boolean preferredPresenceMenuOpen;
    private boolean preferredPresenceUpdatePending;
    private float panelX;
    private float panelY;
    private float panelW;
    private float panelH;
    private float preferredPresenceButtonX;
    private float preferredPresenceButtonY;
    private float preferredPresenceButtonW;
    private float preferredPresenceButtonH;

    /**
     * Mounts the social workspace: channels, friends, messages, and confirm overlays.
     */
    public static UiView view(SocialMainWorkspaceComponent.Props props) {
        return Ui.component(SocialMainWorkspaceComponent.class, SocialMainWorkspaceComponent::new, props);
    }

    private static String presenceStatusLine(User user) {
        Presence presence = user == null || user.presence() == null ? Presence.OFFLINE : user.presence();
        if (presence == Presence.OFFLINE) {
            if (user != null && user.lastSeen() != null) {
                long lastSeenTime = user.lastSeen().getTime();
                return Component.translatable("fascinatedutils.social.presence.offline").getString() + " · " + TimeUtils.timeAgo(lastSeenTime, System.currentTimeMillis() - lastSeenTime < 61_000 ? 1 : 2);
            }
            return Component.translatable("fascinatedutils.social.presence.offline").getString();
        }
        return preferredPresenceLabel(presence);
    }

    private static String preferredPresenceLabel(Presence presence) {
        return switch (presence) {
            case ONLINE -> Component.translatable("fascinatedutils.social.presence.online").getString();
            case AWAY -> Component.translatable("fascinatedutils.social.presence.away").getString();
            case DO_NOT_DISTURB -> Component.translatable("fascinatedutils.social.presence.do_not_disturb").getString();
            case INVISIBLE -> Component.translatable("fascinatedutils.social.presence.invisible").getString();
            case OFFLINE -> Component.translatable("fascinatedutils.social.presence.offline").getString();
        };
    }

    @Override
    public UiView render() {
        Props currentProps = props();
        currentProps.presenceMenuOpenSink().accept(preferredPresenceMenuOpen);
        float viewportWidth = currentProps.viewportWidth();
        float viewportHeight = currentProps.viewportHeight();
        if (viewportWidth <= 0f || viewportHeight <= 0f) {
            throw new IllegalStateException("Declarative mount host delivered non-positive viewport");
        }
        if (selectedChannelId == null && lastOpenedChannelId != null) {
            selectedChannelId = lastOpenedChannelId;
        }
        List<UiSlot> viewportLayers = new ArrayList<>();
        viewportLayers.add(UiSlot.keyed("social.main", Ui.custom(previous -> buildSocialMainDock())));
        if (pendingRemoveFriend != null) {
            Friend friend = pendingRemoveFriend;
            viewportLayers.add(UiSlot.keyed("social.popup.remove." + friend.user().id(), SocialDestructiveFullscreenConfirmOverlay.view(new SocialDestructiveFullscreenConfirmOverlay.Props(Component.translatable("fascinatedutils.social.confirm_remove_friend.title").getString(), Component.translatable("fascinatedutils.social.confirm_remove_friend.message", friend.user().minecraftName()).getString(), Component.translatable("fascinatedutils.social.confirm_remove_friend.confirm").getString(), Component.translatable("fascinatedutils.social.confirm_remove_friend.deny").getString(), () -> {
                pendingRemoveFriend = null;
            }, () -> {
                pendingRemoveFriend = null;
                FascinatedUtils.SCHEDULED_POOL.execute(() -> {
                    try {
                        Alumite.INSTANCE.removeFriend(friend.user().id());
                    } catch (AlumiteApiException exception) {
                        Toast.show().message(SocialErrors.message(exception)).error();
                    } catch (Exception exception) {
                        Toast.show().message(Component.translatable("fascinatedutils.social.error.generic").getString()).error();
                    }
                });
            }))));
        }
        if (pendingCancelRequest != null) {
            PendingFriendRequest cancellableRequest = pendingCancelRequest;
            viewportLayers.add(UiSlot.keyed("social.popup.cancel." + cancellableRequest.requestId(), SocialDestructiveFullscreenConfirmOverlay.view(new SocialDestructiveFullscreenConfirmOverlay.Props(Component.translatable("fascinatedutils.social.confirm_cancel_request.title").getString(), Component.translatable("fascinatedutils.social.confirm_cancel_request.message", cancellableRequest.user().minecraftName()).getString(), Component.translatable("fascinatedutils.social.confirm_cancel_request.confirm").getString(), Component.translatable("fascinatedutils.social.confirm_cancel_request.deny").getString(), () -> {
                pendingCancelRequest = null;
            }, () -> {
                pendingCancelRequest = null;
                FascinatedUtils.SCHEDULED_POOL.execute(() -> {
                    try {
                        Alumite.INSTANCE.cancelFriendRequest(cancellableRequest.requestId());
                    } catch (AlumiteApiException exception) {
                        Toast.show().message(SocialErrors.message(exception)).error();
                    } catch (Exception exception) {
                        Toast.show().message(Component.translatable("fascinatedutils.social.error.generic").getString()).error();
                    }
                });
            }))));
        }
        if (preferredPresenceMenuOpen) {
            viewportLayers.add(UiSlot.keyed("social.overlay.presence", Ui.widgetSlot("social.presence.overlay", buildPreferredPresenceMenuOverlay())));
        }
        chatMessages.collectOverlayLayers(viewportLayers, selectedChannelId);
        return Ui.stackLayers(viewportLayers);
    }

    private FWidget buildSocialMainDock() {
        if (pendingRemoveFriend != null || pendingCancelRequest != null || chatMessages.hasActiveOverlay()) {
            preferredPresenceMenuOpen = false;
            chatMessages.suppressContextualOverlays();
        }

        FAbsoluteStackWidget stack = new FAbsoluteStackWidget();
        stack.addChild(new FWidget() {
            private FWidget leftDock;
            private FWidget rightDock;

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
                float leftWidth = Math.min(LEFT_PANEL_WIDTH, layoutWidth);
                float rightWidth = Math.max(0f, layoutWidth - leftWidth - SPLIT_GAP);
                leftDock = buildPanelContent(leftWidth);
                rightDock = buildChatContent(rightWidth);
                clearChildren();
                addChild(leftDock);
                addChild(rightDock);
                leftDock.layout(measure, layoutX, layoutY, leftWidth, layoutHeight);
                rightDock.layout(measure, layoutX + leftWidth + SPLIT_GAP, layoutY, rightWidth, layoutHeight);
            }
        });
        return stack;
    }

    private FWidget buildPanelContent(float panelW) {
        FRectWidget panelBg = new FRectWidget();
        panelBg.setFillColorArgb(0xEE1A1E24);
        panelBg.setBorder(UITheme.COLOR_BORDER, 1f);
        FWidget pane = SocialLeftPaneWidget.build(new SocialLeftPaneWidget.Props(PAD, TAB_H, panelBg, buildHeader(), buildTabs(), buildLeftList(panelW), buildFooter(), () -> activeTab == Tab.FRIENDS));
        return new FWidget() {
            {
                addChild(pane);
            }

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
                panelX = lx;
                panelY = ly;
                SocialMainWorkspaceComponent.this.panelW = lw;
                panelH = lh;
                pane.layout(measure, lx, ly, lw, lh);
            }
        };
    }

    private FWidget buildChatContent(float panelW) {
        FRectWidget panelBg = new FRectWidget();
        panelBg.setFillColorArgb(0xEE0F1318);
        panelBg.setBorder(UITheme.COLOR_BORDER, 1f);
        boolean showNoChannelSelected = activeTab == Tab.CHAT && selectedChannelId == null;
        FWidget body = activeTab == Tab.CHAT ? showNoChannelSelected ? SocialNoChannelSelectedWidget.build() : chatMessages.buildMessagesBody(selectedChannelId, panelW, messageScrollYRef) : buildFriendsProfilePane();
        FWidget footer = activeTab == Tab.CHAT && !showNoChannelSelected ? buildChatFooter() : buildSpacer();
        return SocialRightPaneWidget.build(new SocialRightPaneWidget.Props(PAD, panelBg, buildChatHeader(), body, footer));
    }

    private FWidget buildSpacer() {
        return new FWidget() {
            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
            }
        };
    }

    private FWidget buildHeader() {
        FWidget compactHeader = SocialHeaderWidget.build(new SocialHeaderWidget.Props(this::handleAddFriendFromHeader, this::handleNewChatAction, () -> searchOpen = !searchOpen, () -> searchOpen));
        FWidget preferredPresencePicker = buildPreferredPresencePicker();
        FOutlinedTextInputWidget searchInput = chatSearchInput;

        return new FWidget() {
            {
                addChild(compactHeader);
                addChild(searchInput);
                addChild(preferredPresencePicker);
            }

            @Override
            public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
                float searchH = searchOpen ? searchInput.intrinsicHeightForColumn(measure, widthBudget) + 6f : 0f;
                return 24f + 6f + searchH + PRESENCE_PICKER_H;
            }

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
                compactHeader.layout(measure, lx, ly, lw, 24f);
                float cursorY = ly + 24f + 6f;
                if (searchOpen) {
                    float inputH = searchInput.intrinsicHeightForColumn(measure, lw);
                    searchInput.layout(measure, lx, cursorY, lw, inputH);
                    searchInput.setVisible(true);
                    cursorY += inputH + 6f;
                }
                else {
                    searchInput.setVisible(false);
                    searchInput.layout(measure, lx, cursorY, lw, 0f);
                }
                preferredPresencePicker.layout(measure, lx, cursorY, lw, PRESENCE_PICKER_H);
            }
        };
    }

    private FWidget buildPreferredPresencePicker() {
        return new FWidget() {
            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
                preferredPresenceButtonX = lx;
                preferredPresenceButtonY = ly;
                preferredPresenceButtonW = lw;
                preferredPresenceButtonH = lh;
            }

            @Override
            protected void renderSelf(GuiRenderer graphics, UiFrameContext frame, float deltaSeconds) {
                float mouseX = frame.pointerX();
                float mouseY = frame.pointerY();
                boolean interactive = !preferredPresenceUpdatePending;
                int fillColor = preferredPresenceMenuOpen ? 0xFF2B3142 : containsPoint(mouseX, mouseY) && interactive ? 0xFF242A38 : 0xFF202531;
                int borderColor = preferredPresenceMenuOpen ? 0xFF6E7897 : containsPoint(mouseX, mouseY) && interactive ? 0xFF59617A : 0xFF454A60;
                graphics.fillRoundedRectFrame(x(), y(), w(), h(), 6f, borderColor, fillColor, 1f, 1f, RectCornerRoundMask.ALL);

                Presence presence = displayedPreferredPresence();
                float dotX = x() + 8f;
                float dotY = y() + (h() - PRESENCE_PICKER_DOT) / 2f;
                graphics.fillRoundedRect(dotX, dotY, PRESENCE_PICKER_DOT, PRESENCE_PICKER_DOT, PRESENCE_PICKER_DOT / 2f, presence.color(), RectCornerRoundMask.ALL);

                String label = preferredPresenceUpdatePending ? Component.translatable("fascinatedutils.social.presence.updating").getString() : preferredPresenceLabel(presence);
                float textY = y() + (h() - graphics.getFontCapHeight()) / 2f;
                graphics.drawText(label, dotX + PRESENCE_PICKER_DOT + 6f, textY, preferredPresenceUpdatePending ? FascinatedGuiTheme.INSTANCE.textMuted() : FascinatedGuiTheme.INSTANCE.textPrimary(), false, false);

                if (!preferredPresenceUpdatePending) {
                    graphics.drawText(preferredPresenceMenuOpen ? "\u25B4" : "\u25BE", x() + w() - 12f, textY, FascinatedGuiTheme.INSTANCE.textMuted(), false, false);
                }
            }

            @Override
            public PointerHitKind pointerHitKind() {
                return PointerHitKind.TARGET;
            }

            @Override
            public UiPointerCursor pointerCursor(float pointerX, float pointerY) {
                return preferredPresenceUpdatePending ? UiPointerCursor.DEFAULT : UiPointerCursor.HAND;
            }

            @Override
            public boolean mouseDown(float pointerX, float pointerY, int button) {
                if (button != 0) {
                    return false;
                }
                if (preferredPresenceUpdatePending) {
                    return true;
                }
                preferredPresenceMenuOpen = !preferredPresenceMenuOpen;
                return true;
            }
        };
    }

    private FWidget buildPreferredPresenceMenuOverlay() {
        FWidget menu = buildPreferredPresenceMenu();
        return new FWidget() {
            {
                addChild(menu);
            }

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
                float menuWidth = preferredPresenceMenuWidth(measure);
                float menuHeight = preferredPresenceMenuHeight();
                float menuX = preferredPresenceButtonX + preferredPresenceButtonW - menuWidth;
                float minMenuX = panelX + PAD;
                float maxMenuX = panelX + panelW - PAD - menuWidth;
                if (maxMenuX < minMenuX) {
                    menuX = minMenuX;
                }
                else {
                    menuX = Math.max(minMenuX, Math.min(menuX, maxMenuX));
                }
                float menuY = preferredPresenceButtonY + preferredPresenceButtonH + 4f;
                float maxMenuY = panelY + panelH - PAD - menuHeight;
                if (menuY > maxMenuY) {
                    menuY = Math.max(panelY + PAD, maxMenuY);
                }
                menu.layout(measure, menuX, menuY, menuWidth, menuHeight);
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
                graphics.drawRect(x(), y(), w(), h(), 0x9905050F);
                graphics.absolutePost(() -> menu.render(graphics, frame, deltaSeconds));
            }

            @Override
            public boolean mouseDown(float pointerX, float pointerY, int button) {
                if (button != 0) {
                    return false;
                }
                if (!menu.containsPoint(pointerX, pointerY)) {
                    preferredPresenceMenuOpen = false;
                    return true;
                }
                return false;
            }
        };
    }

    private FWidget buildPreferredPresenceMenu() {
        return new FWidget() {
            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
            }

            @Override
            protected void renderSelf(GuiRenderer graphics, UiFrameContext frame, float deltaSeconds) {
                float mouseX = frame.pointerX();
                float mouseY = frame.pointerY();
                graphics.fillRoundedRectFrame(x(), y(), w(), h(), UITheme.CORNER_RADIUS_MD, 0xFF454A60, 0xFF171B24, 1f, 1f, RectCornerRoundMask.ALL);

                float checkmarkWidth = graphics.measureTextWidth("\u2713", false);
                float rowY = y() + PRESENCE_MENU_PAD;
                Presence currentPresence = displayedPreferredPresence();
                for (Presence presence : SELECTABLE_PREFERRED_PRESENCES) {
                    boolean rowHovered = mouseX >= x() + 4f && mouseX < x() + w() - 4f && mouseY >= rowY && mouseY < rowY + PRESENCE_MENU_ROW_H;
                    boolean selected = currentPresence == presence;
                    int rowColor = selected ? 0x334960C8 : rowHovered ? 0x22FFFFFF : 0x00000000;
                    if (rowColor != 0) {
                        graphics.fillRoundedRect(x() + 4f, rowY, w() - 8f, PRESENCE_MENU_ROW_H, UITheme.CORNER_RADIUS_SM, rowColor, RectCornerRoundMask.ALL);
                    }

                    float dotX = x() + 12f;
                    float dotY = rowY + (PRESENCE_MENU_ROW_H - PRESENCE_PICKER_DOT) * 0.5f;
                    graphics.fillRoundedRect(dotX, dotY, PRESENCE_PICKER_DOT, PRESENCE_PICKER_DOT, PRESENCE_PICKER_DOT / 2f, presence.color(), RectCornerRoundMask.ALL);

                    float labelX = dotX + PRESENCE_PICKER_DOT + 8f;
                    float textY = rowY + (PRESENCE_MENU_ROW_H - graphics.getFontCapHeight()) * 0.5f;
                    graphics.drawText(preferredPresenceLabel(presence), labelX, textY, FascinatedGuiTheme.INSTANCE.textPrimary(), false, false);

                    if (selected) {
                        graphics.drawText("\u2713", x() + w() - 12f - checkmarkWidth, rowY + (PRESENCE_MENU_ROW_H - graphics.getFontCapHeight()) * 0.5f, 0xFF9DB4FF, false, false);
                    }
                    rowY += PRESENCE_MENU_ROW_H + PRESENCE_MENU_ROW_GAP;
                }
            }

            @Override
            public PointerHitKind pointerHitKind() {
                return PointerHitKind.TARGET;
            }

            @Override
            public UiPointerCursor pointerCursor(float pointerX, float pointerY) {
                return pointerX >= x() + 4f && pointerX < x() + w() - 4f && pointerY >= y() + PRESENCE_MENU_PAD && pointerY < y() + h() - PRESENCE_MENU_PAD ? UiPointerCursor.HAND : UiPointerCursor.DEFAULT;
            }

            @Override
            public boolean mouseDown(float pointerX, float pointerY, int button) {
                if (button != 0) {
                    return false;
                }

                float rowY = y() + PRESENCE_MENU_PAD;
                for (Presence presence : SELECTABLE_PREFERRED_PRESENCES) {
                    if (pointerX >= x() + 4f && pointerX < x() + w() - 4f && pointerY >= rowY && pointerY < rowY + PRESENCE_MENU_ROW_H) {
                        updatePreferredPresence(presence);
                        return true;
                    }
                    rowY += PRESENCE_MENU_ROW_H + PRESENCE_MENU_ROW_GAP;
                }

                return false;
            }
        };
    }

    private FWidget buildTabs() {
        final int friendCount = Alumite.INSTANCE.users().friends().size();
        final String chatLabel = Component.translatable("fascinatedutils.social.tab_chat").getString();
        final String friendsLabel = Component.translatable("fascinatedutils.social.tab_friends", friendCount).getString();
        return SocialModeTabsWidget.build(new SocialModeTabsWidget.Props(chatLabel, friendsLabel, Alumite.INSTANCE.users().incomingFriendRequests().size(), () -> activeTab == Tab.CHAT, () -> {
            if (activeTab != Tab.CHAT) {
                activeTab = Tab.CHAT;
                scrollYRef.setValue(0f);
            }
        }, () -> {
            if (activeTab != Tab.FRIENDS) {
                activeTab = Tab.FRIENDS;
                scrollYRef.setValue(0f);
            }
        }));
    }

    private FScrollColumnWidget buildLeftList(float panelW) {
        float innerW = panelW - 2f * PAD;
        Float savedY = scrollYRef.getValue();
        float scrollY = savedY == null ? 0f : savedY;
        if (activeTab == Tab.CHAT) {
            String searchQuery = chatSearchInput.value().trim().toLowerCase();
            List<Channel> channels = Alumite.INSTANCE.channels().all().stream().filter(channel -> searchQuery.isEmpty() || channelTitle(channel).toLowerCase().contains(searchQuery)).toList();
            return SocialChatListWidget.build(new SocialChatListWidget.Props(channels, scrollY, scrollYRef::setValue, channel -> buildChatChannelRow(channel, innerW), () -> buildEmptyState(Component.translatable("fascinatedutils.social.dm.no_chats").getString())));
        }
        return SocialFriendsListWidget.build(new SocialFriendsListWidget.Props(Alumite.INSTANCE.users().friends(), Alumite.INSTANCE.users().incomingFriendRequests(), Alumite.INSTANCE.users().outgoingFriendRequests(), scrollY, scrollYRef::setValue, friend -> buildFriendRow(friend, innerW), request -> SocialFriendRequestRowWidget.build(request, innerW, ROW_H), request -> SocialOutgoingRequestRowWidget.build(request, innerW, ROW_H, () -> pendingCancelRequest = request), this::buildSectionLabel, Component.translatable("fascinatedutils.social.requests_incoming").getString(), Component.translatable("fascinatedutils.social.requests_outgoing").getString(), () -> buildEmptyState(Component.translatable("fascinatedutils.social.no_friends").getString()), () -> buildEmptyState(Component.translatable("fascinatedutils.social.no_friends").getString())));
    }

    private FWidget buildChatChannelRow(Channel channel, float innerW) {
        String snippet = channelListSnippet(channel);
        String channelLabel = channelTitle(channel);
        String channelAvatarMinecraftUuid = channelListAvatarMinecraftUuid(channel);
        return SocialChatRowWidget.build(new SocialChatRowWidget.Props(channelLabel, channelAvatarMinecraftUuid, snippet, channelListPresenceColor(channel), selectedChannelId != null && selectedChannelId == channel.id(), channelHasUnread(channel), () -> selectChannel(channel.id(), false), channel.asDmChannel() != null ? () -> closeDmChannel(channel.id()) : null), innerW, ROW_H);
    }

    private String channelListSnippet(Channel channel) {
        LastMessagePreview preview = channel.lastMessagePreview();
        if (preview == null || preview.content() == null || preview.content().isBlank()) {
            return "";
        }
        if (preview.authorName() != null && !preview.authorName().isBlank()) {
            return preview.authorName() + ": " + preview.content();
        }
        return preview.content();
    }

    private boolean channelHasUnread(Channel channel) {
        LastMessagePreview preview = channel.lastMessagePreview();
        if (preview == null) {
            return false;
        }
        Integer lastReadMessageId = channel.lastReadMessageId();
        if (lastReadMessageId == null) {
            return true;
        }
        return preview.messageId() > lastReadMessageId;
    }

    private String channelTitle(Channel channel) {
        if (channel == null) {
            return Component.translatable("fascinatedutils.social.dm.title").getString();
        }
        GroupChannel groupChannel = channel.asGroupChannel();
        if (groupChannel != null) {
            if (groupChannel.name() != null && !groupChannel.name().isBlank()) {
                return groupChannel.name();
            }
            return "Channel #" + groupChannel.id();
        }
        DmChannel dmChannel = channel.asDmChannel();
        if (dmChannel != null && dmChannel.recipient() != null) {
            User recipient = displayUser(dmChannel.recipient());
            if (recipient != null && recipient.minecraftName() != null && !recipient.minecraftName().isBlank()) {
                return recipient.minecraftName();
            }
        }
        return Component.translatable("fascinatedutils.social.dm.title").getString();
    }

    private String channelListAvatarMinecraftUuid(Channel channel) {
        if (channel == null) {
            return null;
        }
        DmChannel dmChannel = channel.asDmChannel();
        if (dmChannel == null) {
            return null;
        }
        return dmAvatarMinecraftUuid(dmChannel.recipient());
    }

    private String dmAvatarMinecraftUuid(User recipientUser) {
        if (recipientUser == null) {
            return null;
        }
        User recipient = displayUser(recipientUser);
        if (recipient == null || recipient.minecraftUuid() == null || recipient.minecraftUuid().isBlank()) {
            return null;
        }
        return recipient.minecraftUuid();
    }

    private int channelListPresenceColor(Channel channel) {
        if (channel == null) {
            return UITheme.COLOR_ACCENT;
        }
        DmChannel dmChannel = channel.asDmChannel();
        if (dmChannel == null) {
            return UITheme.COLOR_ACCENT;
        }
        User recipient = dmChannel.recipient();
        if (recipient == null) {
            return Presence.OFFLINE.color();
        }
        Presence status = recipient.presence();
        return (status != null ? status : Presence.OFFLINE).color();
    }

    private FWidget buildFriendRow(Friend friend, float innerW) {
        User friendUser = displayUser(friend.user());
        return SocialFriendRowWidget.build(new SocialFriendRowWidget.Props(friendUser, presenceStatusLine(friendUser), Objects.equals(selectedFriendUserId, friend.user().id()), () -> selectedFriendUserId = friend.user().id(), () -> pendingRemoveFriend = friend), innerW, ROW_H);
    }

    private FWidget buildSectionLabel(String text) {
        return new FWidget() {
            @Override
            public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
                return measure.getFontCapHeight() + 8f;
            }

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
            }

            @Override
            protected void renderSelf(GuiRenderer graphics, UiFrameContext frame, float deltaSeconds) {
                float mouseX = frame.pointerX();
                float mouseY = frame.pointerY();
                float textY = y() + (h() - graphics.getFontCapHeight()) / 2f;
                graphics.drawText(text, x(), textY, FascinatedGuiTheme.INSTANCE.textMuted(), false, false);
                graphics.drawRect(x(), y() + h() - 1f, w(), 1f, 0x22FFFFFF);
            }
        };
    }

    private Presence displayedPreferredPresence() {
        return Alumite.INSTANCE.currentPreferredPresence();
    }

    private void updatePreferredPresence(Presence presence) {
        Presence currentPresence = displayedPreferredPresence();
        preferredPresenceMenuOpen = false;
        if (currentPresence == presence) {
            return;
        }

        preferredPresenceUpdatePending = true;

        FascinatedUtils.SCHEDULED_POOL.execute(() -> {
            try {
                Alumite.INSTANCE.updatePreferredPresence(presence);
            } catch (AlumiteApiException exception) {
                Toast.show().message(SocialErrors.message(exception)).error();
            } catch (Exception exception) {
                Toast.show().message(Component.translatable("fascinatedutils.social.error.generic").getString()).error();
            }

            Minecraft.getInstance().execute(() -> preferredPresenceUpdatePending = false);
        });
    }

    private float preferredPresenceMenuHeight() {
        return PRESENCE_MENU_PAD * 2f + SELECTABLE_PREFERRED_PRESENCES.length * PRESENCE_MENU_ROW_H + Math.max(0, SELECTABLE_PREFERRED_PRESENCES.length - 1) * PRESENCE_MENU_ROW_GAP;
    }

    private float preferredPresenceMenuWidth(UIRenderer measure) {
        int widestText = 0;
        for (Presence presence : SELECTABLE_PREFERRED_PRESENCES) {
            widestText = Math.max(widestText, measure.measureTextWidth(preferredPresenceLabel(presence), false));
        }
        float contentWidth = 12f + PRESENCE_PICKER_DOT + 8f + widestText + 8f + measure.measureTextWidth("\u2713", false) + 12f;
        return Math.max(PRESENCE_MENU_MIN_W, Math.min(PRESENCE_MENU_MAX_W, contentWidth));
    }

    private FWidget buildEmptyState(String message) {
        return new FWidget() {
            @Override
            public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
                return measure.getFontCapHeight() + 8f;
            }

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
            }

            @Override
            protected void renderSelf(GuiRenderer graphics, UiFrameContext frame, float deltaSeconds) {
                float mouseX = frame.pointerX();
                float mouseY = frame.pointerY();
                float centerX = x() + w() / 2f;
                float textY = y() + 4f;
                graphics.drawCenteredText(message, centerX, textY, FascinatedGuiTheme.INSTANCE.textMuted(), false, false);
            }
        };
    }

    private FWidget buildChatHeader() {
        return new FWidget() {
            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
            }

            @Override
            protected void renderSelf(GuiRenderer graphics, UiFrameContext frame, float deltaSeconds) {
                float titleY = y() + (h() - graphics.getFontCapHeight()) / 2f;
                if (activeTab == Tab.FRIENDS) {
                    Friend selectedFriend = selectedFriend();
                    String friendTitle = selectedFriend == null
                            ? Component.translatable("fascinatedutils.social.tab_friends").getString()
                            : "Friend Profile";
                    graphics.drawText(friendTitle, x(), titleY, FascinatedGuiTheme.INSTANCE.textPrimary(), true, false);
                    return;
                }
                Channel channel = selectedChannelId == null ? null : Alumite.INSTANCE.channels().get(selectedChannelId);
                String title = channel == null ? Component.translatable("fascinatedutils.social.dm.title").getString() : channelTitle(channel);
                float textLeft = x();
                DmChannel dmChannel = channel == null ? null : channel.asDmChannel();
                if (dmChannel != null && dmChannel.recipient() != null) {
                    String headerAvatarUuid = dmAvatarMinecraftUuid(dmChannel.recipient());
                    if (headerAvatarUuid != null && !headerAvatarUuid.isBlank()) {
                        float avatarX = x();
                        float avatarY = y() + (h() - CHAT_HEADER_AVATAR) / 2f;
                        Identifier avatarTexture = AvatarTextureCache.INSTANCE.get(headerAvatarUuid, () -> {});
                        String initial = title == null || title.isBlank() ? "?" : String.valueOf(Character.toUpperCase(title.charAt(0)));
                        if (avatarTexture != null) {
                            graphics.fillRoundedRect(avatarX, avatarY, CHAT_HEADER_AVATAR, CHAT_HEADER_AVATAR, 4f, 0xFF000000, RectCornerRoundMask.ALL);
                            graphics.drawTexture(avatarTexture, avatarX, avatarY, CHAT_HEADER_AVATAR, CHAT_HEADER_AVATAR, 0xFFFFFFFF);
                        }
                        else {
                            graphics.fillRoundedRect(avatarX, avatarY, CHAT_HEADER_AVATAR, CHAT_HEADER_AVATAR, 4f, 0xFF3B445A, RectCornerRoundMask.ALL);
                            graphics.drawCenteredText(initial, avatarX + CHAT_HEADER_AVATAR / 2f, avatarY + (CHAT_HEADER_AVATAR - graphics.getFontCapHeight()) / 2f, 0xFFFFFFFF, false, true);
                        }
                        textLeft = avatarX + CHAT_HEADER_AVATAR + 8f;
                    }
                }
                graphics.drawText(title, textLeft, titleY, FascinatedGuiTheme.INSTANCE.textPrimary(), true, false);
            }
        };
    }

    private FWidget buildFriendsProfilePane() {
        Friend selectedFriend = selectedFriend();
        if (selectedFriend == null) {
            return buildEmptyState(Component.translatable("fascinatedutils.social.dm.pick_friend").getString());
        }
        User selectedFriendUser = displayUser(selectedFriend.user());
        if (selectedFriendUser == null) {
            return buildEmptyState(Component.translatable("fascinatedutils.social.error.generic").getString());
        }
        return SocialFriendProfilePaneWidget.build(new SocialFriendProfilePaneWidget.Props(selectedFriendUser, () -> openDmForFriend(selectedFriend), () -> pendingRemoveFriend = selectedFriend));
    }

    private Friend selectedFriend() {
        if (selectedFriendUserId == null) {
            return null;
        }
        return Alumite.INSTANCE.users().friends().stream().filter(friend -> friend.user().id() == selectedFriendUserId).findFirst().orElse(null);
    }

    private FWidget buildChatFooter() {
        FOutlinedTextInputWidget dmInput = props().dmMessageInput();
        dmInput.setPlaceholderSupplier(() -> {
            Channel footerChannel = selectedChannelId == null ? null : Alumite.INSTANCE.channels().get(selectedChannelId);
            String label = footerChannel == null ? Component.translatable("fascinatedutils.social.dm.title").getString() : channelTitle(footerChannel);
            return "Message " + label;
        });
        return SocialChatComposerWidget.build(new SocialChatComposerWidget.Props(dmInput, this::sendSelectedMessage));
    }

    private void anchorMessageScrollToBottom() {
        messageScrollYRef.setValue(SocialChatMessagesHandler.MESSAGE_SCROLL_ANCHOR_BOTTOM);
    }

    private void selectChannel(int channelId, boolean switchToChat) {
        selectedChannelId = channelId;
        lastOpenedChannelId = channelId;
        anchorMessageScrollToBottom();
        if (switchToChat && activeTab != Tab.CHAT) {
            activeTab = Tab.CHAT;
            scrollYRef.setValue(0f);
        }
    }

    private void sendSelectedMessage() {
        if (selectedChannelId == null || sendMessagePending) {
            return;
        }
        String content = props().dmMessageInput().value().trim();
        if (content.isEmpty()) {
            return;
        }
        sendMessagePending = true;
        props().dmMessageInput().setValue("");
        Channel channel = Alumite.INSTANCE.channels().get(selectedChannelId);
        if (channel == null) {
            sendMessagePending = false;
            return;
        }
        FascinatedUtils.SCHEDULED_POOL.execute(() -> {
            boolean sent;
            try {
                channel.sendMessage(content);
                sent = true;
            } catch (Exception exception) {
                Toast.show().message(Component.translatable("fascinatedutils.social.error.generic").getString()).error();
                sent = false;
            }
            final boolean didSend = sent;
            Minecraft minecraftClient = Minecraft.getInstance();
            minecraftClient.execute(() -> {
                sendMessagePending = false;
                if (didSend) {
                    anchorMessageScrollToBottom();
                    minecraftClient.execute(this::anchorMessageScrollToBottom);
                }
            });
        });
    }

    private void handleNewChatAction() {
        activeTab = Tab.FRIENDS;
        scrollYRef.setValue(0f);
    }

    private void handleAddFriendFromHeader() {
        activeTab = Tab.FRIENDS;
        scrollYRef.setValue(0f);
        GuiFocusState.setFocusedId(props().addFriendInput().focusId());
    }

    private void openDmForFriend(Friend friend) {
        if (friend == null) {
            return;
        }
        FascinatedUtils.SCHEDULED_POOL.execute(() -> {
            try {
                DmChannel channel = Alumite.INSTANCE.channels().openDmAndCache(friend.user().id());
                if (channel == null) {
                    return;
                }
                Minecraft.getInstance().execute(() -> selectChannel(channel.id(), true));
            } catch (Exception exception) {
                Toast.show().message(Component.translatable("fascinatedutils.social.error.generic").getString()).error();
            }
        });
    }

    private void closeDmChannel(int channelId) {
        FascinatedUtils.SCHEDULED_POOL.execute(() -> {
            try {
                DmChannel channel = Alumite.INSTANCE.channels().get(channelId) instanceof DmChannel dmChannel ? dmChannel : null;
                if (channel == null) {
                    return;
                }
                channel.hide();
                Minecraft.getInstance().execute(() -> {
                    if (Objects.equals(selectedChannelId, channelId)) {
                        selectedChannelId = null;
                    }
                    if (Objects.equals(lastOpenedChannelId, channelId)) {
                        lastOpenedChannelId = null;
                    }
                });
            } catch (AlumiteApiException exception) {
                Minecraft.getInstance().execute(() -> Toast.show().message(SocialErrors.message(exception)).error());
            } catch (Exception exception) {
                Minecraft.getInstance().execute(() -> Toast.show().message(Component.translatable("fascinatedutils.social.error.generic").getString()).error());
            }
        });
    }

    private User displayUser(User user) {
        if (user == null) {
            return null;
        }
        if (user.resolved()) {
            return user;
        }
        try {
            return Alumite.INSTANCE.users().resolveUser(user.id());
        } catch (AlumiteApiException exception) {
            return null;
        }
    }

    private FWidget buildFooter() {
        FOutlinedTextInputWidget friendInput = props().addFriendInput();
        FButtonWidget addBtn = new FButtonWidget(() -> {
            String username = friendInput.value().trim();
            if (username.isEmpty()) {
                return;
            }
            friendInput.setValue("");
            FascinatedUtils.SCHEDULED_POOL.execute(() -> {
                try {
                    Alumite.INSTANCE.sendFriendRequest(username);
                    Toast.show().message("Friend request sent!").success();
                } catch (AlumiteApiException exception) {
                    Toast.show().message(SocialErrors.message(exception)).error();
                } catch (Exception exception) {
                    Toast.show().message(Component.translatable("fascinatedutils.social.error.generic").getString()).error();
                }
            });
        }, () -> Component.translatable("fascinatedutils.social.add_button").getString(), ADD_BTN_W, 1, 1f, 4f, 1f, 4f, 3f) {
            @Override
            protected int resolveButtonFillColorArgb(boolean hovered) {
                return hovered ? UITheme.COLOR_ACCENT_HOVER : UITheme.COLOR_ACCENT;
            }

            @Override
            protected int resolveButtonBorderColorArgb(boolean hovered) {
                return hovered ? UITheme.COLOR_ACCENT_HOVER : UITheme.COLOR_ACCENT;
            }
        };

        return new FWidget() {
            {
                addChild(friendInput);
                addChild(addBtn);
            }

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
                float inputW = lw - ADD_BTN_W - 4f;
                float inputH = friendInput.intrinsicHeightForColumn(measure, inputW);
                float inputY = ly + (lh - inputH) / 2f;
                friendInput.layout(measure, lx, inputY, inputW, inputH);
                addBtn.layout(measure, lx + inputW + 4f, ly + (lh - BTN_H) / 2f, ADD_BTN_W, BTN_H);
            }
        };
    }

    private enum Tab {CHAT, FRIENDS}

    /**
     * Stable props rebuilt every reconcile pass — carries retained fields owned by {@link cc.fascinated.fascinatedutils.gui.screens.SocialScreen}.
     *
     * @param viewportWidth  measured host width
     * @param viewportHeight measured host height
     * @param addFriendInput persisted add-friend text field widget
     * @param onCloseScreen  closes the owning screen when the header exit control is clicked
     */
    public record Props(float viewportWidth, float viewportHeight, FOutlinedTextInputWidget addFriendInput,
                        FOutlinedTextInputWidget dmMessageInput, Runnable onCloseScreen,
                        Consumer<Boolean> presenceMenuOpenSink) {}

}
