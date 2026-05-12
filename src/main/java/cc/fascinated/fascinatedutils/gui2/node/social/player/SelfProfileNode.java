package cc.fascinated.fascinatedutils.gui2.node.social.player;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.user.Activity;
import cc.fascinated.fascinatedutils.api.user.SelfUser;
import cc.fascinated.fascinatedutils.api.user.User;
import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.node.CardNode;
import cc.fascinated.fascinatedutils.gui2.node.RectNode;
import cc.fascinated.fascinatedutils.gui2.node.TextNode;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;
import cc.fascinated.fascinatedutils.gui2.theme.UiTheme;

import java.util.function.BiConsumer;

public class SelfProfileNode extends PositionedNode<SelfProfileNode> {

    private static final int CARD_INSET_H = 8;
    private static final int CARD_INSET_V = 6;
    private static final int CARD_CORNER = 6;
    private static final int CARD_PAD = 8;
    private static final int AVATAR_SIZE = 28;
    private static final int AVATAR_TEXT_GAP = 8;
    private static final int HOVER_TINT = 0x15FFFFFF;

    private final CardNode card;
    private final RectNode hoverOverlay;
    private final PlayerAvatarNode avatar;
    private final TextNode nameText;
    private final TextNode activityText;

    private BiConsumer<Float, Float> onStatusClick;
    private boolean hovered;

    public SelfProfileNode() {
        height(52).fullWidth();

        card = new CardNode();
        card.left(CARD_INSET_H).right(CARD_INSET_H).top(CARD_INSET_V).bottom(CARD_INSET_V);
        addChild(card);

        hoverOverlay = new RectNode();
        hoverOverlay.left(CARD_INSET_H).right(CARD_INSET_H).top(CARD_INSET_V).bottom(CARD_INSET_V);
        hoverOverlay.setCornerRadius(CARD_CORNER);
        hoverOverlay.setFillSupplier(() -> hovered && onStatusClick != null ? HOVER_TINT : 0);
        addChild(hoverOverlay);

        avatar = new PlayerAvatarNode(AVATAR_SIZE,
                () -> selfUser() != null ? selfUser().user() : null);
        avatar.left(CARD_INSET_H + CARD_PAD).alignY(0.5f);
        addChild(avatar);

        nameText = new TextNode(() -> {
            SelfUser self = selfUser();
            User user = self != null ? self.user() : null;
            return user != null && user.minecraftName() != null ? user.minecraftName() : "";
        }).setColorResolver(UiTheme::textPrimary).setTextAlign(0f, 0.5f);
        addChild(nameText);

        activityText = new TextNode(() -> {
            SelfUser self = selfUser();
            Activity activity = self != null ? self.activity() : null;
            return activity != null ? activity.label() : "";
        }).setColorResolver(UiTheme::textMuted).setTextAlign(0f, 0.5f);
        addChild(activityText);
    }

    private SelfUser selfUser() {
        return Alumite.INSTANCE != null ? Alumite.INSTANCE.users().selfUser() : null;
    }

    public SelfProfileNode setOnStatusClick(BiConsumer<Float, Float> onStatusClick) {
        this.onStatusClick = onStatusClick;
        return this;
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
        super.layout(renderFrame, parentX, parentY, parentWidth, parentHeight);

        int cardX = bounds().positionX() + CARD_INSET_H;
        int cardY = bounds().positionY() + CARD_INSET_V;
        int cardW = bounds().width() - CARD_INSET_H * 2;
        int cardH = bounds().height() - CARD_INSET_V * 2;

        int textX = cardX + CARD_PAD + AVATAR_SIZE + AVATAR_TEXT_GAP;
        int textW = Math.max(0, cardW - CARD_PAD - AVATAR_SIZE - AVATAR_TEXT_GAP - CARD_PAD);

        Activity activity = selfUser() != null ? selfUser().activity() : null;
        activityText.setVisible(activity != null);

        int lineH = renderFrame.fontHeight();
        if (activity != null) {
            int blockH = lineH * 2 + 3;
            int nameY = cardY + (cardH - blockH) / 2;
            nameText.layout(renderFrame, textX, nameY, textW, lineH);
            activityText.layout(renderFrame, textX, nameY + lineH + 3, textW, lineH);
        } else {
            nameText.layout(renderFrame, textX, cardY, textW, cardH);
        }
    }
}
