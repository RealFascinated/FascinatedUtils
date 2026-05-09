package cc.fascinated.fascinatedutils.gui2.node.social.chat;

import cc.fascinated.fascinatedutils.api.channel.Channel;
import cc.fascinated.fascinatedutils.api.channel.DmChannel;
import cc.fascinated.fascinatedutils.api.user.User;
import cc.fascinated.fascinatedutils.api.user.UserStatus;
import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.node.DividerNode;
import cc.fascinated.fascinatedutils.gui2.node.social.player.PlayerAvatarNode;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Header bar for the right (chat) pane showing the DM partner's avatar, name, and status.
 *
 * <p>Shows "Friends" as a plain text title when no channel is selected and the friends tab is
 * active, or the DM partner's name + avatar when a channel is open.
 */
public class ChatHeaderNode extends PositionedNode {

    private static final int AVATAR_SIZE = 22;
    private static final int PADDING = 8;

    private final Supplier<Channel> channelSupplier;
    private final DividerNode divider;
    private final PlayerAvatarNode avatar;
    private BiConsumer<Float, Float> onSecondaryClick;

    public ChatHeaderNode(Supplier<Channel> channelSupplier) {
        this.channelSupplier = channelSupplier;
        height(40).fullWidth();

        divider = new DividerNode();
        divider.bottom(0).fullWidth();
        addChild(divider);

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
        addChild(avatar);
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
        int separatorHeight = renderFrame.theme().separatorThickness();
        divider.height(separatorHeight);
        super.layout(renderFrame, parentX, parentY, parentWidth, parentHeight);
        int posX = bounds().positionX();
        int posY = bounds().positionY();
        int height = bounds().height() - separatorHeight;
        avatar.layout(renderFrame, posX + PADDING, posY + (height - AVATAR_SIZE) / 2, AVATAR_SIZE, AVATAR_SIZE);
    }

    @Override
    protected void renderSelf(RenderFrame renderFrame, float deltaSeconds) {
        int posX = bounds().positionX();
        int posY = bounds().positionY();
        int height = bounds().height() - renderFrame.theme().separatorThickness();

        Channel channel = channelSupplier.get();
        DmChannel dm = channel == null ? null : channel.asDmChannel();
        User recipient = dm != null ? dm.recipient() : null;

        if (recipient == null) {
            return;
        }

        int textX = posX + PADDING + AVATAR_SIZE + 8;
        int lineH = renderFrame.fontHeight();
        String name = recipient.minecraftName() != null ? recipient.minecraftName() : "";

        if (recipient.activity() != null) {
            int blockH = lineH * 2 + 3;
            int nameY = posY + (height - blockH) / 2;
            renderFrame.drawText(name, textX, nameY, renderFrame.theme().textPrimary(), false, true);
            renderFrame.drawText(recipient.activity().label(), textX, nameY + lineH + 3, renderFrame.theme().textMuted(), false, false);
        } else {
            renderFrame.drawText(name, textX, posY + (height - lineH) / 2, renderFrame.theme().textPrimary(), false, true);
        }
    }
}
