package cc.fascinated.fascinatedutils.gui2.node.social;

import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.node.BadgeNode;
import cc.fascinated.fascinatedutils.gui2.node.RectNode;
import cc.fascinated.fascinatedutils.gui2.node.TextNode;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;
import cc.fascinated.fascinatedutils.gui2.theme.UiTheme;
import cc.fascinated.fascinatedutils.gui2.theme.UiThemeRepository;
import net.minecraft.network.chat.Component;

public class SocialTabBarNode extends PositionedNode {

    public enum Tab {
        CHAT, FRIENDS
    }

    private static final int UNDERLINE_H = 2;
    private static final int UNDERLINE_INSET = 10;

    private Tab activeTab = Tab.CHAT;
    private Runnable onChatSelected = () -> {};
    private Runnable onFriendsSelected = () -> {};
    private int incomingRequestBadgeCount;
    private String chatLabel = Component.translatable("alumite.social.tab_chats").getString();
    private String friendsLabel = Component.translatable("alumite.social.friends_heading").getString();
    private final RectNode chatBg;
    private final RectNode friendsBg;
    private final RectNode underline;
    private final TextNode chatTab;
    private final TextNode friendsTab;
    private final BadgeNode badge;
    private final TabSegmentNode chatSegment;
    private final TabSegmentNode friendsSegment;

    public SocialTabBarNode() {
        height(30).fullWidth();

        chatSegment = new TabSegmentNode(() -> onChatSelected.run());
        friendsSegment = new TabSegmentNode(() -> onFriendsSelected.run());

        chatBg = new RectNode();
        chatBg.setFillSupplier(() -> {
            UiTheme theme = UiThemeRepository.get();
            return activeTab == Tab.CHAT ? theme.tabActiveFill() : chatSegment.isHovered() ? theme.tabHoverFill() : 0;
        });
        chatBg.left(0).widthRel(0.5f).fullHeight();
        addChild(chatBg);

        friendsBg = new RectNode();
        friendsBg.setFillSupplier(() -> {
            UiTheme theme = UiThemeRepository.get();
            return activeTab != Tab.CHAT ? theme.tabActiveFill() : friendsSegment.isHovered() ? theme.tabHoverFill() : 0;
        });
        friendsBg.leftRel(0.5f).right(0).fullHeight();
        addChild(friendsBg);

        underline = new RectNode();
        underline.setFillResolver(UiTheme::accent);
        underline.setCornerRadius(UNDERLINE_H / 2);
        underline.fullWidth().bottom(0).height(UNDERLINE_H);
        addChild(underline);

        chatTab = new TextNode(() -> chatLabel).setTextAlign(0.5f, 0.5f);
        chatTab.setColorResolver(theme -> activeTab == Tab.CHAT ? theme.textPrimary() : theme.textMuted());
        chatTab.left(0).widthRel(0.5f).fullHeight();
        addChild(chatTab);

        friendsTab = new TextNode(() -> friendsLabel).setTextAlign(0.5f, 0.5f);
        friendsTab.setColorResolver(theme -> activeTab != Tab.CHAT ? theme.textPrimary() : theme.textMuted());
        friendsTab.leftRel(0.5f).right(0).fullHeight();
        addChild(friendsTab);

        badge = new BadgeNode(() -> incomingRequestBadgeCount);
        badge.right(4).top(2);
        addChild(badge);

        // Segments are added last so they sit on top in hit-test order.
        chatSegment.left(0).widthRel(0.5f).fullHeight();
        friendsSegment.leftRel(0.5f).right(0).fullHeight();
        addChild(chatSegment);
        addChild(friendsSegment);
    }

    public SocialTabBarNode setActiveTab(Tab activeTab) {
        this.activeTab = activeTab;
        return this;
    }

    public SocialTabBarNode setOnChatSelected(Runnable onChatSelected) {
        this.onChatSelected = onChatSelected == null ? () -> {} : onChatSelected;
        return this;
    }

    public SocialTabBarNode setOnFriendsSelected(Runnable onFriendsSelected) {
        this.onFriendsSelected = onFriendsSelected == null ? () -> {} : onFriendsSelected;
        return this;
    }

    public SocialTabBarNode setIncomingRequestBadgeCount(int count) {
        this.incomingRequestBadgeCount = count;
        return this;
    }

    public SocialTabBarNode setChatLabel(String chatLabel) {
        this.chatLabel = chatLabel;
        return this;
    }

    public SocialTabBarNode setFriendsLabel(String friendsLabel) {
        this.friendsLabel = friendsLabel;
        return this;
    }

    @Override
    public boolean blocksHitWhenEmpty() {
        return true;
    }

    @Override
    public void layout(RenderFrame renderFrame, int parentX, int parentY, int parentWidth, int parentHeight) {
        super.layout(renderFrame, parentX, parentY, parentWidth, parentHeight);
        boolean chatActive = activeTab == Tab.CHAT;
        int halfW = bounds().width() / 2;
        int underlineX = bounds().positionX() + (chatActive ? UNDERLINE_INSET : halfW + UNDERLINE_INSET);
        underline.layout(renderFrame, underlineX, bounds().positionY() + bounds().height() - UNDERLINE_H, halfW - UNDERLINE_INSET * 2, UNDERLINE_H);
    }

    private static class TabSegmentNode extends PositionedNode {

        private final Runnable onPress;
        private boolean hovered;

        TabSegmentNode(Runnable onPress) {
            this.onPress = onPress;
        }

        boolean isHovered() {
            return hovered;
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
