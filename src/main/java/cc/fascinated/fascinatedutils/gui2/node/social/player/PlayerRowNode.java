package cc.fascinated.fascinatedutils.gui2.node.social.player;

import cc.fascinated.fascinatedutils.api.user.User;
import cc.fascinated.fascinatedutils.api.user.UserStatus;
import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.core.UiNode;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * A fixed-height list row showing a player avatar, display name, and optional status/activity text.
 *
 * <p>An optional action node (e.g. a remove button) is placed on the trailing edge and made
 * visible only on hover. Callers provide the action node and control its dimensions.
 */
public class PlayerRowNode extends PositionedNode {

    private static final int AVATAR_SIZE = 24;
    private static final int AVATAR_PADDING = 6;
    private static final int ROW_HEIGHT = 36;

    private final PlayerAvatarNode avatar;
    private final Supplier<String> displayNameSupplier;
    private final Supplier<String> subtextSupplier;
    private UiNode trailingAction;
    private int trailingActionSize;
    private Runnable onPrimaryClick = () -> {};
    private BiConsumer<Float, Float> onSecondaryClick;

    private boolean hovered;
    private boolean selected;

    /**
     * Creates a player row node.
     *
     * @param user            supplier of the User whose avatar and name to display; may return null
     * @param subtextSupplier supplier of a secondary line of text shown below the name; null = no subtext
     */
    public PlayerRowNode(Supplier<User> user, Supplier<String> subtextSupplier) {
        this.displayNameSupplier = () -> {
            User resolved = user.get();
            return resolved != null ? resolved.minecraftName() : "";
        };
        this.subtextSupplier = subtextSupplier;
        this.avatar = new PlayerAvatarNode(AVATAR_SIZE, () -> {
            User resolved = user.get();
            return resolved != null ? resolved.minecraftUuid() : null;
        }, displayNameSupplier, () -> {
            User resolved = user.get();
            UserStatus status = resolved != null ? resolved.userStatus() : null;
            return status != null ? status.color() : UserStatus.OFFLINE.color();
        });
        height(ROW_HEIGHT).fullWidth();
        addChild(avatar);
    }

    public PlayerRowNode setSelected(boolean selected) {
        this.selected = selected;
        return this;
    }

    public PlayerRowNode setOnPrimaryClick(Runnable onPrimaryClick) {
        this.onPrimaryClick = onPrimaryClick == null ? () -> {} : onPrimaryClick;
        return this;
    }

    public PlayerRowNode setOnSecondaryClick(BiConsumer<Float, Float> onSecondaryClick) {
        this.onSecondaryClick = onSecondaryClick;
        return this;
    }

    /**
     * Sets a trailing action node (e.g. an icon button) shown only while the row is hovered.
     *
     * @param actionNode the node to place on the trailing edge
     * @param actionSize width and height of the square action slot
     */
    public PlayerRowNode setTrailingAction(UiNode actionNode, float actionSize) {
        if (trailingAction != null) {
            removeChild(trailingAction);
        }
        trailingAction = actionNode;
        trailingActionSize = Math.round(actionSize);
        addChild(trailingAction);
        return this;
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
        if (button == 0) {
            onPrimaryClick.run();
            return true;
        }
        if (button == 1 && onSecondaryClick != null) {
            onSecondaryClick.accept(pointerX, pointerY);
            return true;
        }
        return false;
    }

    @Override
    public void layout(RenderFrame renderFrame, int parentX, int parentY, int parentWidth, int parentHeight) {
        super.layout(renderFrame, parentX, parentY, parentWidth, parentHeight);

        int posX = bounds().positionX();
        int posY = bounds().positionY();
        int width = bounds().width();
        int height = bounds().height();

        int avatarY = posY + (height - AVATAR_SIZE) / 2;
        avatar.layout(renderFrame, posX + AVATAR_PADDING, avatarY, AVATAR_SIZE, AVATAR_SIZE);

        if (trailingAction != null) {
            int actionY = posY + (height - trailingActionSize) / 2;
            trailingAction.layout(renderFrame, posX + width - AVATAR_PADDING - trailingActionSize, actionY, trailingActionSize, trailingActionSize);
            trailingAction.setVisible(hovered);
        }
    }

    @Override
    protected void renderSelf(RenderFrame renderFrame, float deltaSeconds) {
        int posX = bounds().positionX();
        int posY = bounds().positionY();
        int width = bounds().width();
        int height = bounds().height();

        int fill = selected ? renderFrame.theme().rowSelectedFill() : hovered ? renderFrame.theme().rowHoverFill() : 0;
        if (fill != 0) {
            renderFrame.drawRoundedRect(posX, posY, width, height, 4, fill);
        }

        int textX = posX + AVATAR_PADDING + AVATAR_SIZE + 8;
        String name = displayNameSupplier.get();
        String subtext = subtextSupplier != null ? subtextSupplier.get() : null;

        int primaryColor = renderFrame.theme().textPrimary();
        int mutedColor = renderFrame.theme().textMuted();

        if (subtext != null && !subtext.isBlank()) {
            int lineH = renderFrame.fontHeight();
            int blockH = lineH * 2 + 3;
            int nameY = posY + (height - blockH) / 2;
            renderFrame.drawText(name, textX, nameY, primaryColor, false, false);
            renderFrame.drawText(subtext, textX, nameY + lineH + 3, mutedColor, false, false);
        } else {
            int nameY = posY + (height - renderFrame.fontHeight()) / 2;
            renderFrame.drawText(name, textX, nameY, primaryColor, false, false);
        }
    }
}
