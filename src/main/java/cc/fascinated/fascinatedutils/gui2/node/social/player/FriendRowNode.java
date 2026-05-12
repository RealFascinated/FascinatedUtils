package cc.fascinated.fascinatedutils.gui2.node.social.player;

import cc.fascinated.fascinatedutils.api.user.User;
import cc.fascinated.fascinatedutils.api.user.UserStatus;

public class FriendRowNode extends PlayerRowNode<FriendRowNode> {

    public FriendRowNode(User user) {
        super(() -> user, null);
    }

    public FriendRowNode(User user, String subtext) {
        super(() -> user, () -> subtext);
    }
}
