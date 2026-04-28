package cc.fascinated.fascinatedutils.gui.screens;

import cc.fascinated.fascinatedutils.FascinatedUtils;
import cc.fascinated.fascinatedutils.api.AlumiteApi;
import cc.fascinated.fascinatedutils.api.dto.friend.FriendEntryDto;
import cc.fascinated.fascinatedutils.api.dto.friend.PendingFriendRequestDto;
import cc.fascinated.fascinatedutils.systems.social.SocialRegistry;
import cc.fascinated.fascinatedutils.event.impl.social.PresenceUpdateEvent;
import cc.fascinated.fascinatedutils.gui.AvatarTextureCache;
import cc.fascinated.fascinatedutils.gui.UIScale;
import net.minecraft.resources.Identifier;
import cc.fascinated.fascinatedutils.gui.core.*;
import cc.fascinated.fascinatedutils.gui.input.UiCursorController;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import cc.fascinated.fascinatedutils.common.DateUtils;
import cc.fascinated.fascinatedutils.common.TimeUtils;
import cc.fascinated.fascinatedutils.gui.toast.Toast;

public class SocialScreen extends WidgetScreen {
    private static final float PANEL_WIDTH = 250f;
    private static final float PAD = 10f;
    private static final float ROW_H = 44f;
    private static final float BTN_H = 20f;
    private static final float BTN_W = 20f;
    private static final float ADD_BTN_W = 36f;
    private static final float TAB_H = 26f;
    private static final int FOCUS_ADD_FRIEND = 7110;
    private static final float AVATAR_SIZE = 28f;
    private static final float STATUS_DOT = 6f;
    private static final int[] BADGE_COLORS = {
            0xFF6B5B95, 0xFF88B04B, 0xFF955251, 0xFF009B77,
            0xFF45B8AC, 0xFF5B5EA6, 0xFFB565A7, 0xFFDD4132
    };

    private enum Tab { FRIENDS, REQUESTS }

    private final FWidgetHost host = new FWidgetHost();
    private final FOutlinedTextInputWidget addFriendInput;
    private final Ref<Float> scrollYRef = Ref.of(0f);

    private Tab activeTab = Tab.FRIENDS;
    private float scrollAccum;

    public SocialScreen() {
        super(Component.translatable("fascinatedutils.social.title"));
        addFriendInput = new FOutlinedTextInputWidget(FOCUS_ADD_FRIEND, 32, 22f,
                () -> Component.translatable("fascinatedutils.social.add_friend_placeholder").getString());
        addFriendInput.setExternalFocusIdSupplier(GuiFocusState::getFocusedId);
    }

    @Override
    protected void init() {
        super.init();
        rebuildHost();
    }

    private void rebuildHost() {
        host.setRoot(buildPanel());
    }

    private FWidget buildPanel() {
        return new FWidget() {
            private FWidget inner;

            @Override
            public boolean fillsHorizontalInRow() { return true; }

            @Override
            public boolean fillsVerticalInColumn() { return true; }

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
                float panelW = Math.min(PANEL_WIDTH, lw);
                float panelX = lx + lw - panelW;
                inner = buildPanelContent(panelW);
                clearChildren();
                addChild(inner);
                inner.layout(measure, panelX, ly, panelW, lh);
            }
        };
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
                addChild(list);
                addChild(footer);
            }

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
                float curY = ly + PAD;

                float headerH = 40f;
                header.layout(measure, lx + PAD, curY, lw - 2f * PAD, headerH);
                curY += headerH + 6f;

                tabs.layout(measure, lx, curY, lw, TAB_H);
                curY += TAB_H + 6f;

                float footerH = activeTab == Tab.FRIENDS ? 32f : 0f;
                float listH = Math.max(0f, lh - (curY - ly) - footerH - (footerH > 0 ? PAD : 0f));
                list.layout(measure, lx + PAD, curY, lw - 2f * PAD, listH);
                curY += listH + 4f;

                footer.setVisible(activeTab == Tab.FRIENDS);
                if (activeTab == Tab.FRIENDS) {
                    footer.layout(measure, lx + PAD, curY, lw - 2f * PAD, footerH);
                } else {
                    footer.layout(measure, lx + PAD, curY, lw - 2f * PAD, 0f);
                }
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

        FButtonWidget closeBtn = new FButtonWidget(() -> Minecraft.getInstance().setScreen(null),
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

        return new FWidget() {
            {
                addChild(titleLabel);
                addChild(subtitleLabel);
                addChild(closeBtn);
            }

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
                float lineH = measure.getFontCapHeight();
                float totalTextH = lineH * 2 + 4f;
                float textStartY = ly + (lh - totalTextH) / 2f;
                float textW = lw - 22f;
                titleLabel.layout(measure, lx, textStartY, textW, lineH);
                subtitleLabel.layout(measure, lx, textStartY + lineH + 4f, textW, lineH);
                closeBtn.layout(measure, lx + lw - 20f, ly + (lh - BTN_H) / 2f, 20f, BTN_H);
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
                    graphics.drawRect(x(), y() + h() - underlineH, halfW, underlineH, UITheme.COLOR_ACCENT);
                } else {
                    graphics.drawRect(x() + halfW, y() + h() - underlineH, halfW, underlineH, UITheme.COLOR_ACCENT);
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
                    rebuildHost();
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
        scroll.setFillVerticalInColumn(true);
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
                graphics.fillRoundedRect(x(), y(), w(), h(), 5f,
                        rowHovered ? 0x22FFFFFF : UITheme.COLOR_BACKGROUND, RectCornerRoundMask.ALL);

                float avatarX = x() + 4f;
                float avatarY = y() + (h() - AVATAR_SIZE) / 2f;
                Identifier avatarTexture = AvatarTextureCache.INSTANCE.get(
                        friend.user().minecraftUuid(), SocialScreen.this::rebuildHost);
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
                boolean online = presence != null && "online".equals(presence.status());
                int dotColor = online ? 0xFF44CC44 : 0xFF555555;
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
                graphics.drawText(presenceStatusLine(presence), nameX, textStartY + capH + 3f,
                        FascinatedGuiTheme.INSTANCE.textMuted(), false, false);

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
                if (containsPoint(pointerX, pointerY)
                        && pointerX >= removeBtnX && pointerX < removeBtnX + BTN_W
                        && pointerY >= removeBtnY && pointerY < removeBtnY + BTN_H) {
                    FascinatedUtils.SCHEDULED_POOL.execute(() ->
                            AlumiteApi.INSTANCE.removeFriend(friend.user().id()));
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
                graphics.fillRoundedRect(x(), y(), w(), h(), 5f,
                        rowHovered ? 0x22FFFFFF : UITheme.COLOR_BACKGROUND, RectCornerRoundMask.ALL);

                float avatarX = x() + 4f;
                float avatarY = y() + (h() - AVATAR_SIZE) / 2f;
                Identifier avatarTexture = AvatarTextureCache.INSTANCE.get(
                        request.user().minecraftUuid(), SocialScreen.this::rebuildHost);
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
                        boolean accepted = AlumiteApi.INSTANCE.acceptFriendRequest(request.requestId());
                        if (accepted) {
                            Toast.show().message("You're now friends with " + request.user().minecraftName() + "!").success();
                        }
                        Minecraft.getInstance().execute(SocialScreen.this::rebuildHost);
                    });
                    return true;
                }
                if (pointerX >= declineBtnX && pointerX < declineBtnX + BTN_W
                        && pointerY >= btnY && pointerY < btnY + BTN_H) {
                    FascinatedUtils.SCHEDULED_POOL.execute(() -> {
                        AlumiteApi.INSTANCE.declineFriendRequest(request.requestId());
                        Minecraft.getInstance().execute(SocialScreen.this::rebuildHost);
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
                graphics.fillRoundedRect(x(), y(), w(), h(), 5f,
                        rowHovered ? 0x22FFFFFF : UITheme.COLOR_BACKGROUND, RectCornerRoundMask.ALL);

                float avatarX = x() + 4f;
                float avatarY = y() + (h() - AVATAR_SIZE) / 2f;
                String receiverUuid = request.user().minecraftUuid();
                Identifier avatarTexture = receiverUuid != null
                        ? AvatarTextureCache.INSTANCE.get(receiverUuid, SocialScreen.this::rebuildHost)
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
                if (containsPoint(pointerX, pointerY)
                        && pointerX >= cancelBtnX && pointerX < cancelBtnX + BTN_W
                        && pointerY >= cancelBtnY && pointerY < cancelBtnY + BTN_H) {
                    FascinatedUtils.SCHEDULED_POOL.execute(() -> {
                        AlumiteApi.INSTANCE.cancelFriendRequest(request.requestId());
                        Minecraft.getInstance().execute(SocialScreen.this::rebuildHost);
                    });
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
        if (presence == null || "offline".equals(presence.status())) {
            if (presence != null && presence.lastSeen() != null) {
                try {
                    long secsAgo = Duration.between(Instant.parse(presence.lastSeen()), Instant.now()).getSeconds();
                    return "Offline \u00b7 " + TimeUtils.timeAgo(secsAgo * 1000, 2);
                } catch (Exception ignored) {}
            }
            return "Offline";
        }
        return switch (presence.status()) {
            case "online" -> "Online";
            case "away" -> "Away";
            case "invisible" -> "Invisible";
            default -> presence.status();
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

    private FWidget buildFooter() {
        FButtonWidget addBtn = new FButtonWidget(() -> {
            String username = addFriendInput.value().trim();
            if (username.isEmpty()) { return; }
            addFriendInput.setValue("");
            FascinatedUtils.SCHEDULED_POOL.execute(() -> {
                PendingFriendRequestDto dto = AlumiteApi.INSTANCE.sendFriendRequest(username);
                if (dto != null) {
                    SocialRegistry.INSTANCE.addOutgoingFriendRequest(dto);
                    Toast.show().message("Friend request sent!").success();
                    Minecraft.getInstance().execute(this::rebuildHost);
                } else {
                    Toast.show().message("Failed to send request.").error();
                }
            });
        }, () -> Component.translatable("fascinatedutils.social.add_button").getString(), ADD_BTN_W, 1, 1f, 4f, 1f, 4f) {
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
                addChild(addFriendInput);
                addChild(addBtn);
            }

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
                float inputW = lw - ADD_BTN_W - 4f;
                float inputH = addFriendInput.intrinsicHeightForColumn(measure, inputW);
                float inputY = ly + (lh - inputH) / 2f;
                addFriendInput.layout(measure, lx, inputY, inputW, inputH);
                addBtn.layout(measure, lx + inputW + 4f, ly + (lh - BTN_H) / 2f, ADD_BTN_W, BTN_H);
            }
        };
    }

    @Override
    public void renderCustom(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        Minecraft minecraftClient = Minecraft.getInstance();
        float uiWidth = UIScale.uiWidth();
        float uiHeight = UIScale.uiHeight();
        float pX = UIScale.uiPointerX();
        float pY = UIScale.uiPointerY();
        float deltaSeconds = minecraftClient.getDeltaTracker().getGameTimeDeltaTicks() / 20f;
        if (deltaSeconds <= 0f || Float.isNaN(deltaSeconds)) {
            deltaSeconds = delta / 20f;
        }

        GuiRenderer renderer = new GuiRenderer(graphics, FascinatedGuiTheme.INSTANCE);
        renderer.begin(uiWidth, uiHeight);
        host.tickAnimations(deltaSeconds);
        host.layoutAndRender(renderer, 0f, 0f, uiWidth, uiHeight, pX, pY, deltaSeconds);
        renderer.end();

        host.dispatchInput(new InputEvent.MouseMove(pX, pY));
        UiCursorController.apply(minecraftClient.getWindow().handle(), host.pointerCursorAt(pX, pY));

        if (scrollAccum != 0f) {
            host.dispatchInput(new InputEvent.MouseScroll(pX, pY, scrollAccum));
            scrollAccum = 0f;
        }
    }

    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
    }

    public void renderBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // intentionally empty; the panel is rendered in renderCustom
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubled) {
        float pX = UIScale.uiPointerX();
        float pY = UIScale.uiPointerY();
        boolean handled = host.dispatchInput(new InputEvent.MousePress(pX, pY, event.button()));
        return handled || super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        float pX = UIScale.uiPointerX();
        float pY = UIScale.uiPointerY();
        host.dispatchInput(new InputEvent.MouseRelease(pX, pY, event.button()));
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dragX, double dragY) {
        float pX = UIScale.uiPointerX();
        float pY = UIScale.uiPointerY();
        host.dispatchInput(new InputEvent.MouseMove(pX, pY));
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollAccum += (float) verticalAmount;
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        boolean handled = host.dispatchInput(new InputEvent.KeyPress(event.key(), event.scancode(), event.modifiers()));
        if (handled) { return true; }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            Minecraft.getInstance().setScreen(null);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        int codepoint = event.codepoint();
        if (codepoint >= 0 && codepoint <= 0xFFFF) {
            return host.dispatchInput(new InputEvent.CharType((char) codepoint));
        }
        return super.charTyped(event);
    }

    @Override
    public void removed() {
        UiCursorController.apply(Minecraft.getInstance().getWindow().handle(), UiPointerCursor.DEFAULT);
        host.dispose();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}