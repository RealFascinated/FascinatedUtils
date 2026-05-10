package cc.fascinated.fascinatedutils.gui2.node.social.chat;

import cc.fascinated.fascinatedutils.api.channel.Channel;
import cc.fascinated.fascinatedutils.api.channel.DmChannel;
import cc.fascinated.fascinatedutils.api.user.User;
import cc.fascinated.fascinatedutils.api.user.UserStatus;
import cc.fascinated.fascinatedutils.gui2.node.TextNode;
import cc.fascinated.fascinatedutils.gui2.node.social.SocialPanelHeaderNode;
import cc.fascinated.fascinatedutils.gui2.node.social.player.PlayerAvatarNode;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;
import cc.fascinated.fascinatedutils.gui2.theme.UiTheme;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class ChatHeaderNode extends SocialPanelHeaderNode {

    private static final int AVATAR_SIZE = 18;
    private static final int PADDING = 8;

    private final Supplier<Channel> channelSupplier;
    private final PlayerAvatarNode avatar;
    private final TextNode nameText;
    private final TextNode activityText;
    private BiConsumer<Float, Float> onSecondaryClick;

    public ChatHeaderNode(Supplier<Channel> channelSupplier) {
        this.channelSupplier = channelSupplier;

        avatar = new PlayerAvatarNode(AVATAR_SIZE, () -> {
            Channel channel = channelSupplier.get();
            DmChannel dm = channel == null ? null : channel.asDmChannel();
            return dm != null && dm.recipient() != null ? dm.recipient().minecraftUuid() : null;
        }, () -> {
            Channel channel = channelSupplier.get();
            DmChannel dm = channel == null ? null : channel.asDmChannel();
            return dm != null && dm.recipient() != null ? dm.recipient().minecraftName() : null;
        }, () -> {
            Channel channel = channelSupplier.get();
            DmChannel dm = channel == null ? null : channel.asDmChannel();
            if (dm == null || dm.recipient() == null) {
                return UserStatus.OFFLINE.color();
            }
            UserStatus status = dm.recipient().userStatus();
            return (status != null ? status : UserStatus.OFFLINE).color();
        });
        avatar.left(PADDING).alignY(0.5f);
        addChild(avatar);

        nameText = new TextNode(() -> {
            Channel channel = channelSupplier.get();
            DmChannel dm = channel == null ? null : channel.asDmChannel();
            User recipient = dm != null ? dm.recipient() : null;
            return recipient != null && recipient.minecraftName() != null ? recipient.minecraftName() : "";
        }).setColorResolver(UiTheme::textPrimary).setTextAlign(0f, 0.5f);
        addChild(nameText);

        activityText = new TextNode(() -> {
            Channel channel = channelSupplier.get();
            DmChannel dm = channel == null ? null : channel.asDmChannel();
            User recipient = dm != null ? dm.recipient() : null;
            return recipient != null && recipient.activity() != null ? recipient.activity().label() : "";
        }).setColorResolver(UiTheme::textMuted).setTextAlign(0f, 0.5f);
        addChild(activityText);
    }

    public ChatHeaderNode setOnSecondaryClick(BiConsumer<Float, Float> onSecondaryClick) {
        this.onSecondaryClick = onSecondaryClick;
        return this;
    }

    @Override
    public boolean blocksHitWhenEmpty() {
        return true;
    }

    @Override
    public boolean onClick(float pointerX, float pointerY, int button) {
        if (button == 1 && onSecondaryClick != null) {
            onSecondaryClick.accept(pointerX, pointerY);
            return true;
        }
        return false;
    }

    @Override
    public void layout(RenderFrame renderFrame, int parentX, int parentY, int parentWidth, int parentHeight) {
        super.layout(renderFrame, parentX, parentY, parentWidth, parentHeight);
        int posX = bounds().positionX();
        int posY = bounds().positionY();
        int width = bounds().width();
        int height = bounds().height() - renderFrame.theme().separatorThickness();

        Channel channel = channelSupplier.get();
        DmChannel dm = channel == null ? null : channel.asDmChannel();
        User recipient = dm != null ? dm.recipient() : null;
        nameText.setVisible(recipient != null);
        activityText.setVisible(recipient != null && recipient.activity() != null);

        if (recipient != null) {
            float textScale = 0.85f;
            nameText.setScale(textScale);
            activityText.setScale(textScale);
            int textX = posX + PADDING + AVATAR_SIZE + 8;
            int textW = Math.max(0, width - (textX - posX) - PADDING);
            int lineH = Math.round(renderFrame.fontHeight() * textScale);
            if (recipient.activity() != null) {
                int blockH = lineH * 2 + 3;
                int nameY = posY + (height - blockH) / 2;
                nameText.layout(renderFrame, textX, nameY, textW, lineH);
                activityText.layout(renderFrame, textX, nameY + lineH + 3, textW, lineH);
            } else {
                nameText.layout(renderFrame, textX, posY + (height - lineH) / 2, textW, lineH);
            }
        }
    }
}
