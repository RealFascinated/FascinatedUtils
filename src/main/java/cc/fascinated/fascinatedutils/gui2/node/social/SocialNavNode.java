package cc.fascinated.fascinatedutils.gui2.node.social;

import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.core.SpacerNode;
import cc.fascinated.fascinatedutils.gui2.core.UiState;
import cc.fascinated.fascinatedutils.gui2.node.BadgeNode;
import cc.fascinated.fascinatedutils.gui2.node.DividerNode;
import cc.fascinated.fascinatedutils.gui2.node.RectNode;
import cc.fascinated.fascinatedutils.gui2.node.TextNode;
import cc.fascinated.fascinatedutils.gui2.node.social.channel.ChannelListNode;
import cc.fascinated.fascinatedutils.gui2.node.social.player.PlayerContextMenuHandler;
import cc.fascinated.fascinatedutils.gui2.theme.UiTheme;
import cc.fascinated.fascinatedutils.gui2.theme.UiThemeRepository;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class SocialNavNode extends PositionedNode<SocialNavNode> {

    private static final int ITEM_HEIGHT = 36;
    private static final int SEPARATOR_HEIGHT = 1;
    private static final int LIST_PAD = 6;
    private static final int SEPARATOR_INSET = 4;

    public SocialNavNode(boolean friendsActive, String selectedChannelId, int incomingBadgeCount,
                         Runnable onFriendsSelected, Consumer<String> onChannelSelected,
                         PlayerContextMenuHandler contextMenuHandler,
                         UiState<Integer> channelListScrollState) {
        full();
        columnGap(0);

        FriendsNavItemNode friendsItem = new FriendsNavItemNode(friendsActive, incomingBadgeCount,
                onFriendsSelected == null ? () -> {} : onFriendsSelected);
        friendsItem.fullWidth().height(ITEM_HEIGHT);
        addChild(friendsItem);

        DividerNode separator = new DividerNode();
        separator.left(SEPARATOR_INSET).right(SEPARATOR_INSET).height(SEPARATOR_HEIGHT);
        addChild(separator);

        addChild(new PositionedNode().fullWidth().height(4));

        SocialSectionLabelNode dmLabel = new SocialSectionLabelNode(Component.translatable("alumite.social.dm.title").getString());
        dmLabel.left(8).right(0).height(14);
        addChild(dmLabel);

        PositionedNode channelListPadded = new PositionedNode()
                .left(LIST_PAD).right(LIST_PAD)
                .top(4).bottom(LIST_PAD);
        ChannelListNode channels = new ChannelListNode(selectedChannelId, onChannelSelected, contextMenuHandler);
        channels.bindScrollState(channelListScrollState);
        channels.setNodeId("social.channel-list");
        channelListPadded.addChild(channels);

        SpacerNode channelListFill = new SpacerNode();
        channelListFill.addChild(channelListPadded);
        addChild(channelListFill);
    }

    /**
     * The "Friends" nav button rendered at the top of the left nav.
     *
     * <p>Renders a rounded selection pill when active or hovered, and a badge for pending
     * incoming friend requests.</p>
     */
    private static class FriendsNavItemNode extends PositionedNode {

        private static final int PAD_H = 6;
        private static final int LABEL_PAD = 8;

        private final boolean active;
        private final Runnable onPress;
        private final RectNode bg;
        private final TextNode friendsLabel;
        private final BadgeNode badge;
        private boolean hovered;

        FriendsNavItemNode(boolean active, int badgeCount, Runnable onPress) {
            this.active = active;
            this.onPress = onPress;

            bg = new RectNode();
            bg.left(PAD_H).right(PAD_H).top(PAD_H).bottom(PAD_H);
            bg.setCornerRadius(4);
            bg.setFillSupplier(() -> {
                UiTheme theme = UiThemeRepository.get();
                return active ? theme.rowSelectedFill() : hovered ? theme.rowHoverFill() : 0;
            });
            addChild(bg);

            friendsLabel = new TextNode(() -> Component.translatable("alumite.social.friends_heading").getString())
                    .setColorResolver(theme -> active ? theme.textPrimary() : theme.textMuted())
                    .setTextAlign(0f, 0.5f);
            friendsLabel.left(PAD_H + LABEL_PAD).right(0).fullHeight();
            addChild(friendsLabel);

            badge = new BadgeNode(() -> badgeCount).setCap(99);
            badge.right(PAD_H).alignY(0.5f);
            addChild(badge);
        }

        @Override
        public boolean blocksHitWhenEmpty() {
            return true;
        }

        @Override
        public boolean onPointerEnter(float pointerX, float pointerY) {
            hovered = true;
            return false;
        }

        @Override
        public boolean onPointerLeave(float pointerX, float pointerY) {
            hovered = false;
            return false;
        }

        @Override
        public boolean onClick(float pointerX, float pointerY, int button) {
            if (button != 0) {
                return false;
            }
            onPress.run();
            return true;
        }
    }
}
