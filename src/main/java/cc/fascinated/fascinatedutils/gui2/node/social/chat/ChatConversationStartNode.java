package cc.fascinated.fascinatedutils.gui2.node.social.chat;

import cc.fascinated.fascinatedutils.api.channel.Channel;
import cc.fascinated.fascinatedutils.api.channel.DmChannel;
import cc.fascinated.fascinatedutils.api.user.User;
import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.node.TextNode;
import cc.fascinated.fascinatedutils.gui2.node.social.player.PlayerAvatarNode;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;
import cc.fascinated.fascinatedutils.gui2.theme.UiTheme;

class ChatConversationStartNode extends PositionedNode<ChatConversationStartNode> {

    private static final int PAD_V = 16;
    private static final int AVATAR_SIZE = 48;
    private static final int GAP_AVATAR_NAME = 10;
    private static final int GAP_NAME_SUBTITLE = 4;

    private final PlayerAvatarNode avatar;
    private final TextNode nameText;
    private final TextNode subtitleText;

    ChatConversationStartNode(Channel channel) {
        fullWidth();

        DmChannel dm = channel.asDmChannel();
        User recipient = dm != null ? dm.recipient() : null;

        avatar = new PlayerAvatarNode(AVATAR_SIZE, () -> recipient);
        avatar.setShowStatusDot(false);

        String displayName = recipient != null && recipient.minecraftName() != null
                ? recipient.minecraftName()
                : channel.name() != null ? channel.name() : "Unknown";

        String subtitle = dm != null
                ? "This is the beginning of your direct message history with " + displayName + "."
                : "This is the beginning of " + (channel.name() != null ? channel.name() : "this group") + ".";

        nameText = new TextNode(() -> displayName)
                .setColorResolver(UiTheme::textPrimary)
                .setBold(true)
                .setTextAlign(0.5f, 0.5f);
        nameText.fullWidth();

        subtitleText = new TextNode(() -> subtitle)
                .setColorResolver(UiTheme::textMuted)
                .setTextAlign(0.5f, 0f)
                .setWrap(true);
        subtitleText.fullWidth();

        addChild(avatar);
        addChild(nameText);
        addChild(subtitleText);
    }

    @Override
    public void layout(RenderFrame renderFrame, int parentX, int parentY, int parentWidth, int parentHeight) {
        int cursorY = parentY + PAD_V;

        int avatarX = parentX + (parentWidth - AVATAR_SIZE) / 2;
        avatar.layout(renderFrame, avatarX, cursorY, AVATAR_SIZE, AVATAR_SIZE);
        cursorY += AVATAR_SIZE + GAP_AVATAR_NAME;

        nameText.layout(renderFrame, parentX, cursorY, parentWidth, renderFrame.fontHeight());
        cursorY += nameText.bounds().height() + GAP_NAME_SUBTITLE;

        subtitleText.layout(renderFrame, parentX, cursorY, parentWidth, parentHeight);
        cursorY += subtitleText.bounds().height() + PAD_V;

        bounds().set(parentX, parentY, parentWidth, cursorY - parentY);
    }
}
