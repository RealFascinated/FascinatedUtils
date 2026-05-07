package cc.fascinated.fascinatedutils.systems.notification;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.user.UserStatus;
import cc.fascinated.fascinatedutils.api.user.User;
import cc.fascinated.fascinatedutils.event.impl.social.ChannelMessageCreateEvent;
import cc.fascinated.fascinatedutils.event.impl.social.FriendAddEvent;
import cc.fascinated.fascinatedutils.event.impl.social.FriendRequestIncomingEvent;
import cc.fascinated.fascinatedutils.gui.toast.Toast;
import meteordevelopment.orbit.EventHandler;

import java.util.Objects;

public class MessageNotifications {
    @EventHandler
    public void onMessageCreate(ChannelMessageCreateEvent event) {
        if (Alumite.INSTANCE.users().selfUser().preferredUserStatus() == UserStatus.DO_NOT_DISTURB) {
            return;
        }
        String authorId = event.message().authorId();
        if (Objects.equals(Alumite.INSTANCE.users().selfUser().user().id(), authorId)) {
            return;
        }
        User user = Alumite.INSTANCE.users().getUser(authorId);
        String title = user != null ? user.minecraftName() : "User " + authorId;

        Toast.show().title(title).message(event.message().content()).info();
    }

    @EventHandler
    public void onFriendAdd(FriendAddEvent event) {
        if (event.fromOutgoingRequest() && event.user() != null && event.user().minecraftName() != null) {
            Toast.show().message(event.user().minecraftName() + " accepted your friend request!").success();
        }
    }

    @EventHandler
    public void onFriendRequestIncoming(FriendRequestIncomingEvent event) {
        if (event.user() != null && event.user().minecraftName() != null) {
            Toast.show().message(event.user().minecraftName() + " sent you a friend request!").info();
        }
    }
}
