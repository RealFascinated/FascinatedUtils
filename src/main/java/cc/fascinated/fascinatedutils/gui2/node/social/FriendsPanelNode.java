package cc.fascinated.fascinatedutils.gui2.node.social;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.friend.PendingFriendRequest;
import cc.fascinated.fascinatedutils.api.user.User;
import cc.fascinated.fascinatedutils.api.user.UserStatus;
import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.core.ScrollColumnNode;
import cc.fascinated.fascinatedutils.gui2.node.TextNode;
import cc.fascinated.fascinatedutils.gui2.node.social.player.PlayerContextMenuHandler;
import cc.fascinated.fascinatedutils.gui2.node.social.player.PlayerRowNode;
import cc.fascinated.fascinatedutils.gui2.theme.UiThemeRepository;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class FriendsPanelNode extends PositionedNode {

    private static final int HEADER_HEIGHT = 40;
    private static final int LIST_PADDING = 8;

    // Statuses shown in order; INVISIBLE is folded into OFFLINE before grouping.
    private static final UserStatus[] STATUS_ORDER = {
            UserStatus.ONLINE,
            UserStatus.AWAY,
            UserStatus.DO_NOT_DISTURB,
            UserStatus.OFFLINE
    };

    public FriendsPanelNode(PlayerContextMenuHandler contextMenuHandler, Consumer<User> onOpenDm) {
        full();

        FriendsHeaderNode header = new FriendsHeaderNode();
        header.top(0);
        addChild(header);

        PositionedNode listArea = new PositionedNode()
                .fullWidth()
                .top(HEADER_HEIGHT)
                .bottom(0);
        PositionedNode listPadded = new PositionedNode()
                .left(LIST_PADDING).right(LIST_PADDING)
                .top(LIST_PADDING).bottom(LIST_PADDING);
        listPadded.addChild(buildList(contextMenuHandler, onOpenDm));
        listArea.addChild(listPadded);
        addChild(listArea);
    }

    private static ScrollColumnNode buildList(PlayerContextMenuHandler contextMenuHandler, Consumer<User> onOpenDm) {
        ScrollColumnNode list = new ScrollColumnNode();
        list.setGap(2);

        if (Alumite.INSTANCE == null) {
            return list;
        }

        List<User> friends = Alumite.INSTANCE.users().getFriends();
        List<PendingFriendRequest> incoming = Alumite.INSTANCE.users().incomingFriendRequests();
        List<PendingFriendRequest> outgoing = Alumite.INSTANCE.users().outgoingFriendRequests();

        boolean anyContent = (friends != null && !friends.isEmpty())
                || (incoming != null && !incoming.isEmpty())
                || (outgoing != null && !outgoing.isEmpty());

        if (!anyContent) {
            TextNode empty = new TextNode(Component.translatable("alumite.social.no_friends").getString())
                    .setColorArgb(UiThemeRepository.get().textSubtle())
                    .setTextAlign(0f, 0.5f);
            empty.fullWidth().height(40);
            list.addChild(empty);
            return list;
        }

        if (friends != null && !friends.isEmpty()) {
            // Fold INVISIBLE into OFFLINE so they share one section.
            Map<UserStatus, List<User>> byStatus = friends.stream()
                    .collect(Collectors.groupingBy(user -> {
                        UserStatus status = user.userStatus();
                        if (status == null || status == UserStatus.INVISIBLE) {
                            return UserStatus.OFFLINE;
                        }
                        return status;
                    }));

            for (UserStatus status : STATUS_ORDER) {
                List<User> group = byStatus.get(status);
                if (group == null || group.isEmpty()) {
                    continue;
                }
                list.addChild(new SocialSectionLabelNode(String.format(Component.translatable("alumite.social.status_section_fmt").getString(), status.label(), group.size())));
                for (User friend : group) {
                    String subtext = status.label();
                    PlayerRowNode row = new PlayerRowNode(() -> friend, () -> subtext)
                            .setAvatarSize(18)
                            .setRowHeight(28)
                            .setTextScale(0.85f);
                    row.setOnPrimaryClick(() -> {
                        if (onOpenDm != null) {
                            onOpenDm.accept(friend);
                        }
                    });
                    row.setOnSecondaryClick((pointerX, pointerY) -> {
                        if (contextMenuHandler != null) {
                            contextMenuHandler.open(friend, pointerX, pointerY);
                        }
                    });
                    list.addChild(row);
                }
            }
        }

        if (incoming != null && !incoming.isEmpty()) {
            list.addChild(new SocialSectionLabelNode(String.format(Component.translatable("alumite.social.requests_pending_fmt").getString(), incoming.size())));
            for (PendingFriendRequest request : incoming) {
                User requestUser = request.user();
                PlayerRowNode row = new PlayerRowNode(() -> requestUser, () -> Component.translatable("alumite.social.request_status_incoming").getString())
                        .setAvatarSize(18)
                        .setRowHeight(28)
                        .setTextScale(0.85f);
                row.setOnSecondaryClick((pointerX, pointerY) -> {
                    if (contextMenuHandler != null) {
                        contextMenuHandler.open(requestUser, pointerX, pointerY);
                    }
                });
                list.addChild(row);
            }
        }

        if (outgoing != null && !outgoing.isEmpty()) {
            list.addChild(new SocialSectionLabelNode(String.format(Component.translatable("alumite.social.requests_sent_fmt").getString(), outgoing.size())));
            for (PendingFriendRequest request : outgoing) {
                User requestUser = request.user();
                PlayerRowNode row = new PlayerRowNode(() -> requestUser, () -> Component.translatable("alumite.social.request_status_outgoing").getString())
                        .setAvatarSize(18)
                        .setRowHeight(28)
                        .setTextScale(0.85f);
                row.setOnSecondaryClick((pointerX, pointerY) -> {
                    if (contextMenuHandler != null) {
                        contextMenuHandler.open(requestUser, pointerX, pointerY);
                    }
                });
                list.addChild(row);
            }
        }

        return list;
    }
}
