package cc.fascinated.fascinatedutils.gui2.node.social.player;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.friend.PendingFriendRequest;
import cc.fascinated.fascinatedutils.api.user.User;
import cc.fascinated.fascinatedutils.gui2.node.ScrollColumnNode;
import cc.fascinated.fascinatedutils.gui2.node.TextNode;
import cc.fascinated.fascinatedutils.gui2.node.social.SocialSectionLabelNode;
import net.minecraft.network.chat.Component;

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
            TextNode emptyLabel = new TextNode(() -> Component.translatable("alumite.social.no_friends").getString())
                    .setColorArgb(0x88FFFFFF)
                    .setTextAlign(0f, 0.5f);
            emptyLabel.fullWidth().height(40);
            addChild(emptyLabel);
            return;
        }

        if (hasFriends) {
            for (User friend : friends) {
                FriendRowNode row = new FriendRowNode(friend);
                row.setOnSecondaryClick((pointerX, pointerY) -> {
                    if (contextMenuHandler != null) {
                        contextMenuHandler.open(friend, pointerX, pointerY);
                    }
                });
                addChild(row);
            }
        }

        if (hasIncoming) {
            addChild(new SocialSectionLabelNode(Component.translatable("alumite.social.requests_incoming_heading").getString()));
            for (PendingFriendRequest request : incoming) {
                User requestUser = request.user();
                FriendRowNode row = new FriendRowNode(requestUser, Component.translatable("alumite.social.request_status_incoming").getString());
                row.setOnSecondaryClick((pointerX, pointerY) -> {
                    if (contextMenuHandler != null) {
                        contextMenuHandler.open(requestUser, pointerX, pointerY);
                    }
                });
                addChild(row);
            }
        }

        if (hasOutgoing) {
            addChild(new SocialSectionLabelNode(Component.translatable("alumite.social.requests_sent_heading").getString()));
            for (PendingFriendRequest request : outgoing) {
                User requestUser = request.user();
                FriendRowNode row = new FriendRowNode(requestUser, Component.translatable("alumite.social.request_status_outgoing").getString());
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