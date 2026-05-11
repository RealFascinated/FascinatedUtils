package cc.fascinated.fascinatedutils.gui2.node.social.player;

import cc.fascinated.fascinatedutils.api.user.User;
import cc.fascinated.fascinatedutils.api.user.UserStatus;

public class FriendRowNode extends PlayerRowNode<FriendRowNode> {

    public FriendRowNode(User user) {
        super(() -> user, () -> {
            UserStatus status = user.userStatus();
            return (status != null ? status : UserStatus.OFFLINE).label();
        });
    }

    public FriendRowNode(User user, String subtext) {
        super(() -> user, () -> subtext);
    }
}
