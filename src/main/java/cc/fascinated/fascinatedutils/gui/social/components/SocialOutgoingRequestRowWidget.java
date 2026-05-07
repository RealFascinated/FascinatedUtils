package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.api.friend.PendingFriendRequest;
import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.gui.core.*;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FAvatarWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FIconButtonWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FLabelWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;

/**
 * A row widget for an outgoing friend request, with a cancel button.
 * Calls {@code onCancelRequested} when the user clicks cancel; the caller is
 * responsible for showing a confirmation dialog.
 */
public class SocialOutgoingRequestRowWidget {
    private static final float AVATAR_SIZE = 32f;
    private static final float BTN_W = 20f;
    private static final float BTN_H = 20f;
    private static final int[] BADGE_COLORS = {0xFF6B5B95, 0xFF88B04B, 0xFF955251, 0xFF009B77, 0xFF45B8AC, 0xFF5B5EA6, 0xFFB565A7, 0xFFDD4132};

    public static FWidget build(PendingFriendRequest request, float width, float rowHeight, Runnable onCancelRequested) {
        int badgeColor = BADGE_COLORS[Math.abs(request.user().minecraftName().hashCode()) % BADGE_COLORS.length];

        FLabelWidget nameLabel = new FLabelWidget();
        nameLabel.setText(request.user().minecraftName());
        nameLabel.setColorArgb(FascinatedGuiTheme.INSTANCE.textMuted());
        nameLabel.setOverflow(TextOverflow.ELLIPSIS);
        nameLabel.setAlignY(Align.CENTER);

        FAvatarWidget avatar = new FAvatarWidget(AVATAR_SIZE, 4f,
                () -> request.user().minecraftUuid(),
                () -> request.user().minecraftName());
        avatar.setFallbackColor(badgeColor);

        FIconButtonWidget cancelBtn = new FIconButtonWidget(BTN_W, 3f, 4f, ModUiTextures.CLOSE::getId, true) {
            @Override
            protected int resolveButtonFillArgb(boolean hovered) {
                return hovered ? 0xAA5C1F1F : 0x22FFFFFF;
            }

            @Override
            protected int resolveButtonBorderArgb(boolean hovered) {
                return resolveButtonFillArgb(hovered);
            }

            @Override
            protected int resolveContentTintArgb(boolean hovered) {
                return hovered ? 0xFFFF5555 : FascinatedGuiTheme.INSTANCE.textMuted();
            }
        };
        cancelBtn.setDrawBorder(false);
        cancelBtn.setOnClick(onCancelRequested);

        return new FWidget() {
            {
                addChild(avatar);
                addChild(nameLabel);
                addChild(cancelBtn);
            }

            @Override
            public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
                return rowHeight;
            }

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, width, lh);
                float avatarY = ly + (lh - AVATAR_SIZE) / 2f;
                avatar.layout(measure, lx + 4f, avatarY, AVATAR_SIZE, AVATAR_SIZE);
                float cancelBtnX = lx + width - BTN_W - 4f;
                cancelBtn.layout(measure, cancelBtnX, ly + (lh - BTN_H) / 2f, BTN_W, BTN_H);
                float nameX = lx + 4f + AVATAR_SIZE + 6f;
                nameLabel.layout(measure, nameX, ly, cancelBtnX - nameX - 4f, lh);
            }

            @Override
            protected void renderSelf(GuiRenderer graphics, UiFrameContext frame, float deltaSeconds) {
                boolean rowHovered = containsPoint(frame.pointerX(), frame.pointerY());
                graphics.fillRoundedRect(x(), y(), w(), h(), UITheme.CORNER_RADIUS_SM,
                        rowHovered ? 0x22FFFFFF : UITheme.COLOR_BACKGROUND, RectCornerRoundMask.ALL);
            }

            @Override
            public PointerHitKind pointerHitKind() {
                return PointerHitKind.TARGET;
            }

            @Override
            public UiPointerCursor pointerCursor(float pointerX, float pointerY) {
                return UiPointerCursor.HAND;
            }
        };
    }
}
