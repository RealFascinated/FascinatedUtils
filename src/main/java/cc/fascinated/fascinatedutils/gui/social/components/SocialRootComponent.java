package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.FascinatedUtils;
import cc.fascinated.fascinatedutils.api.AlumiteApi;
import cc.fascinated.fascinatedutils.api.AlumiteApiException;
import cc.fascinated.fascinatedutils.api.Errors;
import cc.fascinated.fascinatedutils.api.dto.Presence;
import cc.fascinated.fascinatedutils.api.dto.friend.FriendEntryDto;
import cc.fascinated.fascinatedutils.api.dto.friend.PendingFriendRequestDto;
import cc.fascinated.fascinatedutils.common.TimeUtils;
import cc.fascinated.fascinatedutils.event.impl.social.PresenceUpdateEvent;
import cc.fascinated.fascinatedutils.gui.AvatarTextureCache;
import cc.fascinated.fascinatedutils.gui.core.Align;
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
import cc.fascinated.fascinatedutils.systems.social.SocialRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SocialRootComponent extends UiComponent<SocialRootComponent.Props> {
    private static final float PANEL_WIDTH = 250f;
    private static final float PAD = 10f;
    private static final float ROW_H = 44f;
    private static final float BTN_H = 20f;
    private static final float BTN_W = 20f;
    private static final float ADD_BTN_W = 36f;
    private static final float TAB_H = 26f;
    private static final float PRESENCE_PICKER_H = 24f;
    private static final float PRESENCE_PICKER_DOT = 8f;
    private static final float PRESENCE_MENU_W = 156f;
    private static final float PRESENCE_MENU_PAD = 6f;
    private static final float PRESENCE_MENU_ROW_H = 30f;
    private static final float PRESENCE_MENU_ROW_GAP = 4f;
    private static final float AVATAR_SIZE = 32f;
    private static final float STATUS_DOT = 6f;
    private static final Presence[] SELECTABLE_PREFERRED_PRESENCES = {
            Presence.ONLINE,
            Presence.AWAY,
            Presence.INVISIBLE
    };
    private static final int[] BADGE_COLORS = {
            0xFF6B5B95, 0xFF88B04B, 0xFF955251, 0xFF009B77,
            0xFF45B8AC, 0xFF5B5EA6, 0xFFB565A7, 0xFFDD4132
    };

    private enum Tab { FRIENDS, REQUESTS }

    private final Ref<Float> scrollYRef = Ref.of(0f);

    private Tab activeTab = Tab.FRIENDS;
    private FriendEntryDto pendingRemoveFriend;
    private PendingFriendRequestDto pendingCancelRequest;
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
    private float preferredPresenceMenuX;
    private float preferredPresenceMenuY;
    private float preferredPresenceMenuHeight;

    /**
     * Top-level declarative subtree for {@link cc.fascinated.fascinatedutils.gui.screens.SocialScreen}.
     */
    public static UiView view(SocialRootComponent.Props props) {
        return Ui.component(SocialRootComponent.class, SocialRootComponent::new, props);
    }

    @Override
    public UiView render() {
        Props currentProps = props();
        currentProps.presenceMenuOpenSink().accept(preferredPresenceMenuOpen);
        currentProps.presenceHitTestSink().accept(new PresenceHitTest(
                preferredPresenceMenuOpen,
                preferredPresenceButtonX,
                preferredPresenceButtonY,
                preferredPresenceButtonW,
                preferredPresenceButtonH,
                preferredPresenceMenuX,
                preferredPresenceMenuY,
                PRESENCE_MENU_W,
                preferredPresenceMenuHeight));
        float viewportWidth = currentProps.viewportWidth();
        float viewportHeight = currentProps.viewportHeight();
        if (viewportWidth <= 0f || viewportHeight <= 0f) {
            throw new IllegalStateException("Declarative mount host delivered non-positive viewport");
        }
        List<UiSlot> viewportLayers = new ArrayList<>();
        viewportLayers.add(UiSlot.keyed("social.main", Ui.custom(previous -> buildSocialMainDock())));
        if (pendingRemoveFriend != null) {
            FriendEntryDto friend = pendingRemoveFriend;
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
                                        AlumiteApi.INSTANCE.removeFriend(friend.user().id());
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
            PendingFriendRequestDto cancellableRequest = pendingCancelRequest;
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
                                        AlumiteApi.INSTANCE.cancelFriendRequest(cancellableRequest.requestId());
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
                        Runnable onCloseScreen,
                        Consumer<Boolean> presenceMenuOpenSink,
                        Consumer<PresenceHitTest> presenceHitTestSink) {
    }

    /**
     * Latest hit-test region for the preferred-presence picker + its dropdown, published every
     * reconcile pass for the owning screen to optionally pre-filter pointer presses.
     */
    public record PresenceHitTest(boolean menuOpen,
                                  float buttonLeft,
                                  float buttonTop,
                                  float buttonWidth,
                                  float buttonHeight,
                                  float menuLeft,
                                  float menuTop,
                                  float menuWidth,
                                  float menuHeight) {
        public boolean contains(float pointerX, float pointerY) {
            boolean withinButton = pointerX >= buttonLeft && pointerX < buttonLeft + buttonWidth
                    && pointerY >= buttonTop && pointerY < buttonTop + buttonHeight;
            boolean withinMenu = menuOpen
                    && pointerX >= menuLeft && pointerX < menuLeft + menuWidth
                    && pointerY >= menuTop && pointerY < menuTop + menuHeight;
            return withinButton || withinMenu;
        }
    }

    private FWidget buildSocialMainDock() {
        if (pendingRemoveFriend != null || pendingCancelRequest != null) {
            preferredPresenceMenuOpen = false;
        }

        FAbsoluteStackWidget stack = new FAbsoluteStackWidget();
        stack.addChild(new FWidget() {
            private FWidget innerDock;

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
                float panelLogicalWidth = Math.min(PANEL_WIDTH, layoutWidth);
                float panelLeft = layoutX + layoutWidth - panelLogicalWidth;
                innerDock = buildPanelContent(panelLogicalWidth);
                clearChildren();
                addChild(innerDock);
                innerDock.layout(measure, panelLeft, layoutY, panelLogicalWidth, layoutHeight);
            }
        });
        return stack;
    }

    private FWidget buildPanelContent(float panelW) {
        FRectWidget panelBg = new FRectWidget();
        panelBg.setFillColorArgb(0xEE1A1E24);
        panelBg.setBorder(UITheme.COLOR_BORDER, 1f);

        FWidget header = buildHeader();
        FWidget tabs = buildTabs();
        FScrollColumnWidget list = buildFriendList(panelW);
        FWidget footer = buildFooter();

        FWidget content = new FWidget() {
            {
                addChild(header);
                addChild(tabs);
                addChild(footer);
                addChild(list);
            }

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
                float curY = ly + PAD;

                float headerH = 56f;
                header.layout(measure, lx + PAD, curY, lw - 2f * PAD, headerH);
                curY += headerH + 6f;

                tabs.layout(measure, lx, curY, lw, TAB_H);
                curY += TAB_H + 6f;

                float footerH = activeTab == Tab.FRIENDS ? 32f : 0f;
                footer.setVisible(activeTab == Tab.FRIENDS);
                if (activeTab == Tab.FRIENDS) {
                    footer.layout(measure, lx + PAD, curY, lw - 2f * PAD, footerH);
                    curY += footerH + 6f;
                }
                else {
                    footer.layout(measure, lx + PAD, curY, lw - 2f * PAD, 0f);
                }

                float listH = Math.max(0f, ly + lh - PAD - curY);
                list.layout(measure, lx + PAD, curY, lw - 2f * PAD, listH);
            }
        };

        return new FWidget() {
            {
                addChild(panelBg);
                addChild(content);
            }

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
                panelX = lx;
                panelY = ly;
                SocialRootComponent.this.panelW = lw;
                panelH = lh;
                panelBg.layout(measure, lx, ly, lw, lh);
                content.layout(measure, lx, ly, lw, lh);
            }
        };
    }

    private FWidget buildHeader() {
        FLabelWidget titleLabel = new FLabelWidget();
        titleLabel.setText(Component.translatable("fascinatedutils.social.title").getString());
        titleLabel.setColorArgb(FascinatedGuiTheme.INSTANCE.textPrimary());
        titleLabel.setTextBold(true);

        FLabelWidget subtitleLabel = new FLabelWidget();
        subtitleLabel.setText(Component.translatable("fascinatedutils.social.subtitle").getString());
        subtitleLabel.setColorArgb(FascinatedGuiTheme.INSTANCE.textMuted());

        FButtonWidget closeBtn = new FButtonWidget(props().onCloseScreen(),
                () -> "✕", 20f, 1, 1f, 4f, 1f, 4f) {
            @Override
            protected int resolveButtonFillColorArgb(boolean hovered) {
                return hovered ? 0xFF2A2F3E : 0xFF1E2230;
            }

            @Override
            protected int resolveButtonBorderColorArgb(boolean hovered) {
                return hovered ? 0xFF6C7098 : 0xFF454A60;
            }

            @Override
            protected int resolveButtonLabelColorArgb(boolean hovered) {
                return hovered ? 0xFFFF7070 : FascinatedGuiTheme.INSTANCE.textMuted();
            }
        };

        FWidget preferredPresencePicker = buildPreferredPresencePicker();

        return new FWidget() {
            {
                addChild(titleLabel);
                addChild(subtitleLabel);
                addChild(preferredPresencePicker);
                addChild(closeBtn);
            }

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
                float lineH = measure.getFontCapHeight();
                float totalTextH = lineH * 2 + 4f;
                float row1H = Math.max(totalTextH, BTN_H);
                // Row 1: title/subtitle on left, close button on right
                float closeBtnX = lx + lw - 20f;
                float textStartY = ly + (row1H - totalTextH) / 2f;
                titleLabel.layout(measure, lx, textStartY, lw - 26f, lineH);
                subtitleLabel.layout(measure, lx, textStartY + lineH + 4f, lw - 26f, lineH);
                closeBtn.layout(measure, closeBtnX, ly + (row1H - BTN_H) / 2f, 20f, BTN_H);
                // Row 2: full-width presence picker
                float row2Y = ly + row1H + 8f;
                preferredPresencePicker.layout(measure, lx, row2Y, lw, PRESENCE_PICKER_H);
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
            protected void renderSelf(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
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
            public boolean wantsPointer() {
                return true;
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
                float menuHeight = preferredPresenceMenuHeight();
                float menuX = preferredPresenceButtonX + preferredPresenceButtonW - PRESENCE_MENU_W;
                float minMenuX = panelX + PAD;
                float maxMenuX = panelX + panelW - PAD - PRESENCE_MENU_W;
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
                preferredPresenceMenuX = menuX;
                preferredPresenceMenuY = menuY;
                preferredPresenceMenuHeight = menuHeight;
                menu.layout(measure, menuX, menuY, PRESENCE_MENU_W, menuHeight);
            }

            @Override
            public void render(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
                graphics.absolutePost(() -> menu.render(graphics, mouseX, mouseY, deltaSeconds));
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
            protected void renderSelf(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
                graphics.fillRoundedRectFrame(x(), y(), w(), h(), UITheme.CORNER_RADIUS_MD, 0xFF454A60, 0xFF171B24,
                        1f, 1f, RectCornerRoundMask.ALL);

                float rowY = y() + PRESENCE_MENU_PAD;
                Presence currentPresence = displayedPreferredPresence();
                for (Presence presence : SELECTABLE_PREFERRED_PRESENCES) {
                    boolean hovered = mouseX >= x() + 4f && mouseX < x() + w() - 4f
                            && mouseY >= rowY && mouseY < rowY + PRESENCE_MENU_ROW_H;
                    boolean selected = currentPresence == presence;
                    int rowColor = selected ? 0x334960C8 : hovered ? 0x22FFFFFF : 0x00000000;
                    if (rowColor != 0) {
                        graphics.fillRoundedRect(x() + 4f, rowY, w() - 8f, PRESENCE_MENU_ROW_H, UITheme.CORNER_RADIUS_SM,
                                rowColor, RectCornerRoundMask.ALL);
                    }

                    float dotX = x() + 12f;
                    float dotY = rowY + 7f;
                    graphics.fillRoundedRect(dotX, dotY, PRESENCE_PICKER_DOT, PRESENCE_PICKER_DOT,
                            PRESENCE_PICKER_DOT / 2f, presence.color(), RectCornerRoundMask.ALL);

                    float labelX = dotX + PRESENCE_PICKER_DOT + 8f;
                    graphics.drawText(preferredPresenceLabel(presence), labelX, rowY + 3f,
                            FascinatedGuiTheme.INSTANCE.textPrimary(), false, false);
                    graphics.drawText(preferredPresenceDescription(presence), labelX, rowY + 15f,
                            FascinatedGuiTheme.INSTANCE.textMuted(), false, false);

                    if (selected) {
                        graphics.drawText("\u2713", x() + w() - 14f, rowY + 6f,
                                0xFF9DB4FF, false, false);
                    }
                    rowY += PRESENCE_MENU_ROW_H + PRESENCE_MENU_ROW_GAP;
                }
            }

            @Override
            public boolean wantsPointer() {
                return true;
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
        final int friendCount = SocialRegistry.INSTANCE.getFriends().size();
        final String friendsLabel = Component.translatable("fascinatedutils.social.tab_friends", friendCount).getString();
        final String requestsLabel = Component.translatable("fascinatedutils.social.tab_requests").getString();

        return new FWidget() {
            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
            }

            @Override
            protected void renderSelf(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
                float halfW = w() / 2f;
                boolean friendsHovered = mouseX >= x() && mouseX < x() + halfW && mouseY >= y() && mouseY < y() + h();
                boolean reqHovered = mouseX >= x() + halfW && mouseX < x() + w() && mouseY >= y() && mouseY < y() + h();

                int friendsBg = activeTab == Tab.FRIENDS ? 0x447C5CBF : (friendsHovered ? 0x22FFFFFF : 0x00000000);
                int reqBg = activeTab == Tab.REQUESTS ? 0x447C5CBF : (reqHovered ? 0x22FFFFFF : 0x00000000);

                graphics.drawRect(x(), y(), halfW, h(), friendsBg);
                graphics.drawRect(x() + halfW, y(), halfW, h(), reqBg);

                float underlineH = 2f;
                if (activeTab == Tab.FRIENDS) {
                    graphics.fillRoundedRect(x() + 8f, y() + h() - underlineH, halfW - 16f, underlineH,
                            underlineH / 2f, UITheme.COLOR_ACCENT, RectCornerRoundMask.ALL);
                } else {
                    graphics.fillRoundedRect(x() + halfW + 8f, y() + h() - underlineH, halfW - 16f, underlineH,
                            underlineH / 2f, UITheme.COLOR_ACCENT, RectCornerRoundMask.ALL);
                }

                int friendsTextColor = activeTab == Tab.FRIENDS ? FascinatedGuiTheme.INSTANCE.textPrimary() : FascinatedGuiTheme.INSTANCE.textMuted();
                int reqTextColor = activeTab == Tab.REQUESTS ? FascinatedGuiTheme.INSTANCE.textPrimary() : FascinatedGuiTheme.INSTANCE.textMuted();

                graphics.drawCenteredText(friendsLabel, x() + halfW / 2f, y() + (h() - graphics.getFontCapHeight()) / 2f, friendsTextColor, false, false);
                graphics.drawCenteredText(requestsLabel, x() + halfW + halfW / 2f, y() + (h() - graphics.getFontCapHeight()) / 2f, reqTextColor, false, false);

                int incomingCount = SocialRegistry.INSTANCE.getIncomingFriendRequests().size();
                if (incomingCount > 0) {
                    String badgeText = incomingCount > 9 ? "9+" : String.valueOf(incomingCount);
                    float badgeR = 5f;
                    float badgeCx = x() + w() - badgeR - 3f;
                    float badgeCy = y() + badgeR + 2f;
                    graphics.fillRoundedRect(badgeCx - badgeR, badgeCy - badgeR, badgeR * 2f, badgeR * 2f,
                            badgeR, 0xFFCC2222, RectCornerRoundMask.ALL);
                    graphics.drawCenteredText(badgeText, badgeCx,
                            badgeCy - graphics.getFontCapHeight() / 2f, 0xFFFFFFFF, false, true);
                }
            }

            @Override
            public boolean wantsPointer() { return true; }

            @Override
            public UiPointerCursor pointerCursor(float pointerX, float pointerY) { return UiPointerCursor.HAND; }

            @Override
            public boolean mouseDown(float pointerX, float pointerY, int button) {
                if (button != 0) { return false; }
                float halfW = w() / 2f;
                Tab selected = pointerX < x() + halfW ? Tab.FRIENDS : Tab.REQUESTS;
                if (selected != activeTab) {
                    activeTab = selected;
                    scrollYRef.setValue(0f);
                }
                return true;
            }
        };
    }

    private FScrollColumnWidget buildFriendList(float panelW) {
        float innerW = panelW - 2f * PAD;
        FColumnWidget body = new FColumnWidget(4f, Align.START);

        if (activeTab == Tab.FRIENDS) {
            List<FriendEntryDto> friends = SocialRegistry.INSTANCE.getFriends();
            if (friends.isEmpty()) {
                body.addChild(buildEmptyState(Component.translatable("fascinatedutils.social.no_friends").getString()));
            } else {
                for (FriendEntryDto friend : friends) {
                    body.addChild(buildFriendRow(friend, innerW));
                }
            }
        } else {
            List<PendingFriendRequestDto> incoming = SocialRegistry.INSTANCE.getIncomingFriendRequests();
            List<PendingFriendRequestDto> outgoing = SocialRegistry.INSTANCE.getOutgoingFriendRequests();
            if (incoming.isEmpty() && outgoing.isEmpty()) {
                body.addChild(buildEmptyState(Component.translatable("fascinatedutils.social.no_requests").getString()));
            } else {
                if (!incoming.isEmpty()) {
                    body.addChild(buildSectionLabel(Component.translatable("fascinatedutils.social.requests_incoming").getString()));
                    for (PendingFriendRequestDto request : incoming) {
                        body.addChild(buildRequestRow(request, innerW));
                    }
                }
                if (!outgoing.isEmpty()) {
                    body.addChild(buildSectionLabel(Component.translatable("fascinatedutils.social.requests_outgoing").getString()));
                    for (PendingFriendRequestDto request : outgoing) {
                        body.addChild(buildOutgoingRequestRow(request, innerW));
                    }
                }
            }
        }

        FScrollColumnWidget scroll = FTheme.components().createScrollColumn(body, 3f);
        Float savedY = scrollYRef.getValue();
        scroll.setScrollOffsetY(savedY == null ? 0f : savedY);
        scroll.setScrollOffsetChangeListener(scrollYRef::setValue);
        return scroll;
    }

    private FWidget buildFriendRow(FriendEntryDto friend, float innerW) {
        FLabelWidget nameLabel = new FLabelWidget();
        nameLabel.setText(friend.user().minecraftName());
        nameLabel.setColorArgb(FascinatedGuiTheme.INSTANCE.textPrimary());
        nameLabel.setTextBold(true);
        nameLabel.setOverflow(TextOverflow.ELLIPSIS);
        nameLabel.setAlignY(Align.CENTER);

        return new FWidget() {
            {
                addChild(nameLabel);
            }

            @Override
            public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
                return ROW_H;
            }

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, innerW, lh);
                int capH = measure.getFontCapHeight();
                float textBlockH = capH * 2f + 3f;
                float textStartY = ly + (lh - textBlockH) / 2f;
                float nameX = lx + 4f + AVATAR_SIZE + 6f;
                float nameMaxX = lx + innerW - BTN_W - 8f;
                nameLabel.layout(measure, nameX, textStartY, nameMaxX - nameX, capH);
            }

            @Override
            protected void renderSelf(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
                boolean rowHovered = containsPoint(mouseX, mouseY);
                graphics.fillRoundedRect(x(), y(), w(), h(), UITheme.CORNER_RADIUS_SM,
                        rowHovered ? 0x22FFFFFF : UITheme.COLOR_BACKGROUND, RectCornerRoundMask.ALL);

                float avatarX = x() + 4f;
                float avatarY = y() + (h() - AVATAR_SIZE) / 2f;
                Identifier avatarTexture = AvatarTextureCache.INSTANCE.get(
                        friend.user().minecraftUuid(), () -> {});
                if (avatarTexture != null) {
                    graphics.fillRoundedRect(avatarX, avatarY, AVATAR_SIZE, AVATAR_SIZE, 4f, 0xFF000000, RectCornerRoundMask.ALL);
                    graphics.drawTexture(avatarTexture, avatarX, avatarY, AVATAR_SIZE, AVATAR_SIZE, 0xFFFFFFFF);
                } else {
                    int badgeColor = avatarBadgeColor(friend.user().minecraftName());
                    graphics.fillRoundedRect(avatarX, avatarY, AVATAR_SIZE, AVATAR_SIZE, 4f, badgeColor, RectCornerRoundMask.ALL);
                    graphics.drawCenteredText(initials(friend.user().minecraftName()),
                            avatarX + AVATAR_SIZE / 2f,
                            avatarY + (AVATAR_SIZE - graphics.getFontCapHeight()) / 2f,
                            0xFFFFFFFF, false, true);
                }

                PresenceUpdateEvent presence = SocialRegistry.INSTANCE.getPresenceStatuses().get(friend.user().id());
                int dotColor = (presence == null ? Presence.OFFLINE : presence.status()).color();
                float dotX = avatarX + AVATAR_SIZE - STATUS_DOT - 1f;
                float dotY = avatarY + AVATAR_SIZE - STATUS_DOT - 1f;
                graphics.fillRoundedRect(dotX - 2f, dotY - 2f, STATUS_DOT + 4f, STATUS_DOT + 4f,
                        (STATUS_DOT + 4f) / 2f, 0xFF1A1E24, RectCornerRoundMask.ALL);
                graphics.fillRoundedRect(dotX, dotY, STATUS_DOT, STATUS_DOT,
                        STATUS_DOT / 2f, dotColor, RectCornerRoundMask.ALL);

                int capH = graphics.getFontCapHeight();
                float textBlockH = capH * 2f + 3f;
                float textStartY = y() + (h() - textBlockH) / 2f;
                float nameX = x() + 4f + AVATAR_SIZE + 6f;
                int statusTextColor = (presence == null ? Presence.OFFLINE : presence.status()).color();
                graphics.drawText(presenceStatusLine(presence), nameX, textStartY + capH + 3f,
                        statusTextColor, false, false);

                if (rowHovered) {
                    float removeBtnX = x() + w() - BTN_W - 4f;
                    float removeBtnY = y() + (h() - BTN_H) / 2f;
                    boolean btnHovered = mouseX >= removeBtnX && mouseX < removeBtnX + BTN_W
                            && mouseY >= removeBtnY && mouseY < removeBtnY + BTN_H;
                    graphics.fillRoundedRect(removeBtnX, removeBtnY, BTN_W, BTN_H, 4f,
                            btnHovered ? 0xAA5C1F1F : 0x22FFFFFF, RectCornerRoundMask.ALL);
                    int removeBtnTextColor = btnHovered ? 0xFFFF5555 : FascinatedGuiTheme.INSTANCE.textMuted();
                    graphics.drawCenteredText("\u2715", removeBtnX + BTN_W / 2f,
                            removeBtnY + (BTN_H - graphics.getFontCapHeight()) / 2f,
                            removeBtnTextColor, false, false);
                }
            }

            @Override
            public boolean wantsPointer() { return true; }

            @Override
            public UiPointerCursor pointerCursor(float pointerX, float pointerY) { return UiPointerCursor.HAND; }

            @Override
            public boolean mouseDown(float pointerX, float pointerY, int button) {
                if (button != 0) { return false; }
                float removeBtnX = x() + w() - BTN_W - 4f;
                float removeBtnY = y() + (h() - BTN_H) / 2f;
                if (pointerX >= removeBtnX && pointerX < removeBtnX + BTN_W
                        && pointerY >= removeBtnY && pointerY < removeBtnY + BTN_H) {
                    pendingRemoveFriend = friend;
                    return true;
                }
                return false;
            }
        };
    }

    private FWidget buildRequestRow(PendingFriendRequestDto request, float innerW) {
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
            protected void renderSelf(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
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
            public boolean wantsPointer() { return true; }

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
                            boolean accepted = AlumiteApi.INSTANCE.acceptFriendRequest(request.requestId());
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
                            AlumiteApi.INSTANCE.declineFriendRequest(request.requestId());
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

    private FWidget buildOutgoingRequestRow(PendingFriendRequestDto request, float innerW) {
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
            protected void renderSelf(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
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
            public boolean wantsPointer() { return true; }

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
            protected void renderSelf(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
                float textY = y() + (h() - graphics.getFontCapHeight()) / 2f;
                graphics.drawText(text, x(), textY, FascinatedGuiTheme.INSTANCE.textMuted(), false, false);
                graphics.drawRect(x(), y() + h() - 1f, w(), 1f, 0x22FFFFFF);
            }
        };
    }

    private static String presenceStatusLine(PresenceUpdateEvent presence) {
        if (presence == null || presence.status() == Presence.OFFLINE) {
            if (presence != null && presence.lastSeen() != null) {
                long timeAgo = Instant.parse(presence.lastSeen()).toEpochMilli();
                return Component.translatable("fascinatedutils.social.presence.offline").getString()
                        + " · " + TimeUtils.timeAgo(timeAgo, timeAgo < 61_000 ? 1 : 2);
            }
            return Component.translatable("fascinatedutils.social.presence.offline").getString();
        }
        return preferredPresenceLabel(presence.status());
    }

    private Presence displayedPreferredPresence() {
        return AlumiteApi.INSTANCE.currentPreferredPresence();
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
                AlumiteApi.INSTANCE.updatePreferredPresence(presence);
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

    private static String preferredPresenceLabel(Presence presence) {
        return switch (presence) {
            case ONLINE -> Component.translatable("fascinatedutils.social.presence.online").getString();
            case AWAY -> Component.translatable("fascinatedutils.social.presence.away").getString();
            case INVISIBLE -> Component.translatable("fascinatedutils.social.presence.invisible").getString();
            case OFFLINE -> Component.translatable("fascinatedutils.social.presence.offline").getString();
        };
    }

    private static String preferredPresenceDescription(Presence presence) {
        return switch (presence) {
            case ONLINE -> Component.translatable("fascinatedutils.social.presence.description.online").getString();
            case AWAY -> Component.translatable("fascinatedutils.social.presence.description.away").getString();
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
            protected void renderSelf(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
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

    private FWidget buildFooter() {
        FOutlinedTextInputWidget friendInput = props().addFriendInput();
        FButtonWidget addBtn = new FButtonWidget(() -> {
            String username = friendInput.value().trim();
            if (username.isEmpty()) { return; }
            friendInput.setValue("");
            FascinatedUtils.SCHEDULED_POOL.execute(() -> {
                try {
                    PendingFriendRequestDto dto = AlumiteApi.INSTANCE.sendFriendRequest(username);
                    SocialRegistry.INSTANCE.addOutgoingFriendRequest(dto);
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