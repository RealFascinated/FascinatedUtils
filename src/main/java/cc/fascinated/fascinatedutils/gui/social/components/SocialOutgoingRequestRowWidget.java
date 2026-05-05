package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.api.friend.PendingFriendRequest;
import cc.fascinated.fascinatedutils.gui.AvatarTextureCache;
import cc.fascinated.fascinatedutils.gui.core.Align;
import cc.fascinated.fascinatedutils.gui.core.PointerHitKind;
import cc.fascinated.fascinatedutils.gui.core.TextOverflow;
import cc.fascinated.fascinatedutils.gui.core.UiFrameContext;
import cc.fascinated.fascinatedutils.gui.core.UiPointerCursor;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FLabelWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import net.minecraft.resources.Identifier;

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
        FLabelWidget nameLabel = new FLabelWidget();
        nameLabel.setText(request.user().minecraftName());
        nameLabel.setColorArgb(FascinatedGuiTheme.INSTANCE.textMuted());
        nameLabel.setOverflow(TextOverflow.ELLIPSIS);
        nameLabel.setAlignY(Align.CENTER);

        return new FWidget() {
            {
                addChild(nameLabel);
            }

            @Override
            public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
                return rowHeight;
            }

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, width, lh);
                float nameX = lx + 4f + AVATAR_SIZE + 6f;
                float nameMaxX = lx + width - BTN_W - 8f;
                nameLabel.layout(measure, nameX, ly, nameMaxX - nameX, lh);
            }

            @Override
            protected void renderSelf(GuiRenderer graphics, UiFrameContext frame, float deltaSeconds) {
                float mouseX = frame.pointerX();
                float mouseY = frame.pointerY();
                boolean rowHovered = containsPoint(mouseX, mouseY);
                graphics.fillRoundedRect(x(), y(), w(), h(), UITheme.CORNER_RADIUS_SM, rowHovered ? 0x22FFFFFF : UITheme.COLOR_BACKGROUND, RectCornerRoundMask.ALL);

                float avatarX = x() + 4f;
                float avatarY = y() + (h() - AVATAR_SIZE) / 2f;
                String receiverUuid = request.user().minecraftUuid();
                Identifier avatarTexture = receiverUuid != null ? AvatarTextureCache.INSTANCE.get(receiverUuid, () -> {}) : null;
                if (avatarTexture != null) {
                    graphics.fillRoundedRect(avatarX, avatarY, AVATAR_SIZE, AVATAR_SIZE, 4f, 0xFF000000, RectCornerRoundMask.ALL);
                    graphics.drawTexture(avatarTexture, avatarX, avatarY, AVATAR_SIZE, AVATAR_SIZE, 0xFFFFFFFF);
                } else {
                    int badgeColor = BADGE_COLORS[Math.abs(request.user().minecraftName().hashCode()) % BADGE_COLORS.length];
                    graphics.fillRoundedRect(avatarX, avatarY, AVATAR_SIZE, AVATAR_SIZE, 4f, badgeColor, RectCornerRoundMask.ALL);
                    String initial = request.user().minecraftName().isEmpty() ? "?" : String.valueOf(Character.toUpperCase(request.user().minecraftName().charAt(0)));
                    graphics.drawCenteredText(initial, avatarX + AVATAR_SIZE / 2f, avatarY + (AVATAR_SIZE - graphics.getFontCapHeight()) / 2f, 0xFFFFFFFF, false, true);
                }

                float cancelBtnX = x() + w() - BTN_W - 4f;
                float cancelBtnY = y() + (h() - BTN_H) / 2f;
                boolean btnHovered = mouseX >= cancelBtnX && mouseX < cancelBtnX + BTN_W && mouseY >= cancelBtnY && mouseY < cancelBtnY + BTN_H;
                graphics.fillRoundedRect(cancelBtnX, cancelBtnY, BTN_W, BTN_H, 4f, btnHovered ? 0xAA5C1F1F : 0x22FFFFFF, RectCornerRoundMask.ALL);
                int cancelTextColor = btnHovered ? 0xFFFF5555 : FascinatedGuiTheme.INSTANCE.textMuted();
                graphics.drawCenteredText("\u2715", cancelBtnX + BTN_W / 2f, cancelBtnY + (BTN_H - graphics.getFontCapHeight()) / 2f, cancelTextColor, false, false);
            }

            @Override
            public PointerHitKind pointerHitKind() {
                return PointerHitKind.TARGET;
            }

            @Override
            public UiPointerCursor pointerCursor(float pointerX, float pointerY) {
                return UiPointerCursor.HAND;
            }

            @Override
            public boolean mouseDown(float pointerX, float pointerY, int button) {
                if (button != 0) {
                    return false;
                }
                float cancelBtnX = x() + w() - BTN_W - 4f;
                float cancelBtnY = y() + (h() - BTN_H) / 2f;
                if (pointerX >= cancelBtnX && pointerX < cancelBtnX + BTN_W && pointerY >= cancelBtnY && pointerY < cancelBtnY + BTN_H) {
                    onCancelRequested.run();
                    return true;
                }
                return false;
            }
        };
    }
}
