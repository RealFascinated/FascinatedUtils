package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.channel.Channel;
import cc.fascinated.fascinatedutils.api.channel.DmChannel;
import cc.fascinated.fascinatedutils.api.friend.Friend;
import cc.fascinated.fascinatedutils.api.user.Activity;
import cc.fascinated.fascinatedutils.api.user.User;
import cc.fascinated.fascinatedutils.api.user.UserStatus;
import cc.fascinated.fascinatedutils.gui.core.UiFrameContext;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class SocialChatHeaderWidget extends FWidget {

    private static final float AVATAR_SIZE = 22f;

    private final BooleanSupplier isFriendsTab;
    private final Supplier<Friend> selectedFriend;
    private final Supplier<String> selectedChannelId;
    private final SocialPlayerAvatarWidget avatar;

    public SocialChatHeaderWidget(BooleanSupplier isFriendsTab, Supplier<Friend> selectedFriend,
                                  Supplier<String> selectedChannelId) {
        this.isFriendsTab = isFriendsTab;
        this.selectedFriend = selectedFriend;
        this.selectedChannelId = selectedChannelId;
        avatar = new SocialPlayerAvatarWidget(
                AVATAR_SIZE,
                () -> ChannelUtils.dmAvatarMinecraftUuid(resolveChannel()),
                () -> ChannelUtils.title(resolveChannel()),
                () -> {
                    Channel channel = resolveChannel();
                    DmChannel dmChannel = channel == null ? null : channel.asDmChannel();
                    if (dmChannel == null || dmChannel.recipient() == null) {
                        return UserStatus.OFFLINE.color();
                    }
                    User recipient = dmChannel.recipient();
                    UserStatus status = recipient.userStatus();
                    return (status != null ? status : UserStatus.OFFLINE).color();
                });
        addChild(avatar);
    }

    @Override
    public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
        setBounds(lx, ly, lw, lh);
        avatar.layout(measure, lx, ly + (lh - AVATAR_SIZE) / 2f, AVATAR_SIZE, AVATAR_SIZE);
    }

    @Override
    protected void renderSelf(GuiRenderer graphics, UiFrameContext frame, float deltaSeconds) {
        float titleY = y() + (h() - graphics.getFontCapHeight()) / 2f;
        float capHeight = graphics.getFontCapHeight();

        if (isFriendsTab.getAsBoolean()) {
            Friend friend = selectedFriend.get();
            if (friend == null) {
                graphics.drawText(Component.translatable("alumite.social.tab_friends").getString(), x(), titleY, FascinatedGuiTheme.INSTANCE.textPrimary(), true, false);
            } else {
                User friendUser = friend.user();
                Activity friendActivity = friendUser != null ? friendUser.activity() : null;
                if (friendActivity != null) {
                    float blockY = y() + (h() - capHeight * 2 - 3f) / 2f;
                    graphics.drawText("Friend Profile", x(), blockY, FascinatedGuiTheme.INSTANCE.textPrimary(), true, false);
                    graphics.drawText(friendActivity.label(), x(), blockY + capHeight + 3f, FascinatedGuiTheme.INSTANCE.textMuted(), false, false);
                } else {
                    graphics.drawText("Friend Profile", x(), titleY, FascinatedGuiTheme.INSTANCE.textPrimary(), true, false);
                }
            }
            avatar.setVisible(false);
            return;
        }

        Channel channel = resolveChannel();
        String title = ChannelUtils.title(channel);
        String avatarUuid = ChannelUtils.dmAvatarMinecraftUuid(channel);
        boolean showAvatar = avatarUuid != null && !avatarUuid.isBlank();
        avatar.setVisible(showAvatar);
        float textLeft = showAvatar ? x() + AVATAR_SIZE + 8f : x();

        DmChannel dmChannel = channel == null ? null : channel.asDmChannel();
        User recipient = dmChannel != null ? dmChannel.recipient() : null;
        Activity recipientActivity = recipient != null ? recipient.activity() : null;
        if (recipientActivity != null) {
            float blockY = y() + (h() - capHeight * 2 - 3f) / 2f;
            graphics.drawText(title, textLeft, blockY, FascinatedGuiTheme.INSTANCE.textPrimary(), true, false);
            graphics.drawText(recipientActivity.label(), textLeft, blockY + capHeight + 3f, FascinatedGuiTheme.INSTANCE.textMuted(), false, false);
        } else {
            graphics.drawText(title, textLeft, titleY, FascinatedGuiTheme.INSTANCE.textPrimary(), true, false);
        }
    }

    private Channel resolveChannel() {
        String channelId = selectedChannelId.get();
        return channelId == null ? null : Alumite.INSTANCE.channels().get(channelId);
    }
}
