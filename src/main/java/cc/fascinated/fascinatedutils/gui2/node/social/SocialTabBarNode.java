package cc.fascinated.fascinatedutils.gui2.node.social;

import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;

/**
 * Two-segment tab bar for switching between Chat and Friends views.
 *
 * <p>Renders a pill-style active segment highlight and an accent underline on the active tab.
 * An optional badge count is drawn on the Friends tab for pending incoming friend requests.
 */
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
    private boolean chatHovered;
    private boolean friendsHovered;
    private String chatLabel = "Chats";
    private String friendsLabel = "Friends";

    public SocialTabBarNode() {
        height(30).fullWidth();
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
    public boolean onPointerMove(float pointerX, float pointerY) {
        float midX = bounds().positionX() + bounds().width() / 2f;
        chatHovered = pointerX < midX;
        friendsHovered = pointerX >= midX;
        return false;
    }

    @Override
    public boolean onPointerLeave(float pointerX, float pointerY) {
        chatHovered = false;
        friendsHovered = false;
        return false;
    }

    @Override
    public boolean onClick(float pointerX, float pointerY, int button) {
        if (button != 0) {
            return false;
        }
        float midX = bounds().positionX() + bounds().width() / 2f;
        if (pointerX < midX) {
            onChatSelected.run();
        } else {
            onFriendsSelected.run();
        }
        return true;
    }

    @Override
    protected void renderSelf(RenderFrame renderFrame, float deltaSeconds) {
        int posX = bounds().positionX();
        int posY = bounds().positionY();
        int width = bounds().width();
        int height = bounds().height();
        int halfW = width / 2;

        boolean chatActive = activeTab == Tab.CHAT;
        int chatFill = chatActive ? renderFrame.theme().tabActiveFill() : chatHovered ? renderFrame.theme().tabHoverFill() : 0;
        int friendsFill = !chatActive ? renderFrame.theme().tabActiveFill() : friendsHovered ? renderFrame.theme().tabHoverFill() : 0;

        if (chatFill != 0) {
            renderFrame.drawRect(posX, posY, halfW, height, chatFill);
        }
        if (friendsFill != 0) {
            renderFrame.drawRect(posX + halfW, posY, halfW, height, friendsFill);
        }

        int accent = renderFrame.theme().accent();
        int underlineY = posY + height - UNDERLINE_H;
        if (chatActive) {
            renderFrame.drawRoundedRect(posX + UNDERLINE_INSET, underlineY, halfW - UNDERLINE_INSET * 2, UNDERLINE_H, UNDERLINE_H / 2, accent);
        } else {
            renderFrame.drawRoundedRect(posX + halfW + UNDERLINE_INSET, underlineY, halfW - UNDERLINE_INSET * 2, UNDERLINE_H, UNDERLINE_H / 2, accent);
        }

        int chatColor = chatActive ? renderFrame.theme().textPrimary() : renderFrame.theme().textMuted();
        int friendsColor = chatActive ? renderFrame.theme().textMuted() : renderFrame.theme().textPrimary();
        int textY = posY + (height - renderFrame.fontHeight()) / 2;

        int chatLabelWidth = renderFrame.measureTextWidth(chatLabel, false);
        renderFrame.drawText(chatLabel, posX + (halfW - chatLabelWidth) / 2, textY, chatColor, false, false);

        int friendsLabelWidth = renderFrame.measureTextWidth(friendsLabel, false);
        renderFrame.drawText(friendsLabel, posX + halfW + (halfW - friendsLabelWidth) / 2, textY, friendsColor, false, false);

        if (incomingRequestBadgeCount > 0) {
            String badgeText = incomingRequestBadgeCount > 9 ? "9+" : String.valueOf(incomingRequestBadgeCount);
            int badgeW = renderFrame.measureTextWidth(badgeText, true) + 4;
            int badgeH = renderFrame.fontHeight() + 2;
            int badgeX = posX + width - badgeW - 4;
            int badgeY = posY + 2;
            renderFrame.drawRoundedRect(badgeX, badgeY, badgeW, badgeH, badgeH / 2, renderFrame.theme().danger());
            renderFrame.drawText(badgeText, badgeX + 2, badgeY + 1, renderFrame.theme().onDanger(), false, true);
        }
    }
}
