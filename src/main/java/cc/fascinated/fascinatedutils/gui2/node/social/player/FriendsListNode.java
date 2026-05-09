package cc.fascinated.fascinatedutils.gui2.node.social.player;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.friend.PendingFriendRequest;
import cc.fascinated.fascinatedutils.api.user.User;
import cc.fascinated.fascinatedutils.api.user.UserStatus;
import cc.fascinated.fascinatedutils.gui2.core.ScrollColumnNode;
import cc.fascinated.fascinatedutils.gui2.node.TextNode;
import cc.fascinated.fascinatedutils.gui2.node.social.SocialSectionLabelNode;
import cc.fascinated.fascinatedutils.gui2.node.social.player.PlayerContextMenuHandler;

import java.util.List;

public class FriendsListNode extends ScrollColumnNode {

    public FriendsListNode(PlayerContextMenuHandler contextMenuHandler) {
        setGap(2);

        if (Alumite.INSTANCE == null) {
            return;
        }
        List<User> friends = Alumite.INSTANCE.users().getFriends();
        List<PendingFriendRequest> incoming = Alumite.INSTANCE.users().incomingFriendRequests();
        List<PendingFriendRequest> outgoing = Alumite.INSTANCE.users().outgoingFriendRequests();

        boolean hasFriends = friends != null && !friends.isEmpty();
        boolean hasIncoming = incoming != null && !incoming.isEmpty();
        boolean hasOutgoing = outgoing != null && !outgoing.isEmpty();

        if (!hasFriends && !hasIncoming && !hasOutgoing) {
            TextNode emptyLabel = new TextNode("No friends yet")
                    .setColorArgb(0x88FFFFFF)
                    .setTextAlign(0f, 0.5f);
            emptyLabel.fullWidth().height(40);
            addChild(emptyLabel);
            return;
        }

        if (hasFriends) {
            for (User friend : friends) {
                String statusLabel = friend.userStatus() != null ? friend.userStatus().label() : UserStatus.OFFLINE.label();
                PlayerRowNode row = new PlayerRowNode(() -> friend, () -> statusLabel);
                row.setOnSecondaryClick((pointerX, pointerY) -> {
                    if (contextMenuHandler != null) {
                        contextMenuHandler.open(friend, pointerX, pointerY);
                    }
                });
                addChild(row);
            }
        }

        if (hasIncoming) {
            addChild(new SocialSectionLabelNode("Incoming Requests"));
            for (PendingFriendRequest request : incoming) {
                User requestUser = request.user();
                PlayerRowNode row = new PlayerRowNode(() -> requestUser, () -> "Wants to be friends");
                row.setOnSecondaryClick((pointerX, pointerY) -> {
                    if (contextMenuHandler != null) {
                        contextMenuHandler.open(requestUser, pointerX, pointerY);
                    }
                });
                addChild(row);
            }
        }

        if (hasOutgoing) {
            addChild(new SocialSectionLabelNode("Sent Requests"));
            for (PendingFriendRequest request : outgoing) {
                User requestUser = request.user();
                PlayerRowNode row = new PlayerRowNode(() -> requestUser, () -> "Pending...");
                row.setOnSecondaryClick((pointerX, pointerY) -> {
                    if (contextMenuHandler != null) {
                        contextMenuHandler.open(requestUser, pointerX, pointerY);
                    }
                });
                addChild(row);
            }
        }
    }

}