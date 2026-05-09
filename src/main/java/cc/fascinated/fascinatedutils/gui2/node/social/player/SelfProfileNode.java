package cc.fascinated.fascinatedutils.gui2.node.social.player;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.user.Activity;
import cc.fascinated.fascinatedutils.api.user.SelfUser;
import cc.fascinated.fascinatedutils.api.user.User;
import cc.fascinated.fascinatedutils.api.user.UserStatus;
import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.node.DividerNode;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;

import java.util.function.BiConsumer;

/**
 * Bottom-left footer strip showing the local player's avatar, display name, and current activity.
 *
 * <p>Reads directly from {@link Alumite#INSTANCE} each frame so it always reflects live data
 * without requiring any explicit state wiring from the parent screen.
 */
public class SelfProfileNode extends PositionedNode {

    private static final int AVATAR_SIZE = 24;
    private static final int PADDING = 6;
    private static final int HOVER_TINT = 0x15FFFFFF;

    private final DividerNode divider;
    private final PlayerAvatarNode avatar;
    private BiConsumer<Float, Float> onStatusClick;
    private boolean hovered;

    public SelfProfileNode() {
        height(36).fullWidth();
        divider = new DividerNode();
        divider.top(0).fullWidth();
        addChild(divider);

        avatar = new PlayerAvatarNode(AVATAR_SIZE,
                () -> selfUser() != null && selfUser().user() != null ? selfUser().user().minecraftUuid() : null,
                () -> selfUser() != null && selfUser().user() != null ? selfUser().user().minecraftName() : null,
                () -> {
                    if (selfUser() == null) {
                        return UserStatus.OFFLINE.color();
                    }
                    UserStatus status = selfUser().preferredUserStatus();
                    return status != null ? status.color() : UserStatus.OFFLINE.color();
                });
        addChild(avatar);
    }

    private SelfUser selfUser() {
        return Alumite.INSTANCE != null ? Alumite.INSTANCE.users().selfUser() : null;
    }

    public void setOnStatusClick(BiConsumer<Float, Float> onStatusClick) {
        this.onStatusClick = onStatusClick;
    }

    @Override
    public boolean blocksHitWhenEmpty() {
        return onStatusClick != null;
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
        if (button != 0 || onStatusClick == null) {
            return false;
        }
        onStatusClick.accept(pointerX, pointerY);
        return true;
    }

    @Override
    public void layout(RenderFrame renderFrame, int parentX, int parentY, int parentWidth, int parentHeight) {
        divider.height(renderFrame.theme().separatorThickness());
        super.layout(renderFrame, parentX, parentY, parentWidth, parentHeight);
        int posX = bounds().positionX();
        int posY = bounds().positionY();
        int height = bounds().height();
        avatar.layout(renderFrame, posX + PADDING, posY + (height - AVATAR_SIZE) / 2, AVATAR_SIZE, AVATAR_SIZE);
    }

    @Override
    protected void renderSelf(RenderFrame renderFrame, float deltaSeconds) {
        int posX = bounds().positionX();
        int posY = bounds().positionY();
        int width = bounds().width();
        int height = bounds().height();

        if (hovered && onStatusClick != null) {
            renderFrame.drawRect(posX, posY, width, height, HOVER_TINT);
        }

        SelfUser self = selfUser();
        User user = self != null ? self.user() : null;
        String name = user != null && user.minecraftName() != null ? user.minecraftName() : "";

        int textX = posX + PADDING + AVATAR_SIZE + 8;
        Activity activity = self != null ? self.activity() : null;
        int primaryColor = renderFrame.theme().textPrimary();
        int mutedColor = renderFrame.theme().textMuted();

        if (activity != null) {
            int lineH = renderFrame.fontHeight();
            int blockH = lineH * 2 + 3;
            int nameY = posY + (height - blockH) / 2;
            renderFrame.drawText(name, textX, nameY, primaryColor, false, false);
            renderFrame.drawText(activity.label(), textX, nameY + lineH + 3, mutedColor, false, false);
        } else {
            renderFrame.drawText(name, textX, posY + (height - renderFrame.fontHeight()) / 2, primaryColor, false, false);
        }
    }
}
