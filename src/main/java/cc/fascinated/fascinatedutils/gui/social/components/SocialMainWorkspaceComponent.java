package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.gui.core.UiFrameContext;

import cc.fascinated.fascinatedutils.FascinatedUtils;
import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.AlumiteApiException;
import cc.fascinated.fascinatedutils.api.Errors;
import cc.fascinated.fascinatedutils.api.channel.Channel;
import cc.fascinated.fascinatedutils.api.channel.ChannelDetail;
import cc.fascinated.fascinatedutils.api.channel.ChannelMessage;
import cc.fascinated.fascinatedutils.api.channel.DmChannel;
import cc.fascinated.fascinatedutils.api.channel.GroupChannel;
import cc.fascinated.fascinatedutils.api.channel.GroupMember;
import cc.fascinated.fascinatedutils.api.channel.LastMessagePreview;
import cc.fascinated.fascinatedutils.api.friend.Friend;
import cc.fascinated.fascinatedutils.api.friend.PendingFriendRequest;
import cc.fascinated.fascinatedutils.api.user.Presence;
import cc.fascinated.fascinatedutils.api.user.User;
import cc.fascinated.fascinatedutils.common.TimeUtils;
import cc.fascinated.fascinatedutils.gui.AvatarTextureCache;
import cc.fascinated.fascinatedutils.gui.core.Align;
import cc.fascinated.fascinatedutils.gui.core.GuiFocusState;
import cc.fascinated.fascinatedutils.gui.core.PointerHitKind;
import cc.fascinated.fascinatedutils.gui.core.Ref;
import cc.fascinated.fascinatedutils.gui.core.TextOverflow;
import cc.fascinated.fascinatedutils.gui.core.UiPointerCursor;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class SocialMainWorkspaceComponent extends UiComponent<SocialMainWorkspaceComponent.Props> {
    private static final float LEFT_PANEL_WIDTH = 250f;
    private static final float SPLIT_GAP = 10f;
    private static final float PAD = 10f;
    private static final float ROW_H = 44f;
    private static final float BTN_H = 20f;
    private static final float BTN_W = 20f;
    private static final float ADD_BTN_W = 36f;
    private static final float TAB_H = 26f;
    private static final float PRESENCE_PICKER_H = 24f;
    private static final float PRESENCE_PICKER_DOT = 8f;
    private static final float PRESENCE_MENU_MIN_W = 176f;
    private static final float PRESENCE_MENU_MAX_W = 220f;
    private static final float PRESENCE_MENU_PAD = 6f;
    private static final float PRESENCE_MENU_ROW_H = 30f;
    private static final float PRESENCE_MENU_ROW_GAP = 4f;
    private static final float AVATAR_SIZE = 32f;
    private static final float CHAT_HEADER_AVATAR = 28f;
    private static final int FOCUS_CHAT_SEARCH = 7112;
    private static final Presence[] SELECTABLE_PREFERRED_PRESENCES = {
            Presence.ONLINE,
            Presence.AWAY,
            Presence.DO_NOT_DISTURB,
            Presence.INVISIBLE
    };
    private static final int[] BADGE_COLORS = {
            0xFF6B5B95, 0xFF88B04B, 0xFF955251, 0xFF009B77,
            0xFF45B8AC, 0xFF5B5EA6, 0xFFB565A7, 0xFFDD4132
    };
    private static Integer lastOpenedChannelId;
    /**
     * Stored in {@link #messageScrollYRef}; {@link FScrollColumnWidget} clamps to the real max scroll so the
     * transcript opens pinned to the newest messages.
     */
    private static final float MESSAGE_SCROLL_ANCHOR_BOTTOM = Float.MAX_VALUE;

    private enum Tab { CHAT, FRIENDS }

    private final Ref<Float> scrollYRef = Ref.of(0f);
    private final Ref<Float> messageScrollYRef = Ref.of(MESSAGE_SCROLL_ANCHOR_BOTTOM);

    private Tab activeTab = Tab.CHAT;
    private Integer selectedChannelId;
    private Integer selectedFriendUserId;
    private Integer loadingMessagesChannelId;
    private Integer loadingDetailChannelId;
    private String selectedChannelDisplayName;
    private boolean searchOpen;
    private final FOutlinedTextInputWidget chatSearchInput = new FOutlinedTextInputWidget(
            FOCUS_CHAT_SEARCH, 40, 20f,
            () -> Component.translatable("fascinatedutils.social.search_placeholder").getString());
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
            viewportLayers.add(UiSlot.keyed("social.popup.remove." + friend.user().id(),
                    SocialDestructiveFullscreenConfirmOverlay.view(new SocialDestructiveFullscreenConfirmOverlay.Props(
                            Component.translatable("fascinatedutils.social.confirm_remove_friend.title").getString(),
                            Component.translatable("fascinatedutils.social.confirm_remove_friend.message", friend.user().minecraftName()).getString(),
                            Component.translatable("fascinatedutils.social.confirm_remove_friend.confirm").getString(),
                            Component.translatable("fascinatedutils.social.confirm_remove_friend.deny").getString(),
                            () -> {
                                pendingRemoveFriend = null;
                            },
                            () -> {
                                pendingRemoveFriend = null;
                                FascinatedUtils.SCHEDULED_POOL.execute(() -> {
                                    try {
                                        Alumite.INSTANCE.removeFriend(friend.user().id());
                                    }
                                    catch (AlumiteApiException exception) {
                                        Toast.show().message(socialErrorMessage(exception)).error();
                                    }
                                    catch (Exception exception) {
                                        Toast.show().message(Component.translatable("fascinatedutils.social.error.generic").getString()).error();
                                    }
                                });
                            }))));
        }
        if (pendingCancelRequest != null) {
            PendingFriendRequest cancellableRequest = pendingCancelRequest;
            viewportLayers.add(UiSlot.keyed("social.popup.cancel." + cancellableRequest.requestId(),
                    SocialDestructiveFullscreenConfirmOverlay.view(new SocialDestructiveFullscreenConfirmOverlay.Props(
                            Component.translatable("fascinatedutils.social.confirm_cancel_request.title").getString(),
                            Component.translatable("fascinatedutils.social.confirm_cancel_request.message", cancellableRequest.user().minecraftName()).getString(),
                            Component.translatable("fascinatedutils.social.confirm_cancel_request.confirm").getString(),
                            Component.translatable("fascinatedutils.social.confirm_cancel_request.deny").getString(),
                            () -> {
                                pendingCancelRequest = null;
                            },
                            () -> {
                                pendingCancelRequest = null;
                                FascinatedUtils.SCHEDULED_POOL.execute(() -> {
                                    try {
                                        Alumite.INSTANCE.cancelFriendRequest(cancellableRequest.requestId());
                                    }
                                    catch (AlumiteApiException exception) {
                                        Toast.show().message(socialErrorMessage(exception)).error();
                                    }
                                    catch (Exception exception) {
                                        Toast.show().message(Component.translatable("fascinatedutils.social.error.generic").getString()).error();
                                    }
                                });
                            }))));
        }
        if (preferredPresenceMenuOpen) {
            viewportLayers.add(UiSlot.keyed("social.overlay.presence",
                    Ui.widgetSlot("social.presence.overlay", buildPreferredPresenceMenuOverlay())));
        }
        return Ui.stackLayers(viewportLayers);
    }

    /**
     * Stable props rebuilt every reconcile pass — carries retained fields owned by {@link cc.fascinated.fascinatedutils.gui.screens.SocialScreen}.
     *
     * @param viewportWidth  measured host width
     * @param viewportHeight measured host height
     * @param addFriendInput persisted add-friend text field widget
     * @param onCloseScreen closes the owning screen when the header exit control is clicked
     */
    public record Props(float viewportWidth, float viewportHeight,
                        FOutlinedTextInputWidget addFriendInput,
                        FOutlinedTextInputWidget dmMessageInput,
                        Runnable onCloseScreen,
                        Consumer<Boolean> presenceMenuOpenSink) {
    }

    private FWidget buildSocialMainDock() {
        if (pendingRemoveFriend != null || pendingCancelRequest != null) {
            preferredPresenceMenuOpen = false;
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
        FWidget pane = SocialLeftPaneWidget.build(new SocialLeftPaneWidget.Props(
                PAD,
                TAB_H,
                panelBg,
                buildHeader(),
                buildTabs(),
                buildLeftList(panelW),
                buildFooter(),
                () -> activeTab == Tab.FRIENDS
        ));
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
        FWidget body = activeTab == Tab.CHAT
            ? showNoChannelSelected ? SocialNoChannelSelectedWidget.build() : buildChatMessages(panelW)
            : buildFriendsProfilePane();
        FWidget footer = activeTab == Tab.CHAT && !showNoChannelSelected ? buildChatFooter() : buildSpacer();
        return SocialRightPaneWidget.build(new SocialRightPaneWidget.Props(
                PAD,
                panelBg,
                buildChatHeader(),
            body,
            footer
        ));
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
        FWidget compactHeader = SocialHeaderWidget.build(new SocialHeaderWidget.Props(
                this::handleAddFriendFromHeader,
                this::handleNewChatAction,
                () -> searchOpen = !searchOpen,
                () -> searchOpen
        ));
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
                } else {
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
                int fillColor = preferredPresenceMenuOpen
                        ? 0xFF2B3142
                        : containsPoint(mouseX, mouseY) && interactive ? 0xFF242A38 : 0xFF202531;
                int borderColor = preferredPresenceMenuOpen
                        ? 0xFF6E7897
                        : containsPoint(mouseX, mouseY) && interactive ? 0xFF59617A : 0xFF454A60;
                graphics.fillRoundedRectFrame(x(), y(), w(), h(), 6f, borderColor, fillColor,
                        1f, 1f, RectCornerRoundMask.ALL);

                Presence presence = displayedPreferredPresence();
                float dotX = x() + 8f;
                float dotY = y() + (h() - PRESENCE_PICKER_DOT) / 2f;
                graphics.fillRoundedRect(dotX, dotY, PRESENCE_PICKER_DOT, PRESENCE_PICKER_DOT,
                        PRESENCE_PICKER_DOT / 2f, presence.color(), RectCornerRoundMask.ALL);

                String label = preferredPresenceUpdatePending
                        ? Component.translatable("fascinatedutils.social.presence.updating").getString()
                        : preferredPresenceLabel(presence);
                float textY = y() + (h() - graphics.getFontCapHeight()) / 2f;
                graphics.drawText(label, dotX + PRESENCE_PICKER_DOT + 6f, textY,
                        preferredPresenceUpdatePending
                                ? FascinatedGuiTheme.INSTANCE.textMuted()
                                : FascinatedGuiTheme.INSTANCE.textPrimary(),
                        false, false);

                if (!preferredPresenceUpdatePending) {
                    graphics.drawText(preferredPresenceMenuOpen ? "\u25B4" : "\u25BE", x() + w() - 12f, textY,
                            FascinatedGuiTheme.INSTANCE.textMuted(), false, false);
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
                } else {
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
                graphics.fillRoundedRectFrame(x(), y(), w(), h(), UITheme.CORNER_RADIUS_MD, 0xFF454A60, 0xFF171B24,
                        1f, 1f, RectCornerRoundMask.ALL);

                float lineGap = 1f;
                float textBlockHeight = graphics.getFontHeight() * 2f + lineGap;
                float checkmarkWidth = graphics.measureTextWidth("\u2713", false);
                float rowY = y() + PRESENCE_MENU_PAD;
                Presence currentPresence = displayedPreferredPresence();
                for (Presence presence : SELECTABLE_PREFERRED_PRESENCES) {
                    boolean rowHovered = mouseX >= x() + 4f && mouseX < x() + w() - 4f
                            && mouseY >= rowY && mouseY < rowY + PRESENCE_MENU_ROW_H;
                    boolean selected = currentPresence == presence;
                    int rowColor = selected ? 0x334960C8 : rowHovered ? 0x22FFFFFF : 0x00000000;
                    if (rowColor != 0) {
                        graphics.fillRoundedRect(x() + 4f, rowY, w() - 8f, PRESENCE_MENU_ROW_H, UITheme.CORNER_RADIUS_SM,
                                rowColor, RectCornerRoundMask.ALL);
                    }

                    float dotX = x() + 12f;
                            float dotY = rowY + (PRESENCE_MENU_ROW_H - PRESENCE_PICKER_DOT) * 0.5f;
                    graphics.fillRoundedRect(dotX, dotY, PRESENCE_PICKER_DOT, PRESENCE_PICKER_DOT,
                            PRESENCE_PICKER_DOT / 2f, presence.color(), RectCornerRoundMask.ALL);

                    float labelX = dotX + PRESENCE_PICKER_DOT + 8f;
                            float textY = rowY + (PRESENCE_MENU_ROW_H - textBlockHeight) * 0.5f;
                            graphics.drawText(preferredPresenceLabel(presence), labelX, textY,
                            FascinatedGuiTheme.INSTANCE.textPrimary(), false, false);
                            graphics.drawText(preferredPresenceDescription(presence), labelX, textY + graphics.getFontHeight() + lineGap,
                            FascinatedGuiTheme.INSTANCE.textMuted(), false, false);

                    if (selected) {
                            graphics.drawText("\u2713", x() + w() - 12f - checkmarkWidth,
                                rowY + (PRESENCE_MENU_ROW_H - graphics.getFontCapHeight()) * 0.5f,
                                0xFF9DB4FF, false, false);
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
                return pointerX >= x() + 4f && pointerX < x() + w() - 4f
                        && pointerY >= y() + PRESENCE_MENU_PAD && pointerY < y() + h() - PRESENCE_MENU_PAD
                        ? UiPointerCursor.HAND
                        : UiPointerCursor.DEFAULT;
            }

            @Override
            public boolean mouseDown(float pointerX, float pointerY, int button) {
                if (button != 0) {
                    return false;
                }

                float rowY = y() + PRESENCE_MENU_PAD;
                for (Presence presence : SELECTABLE_PREFERRED_PRESENCES) {
                    if (pointerX >= x() + 4f && pointerX < x() + w() - 4f
                            && pointerY >= rowY && pointerY < rowY + PRESENCE_MENU_ROW_H) {
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
        return SocialModeTabsWidget.build(new SocialModeTabsWidget.Props(
                chatLabel,
                friendsLabel,
            Alumite.INSTANCE.users().incomingFriendRequests().size(),
                () -> activeTab == Tab.CHAT,
                () -> {
                    if (activeTab != Tab.CHAT) {
                        activeTab = Tab.CHAT;
                        scrollYRef.setValue(0f);
                    }
                },
                () -> {
                    if (activeTab != Tab.FRIENDS) {
                        activeTab = Tab.FRIENDS;
                        scrollYRef.setValue(0f);
                    }
                }
        ));
    }

    private FScrollColumnWidget buildLeftList(float panelW) {
        float innerW = panelW - 2f * PAD;
        Float savedY = scrollYRef.getValue();
        float scrollY = savedY == null ? 0f : savedY;
        if (activeTab == Tab.CHAT) {
            String searchQuery = chatSearchInput.value().trim().toLowerCase();
            List<Channel> channels = Alumite.INSTANCE.channels().all().stream()
                    .filter(channel -> searchQuery.isEmpty()
                            || channelListTitle(channel).toLowerCase().contains(searchQuery))
                    .toList();
            return SocialChatListWidget.build(new SocialChatListWidget.Props(
                    channels,
                    scrollY,
                    scrollYRef::setValue,
                    channel -> buildChatChannelRow(channel, innerW),
                    () -> buildEmptyState(Component.translatable("fascinatedutils.social.dm.no_chats").getString())
            ));
        }
        return SocialFriendsListWidget.build(new SocialFriendsListWidget.Props(
            Alumite.INSTANCE.users().friends(),
            Alumite.INSTANCE.users().incomingFriendRequests(),
            Alumite.INSTANCE.users().outgoingFriendRequests(),
                scrollY,
                scrollYRef::setValue,
                friend -> buildFriendRow(friend, innerW),
                request -> buildRequestRow(request, innerW),
                request -> buildOutgoingRequestRow(request, innerW),
                this::buildSectionLabel,
                Component.translatable("fascinatedutils.social.requests_incoming").getString(),
                Component.translatable("fascinatedutils.social.requests_outgoing").getString(),
                () -> buildEmptyState(Component.translatable("fascinatedutils.social.no_friends").getString()),
                () -> buildEmptyState(Component.translatable("fascinatedutils.social.no_friends").getString())
        ));
    }

    private FWidget buildChatChannelRow(Channel channel, float innerW) {
        String snippet = channelListSnippet(channel);
        String channelLabel = channelListTitle(channel);
        String channelAvatarMinecraftUuid = channelListAvatarMinecraftUuid(channel);
        return SocialChatRowWidget.build(new SocialChatRowWidget.Props(
                channelLabel,
                channelAvatarMinecraftUuid,
                snippet,
                channelListPresenceColor(channel),
                selectedChannelId != null && selectedChannelId == channel.id(),
                channelHasUnread(channel),
                () -> selectChannel(channel.id(), false, channelLabel),
                channel.asDmChannel() != null ? () -> closeDmChannel(channel.id()) : null
        ), innerW, ROW_H);
    }

    private String channelListSnippet(Channel channel) {
        LastMessagePreview preview = channel.lastMessagePreview();
        if (preview != null && preview.content() != null && !preview.content().isBlank()) {
            if (preview.authorName() != null && !preview.authorName().isBlank()) {
                return preview.authorName() + ": " + preview.content();
            }
            return preview.content();
        }
        List<ChannelMessage> channelMessages = channel.messagesOrNull();
        if (channelMessages == null || channelMessages.isEmpty()) {
            return "";
        }
        return channelMessages.get(channelMessages.size() - 1).content();
    }

    private boolean channelHasUnread(Channel channel) {
        int lastMessageId;
        LastMessagePreview preview = channel.lastMessagePreview();
        if (preview != null) {
            lastMessageId = preview.messageId();
        } else {
            List<ChannelMessage> channelMessages = channel.messagesOrNull();
            if (channelMessages == null || channelMessages.isEmpty()) {
                return false;
            }
            lastMessageId = channelMessages.get(channelMessages.size() - 1).id();
        }
        Integer lastReadMessageId = channel.lastReadMessageId();
        if (lastReadMessageId == null) {
            return true;
        }
        return lastMessageId > lastReadMessageId;
    }

    private String channelListTitle(Channel channel) {
        if (channel == null) {
            return Component.translatable("fascinatedutils.social.dm.title").getString();
        }
        if (channel.name() != null && !channel.name().isBlank()) {
            return channel.name();
        }
        DmChannel dmChannel = channel.asDmChannel();
        if (dmChannel == null) {
            return "Channel #" + channel.id();
        }
        if (!dmChannel.detailLoaded()) {
            loadDetail(channel.id());
            return Component.translatable("fascinatedutils.social.dm.title").getString();
        }
        return channelTitle(dmChannel);
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
        User displayRecipient = displayUser(recipient);
        if (displayRecipient == null) {
            return Presence.OFFLINE.color();
        }
        Presence status = displayRecipient.presence();
        if (status == null) {
            status = Presence.OFFLINE;
        }
        return status.color();
    }

    private FWidget buildFriendRow(Friend friend, float innerW) {
        User friendUser = displayUser(friend.user());
        Presence status = friendUser == null || friendUser.presence() == null ? Presence.OFFLINE : friendUser.presence();
        String minecraftName = friendUser == null || friendUser.minecraftName() == null ? "..." : friendUser.minecraftName();
        String minecraftUuid = friendUser == null ? null : friendUser.minecraftUuid();
        return SocialFriendRowWidget.build(new SocialFriendRowWidget.Props(
                minecraftName,
                minecraftUuid,
                presenceStatusLine(friendUser),
                status.color(),
                Objects.equals(selectedFriendUserId, friend.user().id()),
                () -> selectedFriendUserId = friend.user().id(),
                () -> pendingRemoveFriend = friend
        ), innerW, ROW_H);
    }

    private FWidget buildRequestRow(PendingFriendRequest request, float innerW) {
        FLabelWidget nameLabel = new FLabelWidget();
        nameLabel.setText(request.user().minecraftName());
        nameLabel.setColorArgb(FascinatedGuiTheme.INSTANCE.textPrimary());
        nameLabel.setTextBold(true);
        nameLabel.setOverflow(TextOverflow.ELLIPSIS);
        nameLabel.setAlignY(Align.CENTER);

        return new FWidget() {
            {
                addChild(nameLabel);
            }

            @Override
            public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) { return ROW_H; }

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, innerW, lh);
                float nameX = lx + 4f + AVATAR_SIZE + 6f;
                float nameMaxX = lx + innerW - 2f * (BTN_W + 2f) - 8f;
                nameLabel.layout(measure, nameX, ly, nameMaxX - nameX, lh);
            }

            @Override
            protected void renderSelf(GuiRenderer graphics, UiFrameContext frame, float deltaSeconds) {
        float mouseX = frame.pointerX();
        float mouseY = frame.pointerY();
                boolean rowHovered = containsPoint(mouseX, mouseY);
                graphics.fillRoundedRect(x(), y(), w(), h(), UITheme.CORNER_RADIUS_SM,
                        rowHovered ? 0x22FFFFFF : UITheme.COLOR_BACKGROUND, RectCornerRoundMask.ALL);

                float avatarX = x() + 4f;
                float avatarY = y() + (h() - AVATAR_SIZE) / 2f;
                Identifier avatarTexture = AvatarTextureCache.INSTANCE.get(
                        request.user().minecraftUuid(), () -> {});
                if (avatarTexture != null) {
                    graphics.fillRoundedRect(avatarX, avatarY, AVATAR_SIZE, AVATAR_SIZE, 4f, 0xFF000000, RectCornerRoundMask.ALL);
                    graphics.drawTexture(avatarTexture, avatarX, avatarY, AVATAR_SIZE, AVATAR_SIZE, 0xFFFFFFFF);
                } else {
                    int badgeColor = avatarBadgeColor(request.user().minecraftName());
                    graphics.fillRoundedRect(avatarX, avatarY, AVATAR_SIZE, AVATAR_SIZE, 4f, badgeColor, RectCornerRoundMask.ALL);
                    graphics.drawCenteredText(initials(request.user().minecraftName()),
                            avatarX + AVATAR_SIZE / 2f,
                            avatarY + (AVATAR_SIZE - graphics.getFontCapHeight()) / 2f,
                            0xFFFFFFFF, false, true);
                }

                float declineBtnX = x() + w() - BTN_W - 4f;
                float acceptBtnX = declineBtnX - BTN_W - 2f;
                float btnY = y() + (h() - BTN_H) / 2f;
                boolean acceptHovered = mouseX >= acceptBtnX && mouseX < acceptBtnX + BTN_W
                        && mouseY >= btnY && mouseY < btnY + BTN_H;
                boolean declineHovered = mouseX >= declineBtnX && mouseX < declineBtnX + BTN_W
                        && mouseY >= btnY && mouseY < btnY + BTN_H;

                graphics.fillRoundedRect(acceptBtnX, btnY, BTN_W, BTN_H, 4f,
                        acceptHovered ? 0xAA1F5C1F : 0x22FFFFFF, RectCornerRoundMask.ALL);
                graphics.drawCenteredText("\u2713", acceptBtnX + BTN_W / 2f,
                        btnY + (BTN_H - graphics.getFontCapHeight()) / 2f,
                        acceptHovered ? 0xFF55FF55 : FascinatedGuiTheme.INSTANCE.textMuted(), false, false);

                graphics.fillRoundedRect(declineBtnX, btnY, BTN_W, BTN_H, 4f,
                        declineHovered ? 0xAA5C1F1F : 0x22FFFFFF, RectCornerRoundMask.ALL);
                graphics.drawCenteredText("\u2715", declineBtnX + BTN_W / 2f,
                        btnY + (BTN_H - graphics.getFontCapHeight()) / 2f,
                        declineHovered ? 0xFFFF5555 : FascinatedGuiTheme.INSTANCE.textMuted(), false, false);
            }

            @Override
            public PointerHitKind pointerHitKind() {
                return PointerHitKind.TARGET;
            }

            @Override
            public UiPointerCursor pointerCursor(float pointerX, float pointerY) { return UiPointerCursor.HAND; }

            @Override
            public boolean mouseDown(float pointerX, float pointerY, int button) {
                if (button != 0) { return false; }
                float declineBtnX = x() + w() - BTN_W - 4f;
                float acceptBtnX = declineBtnX - BTN_W - 2f;
                float btnY = y() + (h() - BTN_H) / 2f;
                if (pointerX >= acceptBtnX && pointerX < acceptBtnX + BTN_W
                        && pointerY >= btnY && pointerY < btnY + BTN_H) {
                    FascinatedUtils.SCHEDULED_POOL.execute(() -> {
                        try {
                            boolean accepted = Alumite.INSTANCE.acceptFriendRequest(request.requestId());
                            if (accepted) {
                                Toast.show().message("You're now friends with " + request.user().minecraftName() + "!").success();
                            }
                        } catch (AlumiteApiException exception) {
                            Toast.show().message(socialErrorMessage(exception)).error();
                        } catch (Exception exception) {
                            Toast.show().message(Component.translatable("fascinatedutils.social.error.generic").getString()).error();
                        }
                    });
                    return true;
                }
                if (pointerX >= declineBtnX && pointerX < declineBtnX + BTN_W
                        && pointerY >= btnY && pointerY < btnY + BTN_H) {
                    FascinatedUtils.SCHEDULED_POOL.execute(() -> {
                        try {
                            Alumite.INSTANCE.declineFriendRequest(request.requestId());
                        } catch (AlumiteApiException exception) {
                            Toast.show().message(socialErrorMessage(exception)).error();
                        } catch (Exception exception) {
                            Toast.show().message(Component.translatable("fascinatedutils.social.error.generic").getString()).error();
                        }
                    });
                    return true;
                }
                return false;
            }
        };
    }

    private FWidget buildOutgoingRequestRow(PendingFriendRequest request, float innerW) {
        FLabelWidget nameLabel = new FLabelWidget();
        nameLabel.setText(request.user().minecraftName());
        nameLabel.setColorArgb(FascinatedGuiTheme.INSTANCE.textMuted());
        nameLabel.setOverflow(TextOverflow.ELLIPSIS);
        nameLabel.setAlignY(Align.CENTER);

        return new FWidget() {
            {
                addChild(nameLabel);
            }

            @Override
            public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) { return ROW_H; }

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, innerW, lh);
                float nameX = lx + 4f + AVATAR_SIZE + 6f;
                float nameMaxX = lx + innerW - BTN_W - 8f;
                nameLabel.layout(measure, nameX, ly, nameMaxX - nameX, lh);
            }

            @Override
            protected void renderSelf(GuiRenderer graphics, UiFrameContext frame, float deltaSeconds) {
        float mouseX = frame.pointerX();
        float mouseY = frame.pointerY();
                boolean rowHovered = containsPoint(mouseX, mouseY);
                graphics.fillRoundedRect(x(), y(), w(), h(), UITheme.CORNER_RADIUS_SM,
                        rowHovered ? 0x22FFFFFF : UITheme.COLOR_BACKGROUND, RectCornerRoundMask.ALL);

                float avatarX = x() + 4f;
                float avatarY = y() + (h() - AVATAR_SIZE) / 2f;
                String receiverUuid = request.user().minecraftUuid();
                Identifier avatarTexture = receiverUuid != null
                        ? AvatarTextureCache.INSTANCE.get(receiverUuid, () -> {})
                        : null;
                if (avatarTexture != null) {
                    graphics.fillRoundedRect(avatarX, avatarY, AVATAR_SIZE, AVATAR_SIZE, 4f, 0xFF000000, RectCornerRoundMask.ALL);
                    graphics.drawTexture(avatarTexture, avatarX, avatarY, AVATAR_SIZE, AVATAR_SIZE, 0xFFFFFFFF);
                } else {
                    int badgeColor = avatarBadgeColor(request.user().minecraftName());
                    graphics.fillRoundedRect(avatarX, avatarY, AVATAR_SIZE, AVATAR_SIZE, 4f, badgeColor, RectCornerRoundMask.ALL);
                    graphics.drawCenteredText(initials(request.user().minecraftName()),
                            avatarX + AVATAR_SIZE / 2f,
                            avatarY + (AVATAR_SIZE - graphics.getFontCapHeight()) / 2f,
                            0xFFFFFFFF, false, true);
                }

                float cancelBtnX = x() + w() - BTN_W - 4f;
                float cancelBtnY = y() + (h() - BTN_H) / 2f;
                boolean btnHovered = mouseX >= cancelBtnX && mouseX < cancelBtnX + BTN_W
                        && mouseY >= cancelBtnY && mouseY < cancelBtnY + BTN_H;
                graphics.fillRoundedRect(cancelBtnX, cancelBtnY, BTN_W, BTN_H, 4f,
                        btnHovered ? 0xAA5C1F1F : 0x22FFFFFF, RectCornerRoundMask.ALL);
                int cancelTextColor = btnHovered ? 0xFFFF5555 : FascinatedGuiTheme.INSTANCE.textMuted();
                graphics.drawCenteredText("\u2715", cancelBtnX + BTN_W / 2f,
                        cancelBtnY + (BTN_H - graphics.getFontCapHeight()) / 2f,
                        cancelTextColor, false, false);
            }

            @Override
            public PointerHitKind pointerHitKind() {
                return PointerHitKind.TARGET;
            }

            @Override
            public UiPointerCursor pointerCursor(float pointerX, float pointerY) { return UiPointerCursor.HAND; }

            @Override
            public boolean mouseDown(float pointerX, float pointerY, int button) {
                if (button != 0) { return false; }
                float cancelBtnX = x() + w() - BTN_W - 4f;
                float cancelBtnY = y() + (h() - BTN_H) / 2f;
                if (pointerX >= cancelBtnX && pointerX < cancelBtnX + BTN_W
                        && pointerY >= cancelBtnY && pointerY < cancelBtnY + BTN_H) {
                    pendingCancelRequest = request;
                    return true;
                }
                return false;
            }
        };
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

    private static String presenceStatusLine(User user) {
        Presence presence = user == null || user.presence() == null ? Presence.OFFLINE : user.presence();
        if (presence == Presence.OFFLINE) {
            if (user != null && user.lastSeen() != null) {
                long lastSeenTime = user.lastSeen().getTime();
                return Component.translatable("fascinatedutils.social.presence.offline").getString()
                        + " · " + TimeUtils.timeAgo(lastSeenTime, System.currentTimeMillis() - lastSeenTime < 61_000 ? 1 : 2);
            }
            return Component.translatable("fascinatedutils.social.presence.offline").getString();
        }
        return preferredPresenceLabel(presence);
    }

    private static String presenceLabelOnly(User user) {
        Presence presence = user == null || user.presence() == null ? Presence.OFFLINE : user.presence();
        return preferredPresenceLabel(presence);
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
                Toast.show().message(socialErrorMessage(exception)).error();
            } catch (Exception exception) {
                Toast.show().message(Component.translatable("fascinatedutils.social.error.generic").getString()).error();
            }

            Minecraft.getInstance().execute(() -> preferredPresenceUpdatePending = false);
        });
    }

    private float preferredPresenceMenuHeight() {
        return PRESENCE_MENU_PAD * 2f
                + SELECTABLE_PREFERRED_PRESENCES.length * PRESENCE_MENU_ROW_H
                + Math.max(0, SELECTABLE_PREFERRED_PRESENCES.length - 1) * PRESENCE_MENU_ROW_GAP;
    }

    private float preferredPresenceMenuWidth(UIRenderer measure) {
        int widestText = 0;
        for (Presence presence : SELECTABLE_PREFERRED_PRESENCES) {
            widestText = Math.max(widestText, Math.max(
                    measure.measureTextWidth(preferredPresenceLabel(presence), false),
                    measure.measureTextWidth(preferredPresenceDescription(presence), false)));
        }
        float contentWidth = 12f + PRESENCE_PICKER_DOT + 8f + widestText + 8f + measure.measureTextWidth("\u2713", false) + 12f;
        return Math.max(PRESENCE_MENU_MIN_W, Math.min(PRESENCE_MENU_MAX_W, contentWidth));
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

    private static String preferredPresenceDescription(Presence presence) {
        return switch (presence) {
            case ONLINE -> Component.translatable("fascinatedutils.social.presence.description.online").getString();
            case AWAY -> Component.translatable("fascinatedutils.social.presence.description.away").getString();
            case DO_NOT_DISTURB -> Component.translatable("fascinatedutils.social.presence.description.do_not_disturb").getString();
            case INVISIBLE -> Component.translatable("fascinatedutils.social.presence.description.invisible").getString();
            case OFFLINE -> Component.translatable("fascinatedutils.social.presence.offline").getString();
        };
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

    private static int avatarBadgeColor(String name) {
        return BADGE_COLORS[Math.abs(name.hashCode()) % BADGE_COLORS.length];
    }

    private static String initials(String name) {
        if (name.isEmpty()) { return "?"; }
        return String.valueOf(Character.toUpperCase(name.charAt(0)));
    }

    private static String socialErrorMessage(Errors error) {
        if (error == null) {
            return Component.translatable("fascinatedutils.social.error.generic").getString();
        }

        String translationKey = "fascinatedutils.social.error." + error.getCode();
        String translated = Component.translatable(translationKey).getString();
        if (translated.equals(translationKey)) {
            return error.getDisplayText();
        }

        return translated;
    }

    private static String socialErrorMessage(AlumiteApiException exception) {
        Errors error = exception.getError();
        if (error != null) {
            return socialErrorMessage(error);
        }

        String displayText = exception.getDisplayText();
        if (displayText != null && !displayText.isBlank()) {
            return displayText;
        }

        return Component.translatable("fascinatedutils.social.error.generic").getString();
    }

    private FWidget buildChatHeader() {
        return new FWidget() {
            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
            }

            @Override
            protected void renderSelf(GuiRenderer graphics, UiFrameContext frame, float deltaSeconds) {
        float mouseX = frame.pointerX();
        float mouseY = frame.pointerY();
                float titleY = y() + 5f;
                float subtitleY = y() + 20f;
                if (activeTab == Tab.FRIENDS) {
                    Friend selectedFriend = selectedFriend();
                    if (selectedFriend == null) {
                        graphics.drawText(Component.translatable("fascinatedutils.social.tab_friends").getString(), x(), titleY,
                                FascinatedGuiTheme.INSTANCE.textPrimary(), true, false);
                        graphics.drawText(Component.translatable("fascinatedutils.social.dm.pick_friend").getString(), x(), subtitleY,
                                FascinatedGuiTheme.INSTANCE.textMuted(), false, false);
                        return;
                    }
                    graphics.drawText("Friend Profile", x(), titleY,
                            FascinatedGuiTheme.INSTANCE.textPrimary(), true, false);
                    graphics.drawText(selectedFriend.user().minecraftName(), x(), subtitleY,
                            FascinatedGuiTheme.INSTANCE.textMuted(), false, false);
                    return;
                }
                Channel channel = selectedChannelId == null ? null : Alumite.INSTANCE.channels().get(selectedChannelId);
                if (selectedChannelId != null && channel != null && !channel.detailLoaded()) {
                    loadDetail(selectedChannelId);
                }
                String title = channel == null
                        ? Component.translatable("fascinatedutils.social.dm.title").getString()
                        : channelTitle(channel);
                if ((title == null || title.isBlank()
                        || title.equals(Component.translatable("fascinatedutils.social.dm.title").getString()))
                        && selectedChannelDisplayName != null && !selectedChannelDisplayName.isBlank()) {
                    title = selectedChannelDisplayName;
                }
                String subTitle = channel == null
                        ? Component.translatable("fascinatedutils.social.dm.not_selected").getString()
                    : channelSubtitle(channel);
                float textLeft = x();
                DmChannel dmChannel = channel == null ? null : channel.asDmChannel();
                if (dmChannel != null && dmChannel.recipient() != null) {
                    String headerAvatarUuid = dmAvatarMinecraftUuid(dmChannel.recipient());
                    if (headerAvatarUuid != null && !headerAvatarUuid.isBlank()) {
                        float avatarX = x();
                        float avatarY = y() + (h() - CHAT_HEADER_AVATAR) / 2f;
                        Identifier avatarTexture = AvatarTextureCache.INSTANCE.get(headerAvatarUuid, () -> {});
                        String initial = title == null || title.isBlank()
                                ? "?"
                                : String.valueOf(Character.toUpperCase(title.charAt(0)));
                        if (avatarTexture != null) {
                            graphics.fillRoundedRect(avatarX, avatarY, CHAT_HEADER_AVATAR, CHAT_HEADER_AVATAR, 4f, 0xFF000000,
                                    RectCornerRoundMask.ALL);
                            graphics.drawTexture(avatarTexture, avatarX, avatarY, CHAT_HEADER_AVATAR, CHAT_HEADER_AVATAR, 0xFFFFFFFF);
                        } else {
                            graphics.fillRoundedRect(avatarX, avatarY, CHAT_HEADER_AVATAR, CHAT_HEADER_AVATAR, 4f, 0xFF3B445A,
                                    RectCornerRoundMask.ALL);
                            graphics.drawCenteredText(initial, avatarX + CHAT_HEADER_AVATAR / 2f,
                                    avatarY + (CHAT_HEADER_AVATAR - graphics.getFontCapHeight()) / 2f, 0xFFFFFFFF, false, true);
                        }
                        textLeft = avatarX + CHAT_HEADER_AVATAR + 8f;
                    }
                }
                graphics.drawText(title, textLeft, titleY, FascinatedGuiTheme.INSTANCE.textPrimary(), true, false);
                graphics.drawText(subTitle, textLeft, subtitleY, FascinatedGuiTheme.INSTANCE.textMuted(), false, false);
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
        String lastSeenLine = "";
        if (selectedFriendUser.presence() == null || selectedFriendUser.presence() == Presence.OFFLINE) {
            lastSeenLine = selectedFriendUser.lastSeen() != null
                    ? "Last seen " + messageTimeAgo(selectedFriendUser.lastSeen())
                    : "Last seen unknown";
        }
        return SocialFriendProfilePaneWidget.build(new SocialFriendProfilePaneWidget.Props(
                selectedFriendUser.minecraftName(),
                selectedFriendUser.minecraftUuid(),
                presenceLabelOnly(selectedFriendUser),
                lastSeenLine,
                () -> openDmForFriend(selectedFriend),
                () -> pendingRemoveFriend = selectedFriend
        ));
    }

    private Friend selectedFriend() {
        if (selectedFriendUserId == null) {
            return null;
        }
        return Alumite.INSTANCE.users().friends().stream()
                .filter(friend -> friend.user().id() == selectedFriendUserId)
                .findFirst()
                .orElse(null);
    }

    private FWidget buildChatMessages(float panelW) {
        Integer channelId = selectedChannelId;
        if (channelId == null) {
            return SocialNoChannelSelectedWidget.build();
        }
        Channel channel = Alumite.INSTANCE.channels().get(channelId);
        List<ChannelMessage> messages = channel == null ? null : channel.messagesOrNull();
        if (messages == null) {
            loadMessages(channelId);
            return buildEmptyState(Component.translatable("fascinatedutils.social.loading").getString());
        }
        if (channel != null && !channel.detailLoaded()) {
            loadDetail(channelId);
        }
        Map<Integer, String> namesById = channel == null ? new HashMap<>() : buildParticipantNames(channel);
        FColumnWidget body = new FColumnWidget(5f, Align.START);
        for (ChannelMessage message : messages) {
            body.addChild(buildMessageRow(message, namesById, panelW - 2f * PAD));
        }
        FScrollColumnWidget scroll = FTheme.components().createScrollColumn(body, 3f);
        Float savedY = messageScrollYRef.getValue();
        scroll.setScrollOffsetY(savedY == null ? MESSAGE_SCROLL_ANCHOR_BOTTOM : savedY);
        scroll.setScrollOffsetChangeListener(offset -> {
            messageScrollYRef.setValue(offset);
            if (offset <= 0.01f) {
                loadOlderMessages(channelId);
                markSelectedChannelRead();
            }
        });
        return scroll;
    }

    private FWidget buildMessageRow(ChannelMessage message, Map<Integer, String> namesById, float width) {
        boolean own = Objects.equals(Alumite.INSTANCE.activeUserId(), message.authorId());
        String authorName = own
            ? namesById.getOrDefault(message.authorId(), "You")
            : namesById.getOrDefault(message.authorId(), "#" + message.authorId());
        String topLine = authorName + " · " + messageTimeAgo(message.createdAt());
        return SocialMessageBubbleWidget.build(new SocialMessageBubbleWidget.Props(
                topLine,
                message.content(),
                own
        ), width);
    }

    private FWidget buildChatFooter() {
        FOutlinedTextInputWidget dmInput = props().dmMessageInput();
        dmInput.setPlaceholderSupplier(() -> {
            String label = selectedChannelDisplayName == null || selectedChannelDisplayName.isBlank()
                    ? Component.translatable("fascinatedutils.social.dm.title").getString()
                    : selectedChannelDisplayName;
            return "Message " + label;
        });
        return SocialChatComposerWidget.build(new SocialChatComposerWidget.Props(dmInput, this::sendSelectedMessage));
    }

    private void loadMessages(int channelId) {
        if (loadingMessagesChannelId != null && loadingMessagesChannelId == channelId) {
            return;
        }
        Channel channel = Alumite.INSTANCE.channels().get(channelId);
        if (channel == null) {
            return;
        }
        loadingMessagesChannelId = channelId;
        FascinatedUtils.SCHEDULED_POOL.execute(() -> {
            try {
                channel.fetchMessages(50);
            } catch (Exception exception) {
                Toast.show().message(Component.translatable("fascinatedutils.social.error.generic").getString()).error();
            }
            Minecraft.getInstance().execute(() -> loadingMessagesChannelId = null);
        });
    }

    private void loadOlderMessages(int channelId) {
        Channel channel = Alumite.INSTANCE.channels().get(channelId);
        if (channel == null) {
            return;
        }
        List<ChannelMessage> existingMessages = channel.messagesOrNull();
        if (existingMessages == null || existingMessages.isEmpty()) {
            return;
        }
        int oldestMessageId = existingMessages.get(0).id();
        FascinatedUtils.SCHEDULED_POOL.execute(() -> {
            try {
                List<ChannelMessage> olderMessages = channel.fetchMessagesPage(50, oldestMessageId, null);
                if (olderMessages == null || olderMessages.isEmpty()) {
                    return;
                }
                channel.mergeOlderMessagesPage(olderMessages);
            } catch (Exception ignored) {
            }
        });
    }

    private void loadDetail(int channelId) {
        if (loadingDetailChannelId != null && loadingDetailChannelId == channelId) {
            return;
        }
        Channel channel = Alumite.INSTANCE.channels().get(channelId);
        if (channel == null) {
            return;
        }
        if (channel.detailLoaded()) {
            return;
        }
        loadingDetailChannelId = channelId;
        FascinatedUtils.SCHEDULED_POOL.execute(() -> {
            try {
                channel.fetchDetail();
            } catch (Exception exception) {
                Toast.show().message(Component.translatable("fascinatedutils.social.error.generic").getString()).error();
            }
            Minecraft.getInstance().execute(() -> loadingDetailChannelId = null);
        });
    }

    private void anchorMessageScrollToBottom() {
        messageScrollYRef.setValue(MESSAGE_SCROLL_ANCHOR_BOTTOM);
    }

    private void selectChannel(int channelId, boolean switchToChat, String displayNameHint) {
        selectedChannelId = channelId;
        lastOpenedChannelId = channelId;
        if (displayNameHint != null && !displayNameHint.isBlank()) {
            selectedChannelDisplayName = displayNameHint;
        }
        anchorMessageScrollToBottom();
        if (switchToChat && activeTab != Tab.CHAT) {
            activeTab = Tab.CHAT;
            scrollYRef.setValue(0f);
        }
        loadDetail(channelId);
        loadMessages(channelId);
        markSelectedChannelRead();
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
                ChannelDetail channelDetail = Alumite.INSTANCE.channels().openDmAndCache(friend.user().id());
                Minecraft.getInstance().execute(() -> selectChannel(channelDetail.id(), true, friend.user().minecraftName()));
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
                        selectedChannelDisplayName = null;
                    }
                    if (Objects.equals(lastOpenedChannelId, channelId)) {
                        lastOpenedChannelId = null;
                    }
                });
            } catch (AlumiteApiException exception) {
                Minecraft.getInstance().execute(() -> Toast.show().message(socialErrorMessage(exception)).error());
            } catch (Exception exception) {
                Minecraft.getInstance().execute(() -> Toast.show().message(Component.translatable("fascinatedutils.social.error.generic").getString()).error());
            }
        });
    }

    private void markSelectedChannelRead() {
        Integer channelId = selectedChannelId;
        if (channelId == null) {
            return;
        }
        Channel channel = Alumite.INSTANCE.channels().get(channelId);
        if (channel == null) {
            return;
        }
        List<ChannelMessage> messages = channel.messagesOrNull();
        if (messages == null || messages.isEmpty()) {
            return;
        }
        int lastMessageId = messages.get(messages.size() - 1).id();
        FascinatedUtils.SCHEDULED_POOL.execute(() -> {
            try {
                channel.markRead(lastMessageId);
            } catch (Exception ignored) {
            }
        });
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
        if (dmChannel != null) {
            if (dmChannel.recipient() == null) {
                return Component.translatable("fascinatedutils.social.dm.title").getString();
            }
            User recipient = displayUser(dmChannel.recipient());
            if (recipient != null
                    && recipient.minecraftName() != null
                    && !recipient.minecraftName().isBlank()) {
                return recipient.minecraftName();
            }
        }
        return Component.translatable("fascinatedutils.social.dm.title").getString();
    }

    private String channelSubtitle(Channel channel) {
        GroupChannel groupChannel = channel == null ? null : channel.asGroupChannel();
        if (groupChannel != null) {
            return Component.translatable("fascinatedutils.social.dm.member_count", groupChannel.members().size()).getString();
        }
        return Component.translatable("fascinatedutils.social.dm.title").getString();
    }

    private Map<Integer, String> buildParticipantNames(Channel channel) {
        Map<Integer, String> namesById = new HashMap<>();
        if (channel == null) {
            return namesById;
        }
        DmChannel dmChannel = channel.asDmChannel();
        if (dmChannel != null) {
            User recipient = displayUser(dmChannel.recipient());
            if (recipient != null) {
                namesById.put(recipient.id(), recipient.minecraftName());
            }
            return namesById;
        }
        GroupChannel groupChannel = channel.asGroupChannel();
        if (groupChannel != null) {
            for (GroupMember member : groupChannel.members()) {
                User user = displayUser(member.user());
                if (user != null) {
                    namesById.put(user.id(), user.minecraftName());
                }
            }
        }
        return namesById;
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

    private static String messageTimeAgo(String time) {
        if (time == null || time.isBlank()) {
            return "";
        }
        try {
            return TimeUtils.timeAgo(Instant.parse(time).toEpochMilli(), 1);
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String messageTimeAgo(java.util.Date time) {
        if (time == null) {
            return "";
        }
        try {
            return TimeUtils.timeAgo(time.getTime(), 1);
        } catch (Exception ignored) {
            return "";
        }
    }

    private FWidget buildFooter() {
        FOutlinedTextInputWidget friendInput = props().addFriendInput();
        FButtonWidget addBtn = new FButtonWidget(() -> {
            String username = friendInput.value().trim();
            if (username.isEmpty()) { return; }
            friendInput.setValue("");
            FascinatedUtils.SCHEDULED_POOL.execute(() -> {
                try {
                    Alumite.INSTANCE.sendFriendRequest(username);
                    Toast.show().message("Friend request sent!").success();
                } catch (AlumiteApiException exception) {
                    Toast.show().message(socialErrorMessage(exception)).error();
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

}