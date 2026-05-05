package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.api.friend.Friend;
import cc.fascinated.fascinatedutils.api.friend.PendingFriendRequest;
import cc.fascinated.fascinatedutils.gui.widgets.FColumnWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FScrollColumnWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class SocialFriendsListWidget {

    public record Props(
            List<Friend> friends,
            List<PendingFriendRequest> incomingRequests,
            List<PendingFriendRequest> outgoingRequests,
            float scrollY,
            Consumer<Float> scrollYSink,
            Function<Friend, FWidget> friendRowFactory,
            Function<PendingFriendRequest, FWidget> incomingRowFactory,
            Function<PendingFriendRequest, FWidget> outgoingRowFactory,
            Function<String, FWidget> sectionLabelFactory,
            String incomingLabel,
            String outgoingLabel,
            Supplier<FWidget> emptyFactory,
            Supplier<FWidget> emptyFriendsFactory
    ) {}

    public static FScrollColumnWidget build(Props props) {
        FColumnWidget body = new FColumnWidget(4f, cc.fascinated.fascinatedutils.gui.core.Align.START);
        List<Friend> friends = props.friends();
        List<PendingFriendRequest> incoming = props.incomingRequests();
        List<PendingFriendRequest> outgoing = props.outgoingRequests();

        boolean hasFriends = friends != null && !friends.isEmpty();
        boolean hasIncoming = incoming != null && !incoming.isEmpty();
        boolean hasOutgoing = outgoing != null && !outgoing.isEmpty();

        if (!hasFriends && !hasIncoming && !hasOutgoing) {
            body.addChild(props.emptyFactory().get());
        } else {
            if (hasFriends) {
                for (Friend friend : friends) {
                    body.addChild(props.friendRowFactory().apply(friend));
                }
            } else {
                body.addChild(props.emptyFriendsFactory().get());
            }
            if (hasIncoming) {
                body.addChild(props.sectionLabelFactory().apply(props.incomingLabel()));
                for (PendingFriendRequest request : incoming) {
                    body.addChild(props.incomingRowFactory().apply(request));
                }
            }
            if (hasOutgoing) {
                body.addChild(props.sectionLabelFactory().apply(props.outgoingLabel()));
                for (PendingFriendRequest request : outgoing) {
                    body.addChild(props.outgoingRowFactory().apply(request));
                }
            }
        }

        FScrollColumnWidget scroll = FTheme.components().createScrollColumn(body, 3f);
        scroll.setScrollOffsetY(props.scrollY());
        scroll.setScrollOffsetChangeListener(props.scrollYSink());
        return scroll;
    }
}
