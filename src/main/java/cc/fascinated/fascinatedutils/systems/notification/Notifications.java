package cc.fascinated.fascinatedutils.systems.notification;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.channel.json.AttachmentDTO;
import cc.fascinated.fascinatedutils.api.user.UserStatus;
import cc.fascinated.fascinatedutils.api.user.User;
import cc.fascinated.fascinatedutils.event.impl.social.ChannelMessageCreateEvent;
import cc.fascinated.fascinatedutils.event.impl.social.FriendAddEvent;
import cc.fascinated.fascinatedutils.event.impl.social.FriendRequestIncomingEvent;
import cc.fascinated.fascinatedutils.gui.screens.SocialScreen;
import cc.fascinated.fascinatedutils.gui.toast.Toast;
import cc.fascinated.fascinatedutils.settings.SettingsRegistry;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;

import java.util.Objects;

public class Notifications {
    @EventHandler
    public void onMessageCreate(ChannelMessageCreateEvent event) {
        if (!shouldToast()) {
            return;
        }

        String authorId = event.message().authorId();
        if (Objects.equals(Alumite.INSTANCE.users().selfUser().user().id(), authorId)) {
            return;
        }
        User user = Alumite.INSTANCE.users().getUser(authorId);
        String title = user != null ? user.minecraftName() : "User " + authorId;

        String content = event.message().content();
        AttachmentDTO firstImage = event.message().attachments().stream()
                .filter(att -> att.mimeType() != null && att.mimeType().startsWith("image/"))
                .findFirst().orElse(null);
        Toast.Builder builder = Toast.show()
                .title(title)
                .message(content != null ? content : "");
        if (firstImage != null) {
            builder.imageId(firstImage.id()).imageUrl(firstImage.url())
                    .imageWidth(firstImage.width()).imageHeight(firstImage.height());
        }
        builder.info();
    }

    @EventHandler
    public void onFriendAdd(FriendAddEvent event) {
        if (!shouldToast()) {
            return;
        }

        if (event.fromOutgoingRequest() && event.user() != null && event.user().minecraftName() != null) {
            Toast.show().message(event.user().minecraftName() + " accepted your friend request!").success();
        }
    }

    @EventHandler
    public void onFriendRequestIncoming(FriendRequestIncomingEvent event) {
        if (!shouldToast()) {
            return;
        }

        if (event.user() != null && event.user().minecraftName() != null) {
            Toast.show().message(event.user().minecraftName() + " sent you a friend request!").info();
        }
    }

    public boolean shouldToast() {
        return !SettingsRegistry.INSTANCE.getSettings().getSocialNotifications().isDisabled() &&
                Alumite.INSTANCE.users().selfUser().preferredUserStatus() != UserStatus.DO_NOT_DISTURB &&
                !(Minecraft.getInstance().screen instanceof SocialScreen);
    }
}
