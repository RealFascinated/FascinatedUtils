package cc.fascinated.fascinatedutils.gui2.node.screenshot;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.AlumiteApiException;
import cc.fascinated.fascinatedutils.api.channel.DmChannel;
import cc.fascinated.fascinatedutils.api.user.User;
import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.node.ScrollColumnNode;
import cc.fascinated.fascinatedutils.gui2.node.ButtonNode;
import cc.fascinated.fascinatedutils.gui2.node.PopupNode;
import cc.fascinated.fascinatedutils.gui2.node.TextNode;
import cc.fascinated.fascinatedutils.gui2.node.social.player.FriendRowNode;
import cc.fascinated.fascinatedutils.gui2.screens.impl.SocialScreen;
import cc.fascinated.fascinatedutils.gui2.theme.UiTheme;
import cc.fascinated.fascinatedutils.systems.screenshot.Screenshot;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class SendScreenshotPopupNode extends PopupNode<SendScreenshotPopupNode> {

    private static final int POPUP_WIDTH = 260;
    private static final int POPUP_HEIGHT = 300;
    private static final int PAD = 10;
    private static final int TITLE_HEIGHT = 14;
    private static final int SEND_BUTTON_WIDTH = 50;
    private static final int SEND_BUTTON_HEIGHT = 18;
    private static final int CLOSE_BUTTON_WIDTH = 80;
    private static final int CLOSE_BUTTON_HEIGHT = 20;
    private static final int ROW_HEIGHT = 28;

    public SendScreenshotPopupNode(Screenshot screenshot, Runnable onDismiss) {
        setPopupWidth(POPUP_WIDTH);
        setPopupHeight(POPUP_HEIGHT);
        setOnDismiss(onDismiss);

        TextNode titleNode = new TextNode(() -> Component.translatable("alumite.screenshot.send_popup.title").getString());
        titleNode.setBold(true)
                .setColorResolver(UiTheme::textPrimary)
                .alignX(0.5f)
                .top(PAD)
                .height(TITLE_HEIGHT);
        addPopupChild(titleNode);

        int listTop = PAD + TITLE_HEIGHT + PAD;

        ButtonNode closeButton = new ButtonNode(() -> Component.translatable("alumite.screenshot.send_popup.close").getString());
        closeButton.size(CLOSE_BUTTON_WIDTH, CLOSE_BUTTON_HEIGHT).alignX(0.5f).bottom(PAD);
        closeButton.setRounded(true).setOnPress(onDismiss);
        addPopupChild(closeButton);

        if (Alumite.INSTANCE == null) {
            TextNode disconnectedLabel = new TextNode(() -> Component.translatable("alumite.screenshot.send_popup.not_connected").getString());
            disconnectedLabel.setColorResolver(UiTheme::textMuted)
                    .alignX(0.5f)
                    .fullWidth()
                    .top(listTop)
                    .height(20);
            addPopupChild(disconnectedLabel);
            return;
        }

        List<User> friends = Alumite.INSTANCE.users().getFriends();

        ScrollColumnNode friendList = new ScrollColumnNode();
        friendList.setGap(2);

        PositionedNode<?> listArea = new PositionedNode<>();
        listArea.left(PAD).right(PAD).top(listTop).bottom(PAD + CLOSE_BUTTON_HEIGHT + PAD);
        listArea.addChild(friendList);

        if (friends == null || friends.isEmpty()) {
            TextNode emptyLabel = new TextNode(() -> Component.translatable("alumite.screenshot.send_popup.no_friends").getString());
            emptyLabel.setColorResolver(UiTheme::textMuted)
                    .setTextAlign(0.5f, 0.5f)
                    .fullWidth()
                    .height(40);
            friendList.addChild(emptyLabel);
        } else {
            for (User friend : friends) {
                PositionedNode<?> row = new PositionedNode<>().fullWidth().height(ROW_HEIGHT);

                FriendRowNode friendRow = new FriendRowNode(friend);
                friendRow.height(ROW_HEIGHT).left(0).right(SEND_BUTTON_WIDTH + 4);
                friendRow.setAvatarSize(18);
                friendRow.setRowHeight(ROW_HEIGHT);
                friendRow.setTextScale(0.85f);
                friendRow.setHoverEnabled(false);

                ButtonNode sendButton = new ButtonNode(() -> Component.translatable("alumite.screenshot.send_popup.send").getString());
                sendButton.size(SEND_BUTTON_WIDTH, SEND_BUTTON_HEIGHT).right(2).alignY(0.5f);
                sendButton.setRounded(true).setOnPress(() -> {
                    onDismiss.run();
                    sendToFriend(screenshot, friend);
                });

                row.addChild(friendRow);
                row.addChild(sendButton);
                friendList.addChild(row);
            }
        }

        addPopupChild(listArea);
    }

    private static void sendToFriend(Screenshot screenshot, User friend) {
        CompletableFuture.runAsync(() -> {
            try {
                DmChannel channel = Alumite.INSTANCE.channels().openDm(friend.id());
                Minecraft instance = Minecraft.getInstance();
                instance.execute(() -> {
                    SocialScreen screen = new SocialScreen();
                    screen.openChannel(channel.id());
                    instance.setScreen(screen);
                    channel.sendMessage(null, screenshot.getPath());
                });
            } catch (AlumiteApiException ignored) {
            }
        });
    }
}
