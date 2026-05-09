package cc.fascinated.fascinatedutils.gui2.node.social.player;

import cc.fascinated.fascinatedutils.api.user.User;
import cc.fascinated.fascinatedutils.api.user.UserStatus;
import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.core.UiNode;
import cc.fascinated.fascinatedutils.gui2.node.RectNode;
import cc.fascinated.fascinatedutils.gui2.node.TextNode;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;
import cc.fascinated.fascinatedutils.gui2.theme.UiTheme;
import cc.fascinated.fascinatedutils.gui2.theme.UiThemeRepository;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class PlayerRowNode extends PositionedNode {

    private static final int AVATAR_SIZE = 24;
    private static final int AVATAR_PADDING = 6;
    private static final int ROW_HEIGHT = 36;
    private static final int BG_CORNER_RADIUS = 4;

    private final RectNode bg;
    private final PlayerAvatarNode avatar;
    private final TextNode nameText;
    private final TextNode subtextNode;
    private final Supplier<String> subtextSupplier;
    private int avatarSize = AVATAR_SIZE;
    private float textScale = 1.0f;
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
        Supplier<String> displayNameSupplier = () -> {
            User resolved = user.get();
            return resolved != null ? resolved.minecraftName() : "";
        };
        this.subtextSupplier = subtextSupplier;

        avatar = new PlayerAvatarNode(AVATAR_SIZE, () -> {
            User resolved = user.get();
            return resolved != null ? resolved.minecraftUuid() : null;
        }, displayNameSupplier, () -> {
            User resolved = user.get();
            UserStatus status = resolved != null ? resolved.userStatus() : null;
            return status != null ? status.color() : UserStatus.OFFLINE.color();
        });

        bg = new RectNode();
        bg.full();
        bg.setCornerRadius(BG_CORNER_RADIUS);
        bg.setFillSupplier(() -> {
            UiTheme theme = UiThemeRepository.get();
            return selected ? theme.rowSelectedFill() : hovered ? theme.rowHoverFill() : 0;
        });

        nameText = new TextNode(displayNameSupplier)
                .setColorResolver(UiTheme::textPrimary)
                .setTextAlign(0f, 0.5f);

        subtextNode = new TextNode(subtextSupplier != null ? subtextSupplier : () -> "")
                .setColorResolver(UiTheme::textMuted)
                .setTextAlign(0f, 0.5f);

        height(ROW_HEIGHT).fullWidth();
        addChild(bg);
        addChild(avatar);
        addChild(nameText);
        addChild(subtextNode);
    }

    public PlayerRowNode setSelected(boolean selected) {
        this.selected = selected;
        return this;
    }

    public PlayerRowNode setAvatarSize(int avatarSize) {
        this.avatarSize = avatarSize;
        avatar.size(avatarSize);
        return this;
    }

    public PlayerRowNode setRowHeight(int rowHeight) {
        height(rowHeight);
        return this;
    }

    public PlayerRowNode setTextScale(float textScale) {
        this.textScale = textScale;
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

        int avatarY = posY + (height - avatarSize) / 2;
        avatar.layout(renderFrame, posX + AVATAR_PADDING, avatarY, avatarSize, avatarSize);

        int trailingReserved = trailingAction != null ? AVATAR_PADDING + trailingActionSize : AVATAR_PADDING;
        int textX = posX + AVATAR_PADDING + avatarSize + 8;
        int textW = Math.max(0, width - textX + posX - trailingReserved);

        nameText.setScale(textScale);
        subtextNode.setScale(textScale);

        String subtext = subtextSupplier != null ? subtextSupplier.get() : null;
        boolean hasSubtext = subtext != null && !subtext.isBlank();
        subtextNode.setVisible(hasSubtext);

        int lineH = Math.round(renderFrame.fontHeight() * textScale);
        if (hasSubtext) {
            int blockH = lineH * 2 + 3;
            int nameY = posY + (height - blockH) / 2;
            nameText.layout(renderFrame, textX, nameY, textW, lineH);
            subtextNode.layout(renderFrame, textX, nameY + lineH + 3, textW, lineH);
        } else {
            nameText.layout(renderFrame, textX, posY, textW, height);
        }

        if (trailingAction != null) {
            int actionY = posY + (height - trailingActionSize) / 2;
            trailingAction.layout(renderFrame, posX + width - AVATAR_PADDING - trailingActionSize, actionY, trailingActionSize, trailingActionSize);
            trailingAction.setVisible(hovered);
        }
    }
}

