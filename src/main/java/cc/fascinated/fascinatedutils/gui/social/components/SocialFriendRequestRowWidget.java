package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.FascinatedUtils;
import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.AlumiteApiException;
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
import cc.fascinated.fascinatedutils.gui.toast.Toast;
import cc.fascinated.fascinatedutils.gui.widgets.FLabelWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * A row widget for an incoming friend request, with accept and decline buttons.
 * Handles the API calls for accept and decline internally.
 */
public class SocialFriendRequestRowWidget {
    private static final float AVATAR_SIZE = 32f;
    private static final float BTN_W = 20f;
    private static final float BTN_H = 20f;
    private static final int[] BADGE_COLORS = {0xFF6B5B95, 0xFF88B04B, 0xFF955251, 0xFF009B77, 0xFF45B8AC, 0xFF5B5EA6, 0xFFB565A7, 0xFFDD4132};

    public static FWidget build(PendingFriendRequest request, float width, float rowHeight) {
        FLabelWidget nameLabel = new FLabelWidget();
        nameLabel.setText(request.user().minecraftName());
        nameLabel.setColorArgb(FascinatedGuiTheme.INSTANCE.textPrimary());
        nameLabel.setTextBold(true);
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
                float nameMaxX = lx + width - 2f * (BTN_W + 2f) - 8f;
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
                Identifier avatarTexture = AvatarTextureCache.INSTANCE.get(request.user().minecraftUuid(), () -> {});
                if (avatarTexture != null) {
                    graphics.fillRoundedRect(avatarX, avatarY, AVATAR_SIZE, AVATAR_SIZE, 4f, 0xFF000000, RectCornerRoundMask.ALL);
                    graphics.drawTexture(avatarTexture, avatarX, avatarY, AVATAR_SIZE, AVATAR_SIZE, 0xFFFFFFFF);
                } else {
                    int badgeColor = BADGE_COLORS[Math.abs(request.user().minecraftName().hashCode()) % BADGE_COLORS.length];
                    graphics.fillRoundedRect(avatarX, avatarY, AVATAR_SIZE, AVATAR_SIZE, 4f, badgeColor, RectCornerRoundMask.ALL);
                    String initial = request.user().minecraftName().isEmpty() ? "?" : String.valueOf(Character.toUpperCase(request.user().minecraftName().charAt(0)));
                    graphics.drawCenteredText(initial, avatarX + AVATAR_SIZE / 2f, avatarY + (AVATAR_SIZE - graphics.getFontCapHeight()) / 2f, 0xFFFFFFFF, false, true);
                }

                float declineBtnX = x() + w() - BTN_W - 4f;
                float acceptBtnX = declineBtnX - BTN_W - 2f;
                float btnY = y() + (h() - BTN_H) / 2f;
                boolean acceptHovered = mouseX >= acceptBtnX && mouseX < acceptBtnX + BTN_W && mouseY >= btnY && mouseY < btnY + BTN_H;
                boolean declineHovered = mouseX >= declineBtnX && mouseX < declineBtnX + BTN_W && mouseY >= btnY && mouseY < btnY + BTN_H;

                graphics.fillRoundedRect(acceptBtnX, btnY, BTN_W, BTN_H, 4f, acceptHovered ? 0xAA1F5C1F : 0x22FFFFFF, RectCornerRoundMask.ALL);
                graphics.drawCenteredText("\u2713", acceptBtnX + BTN_W / 2f, btnY + (BTN_H - graphics.getFontCapHeight()) / 2f, acceptHovered ? 0xFF55FF55 : FascinatedGuiTheme.INSTANCE.textMuted(), false, false);

                graphics.fillRoundedRect(declineBtnX, btnY, BTN_W, BTN_H, 4f, declineHovered ? 0xAA5C1F1F : 0x22FFFFFF, RectCornerRoundMask.ALL);
                graphics.drawCenteredText("\u2715", declineBtnX + BTN_W / 2f, btnY + (BTN_H - graphics.getFontCapHeight()) / 2f, declineHovered ? 0xFFFF5555 : FascinatedGuiTheme.INSTANCE.textMuted(), false, false);
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
                float declineBtnX = x() + w() - BTN_W - 4f;
                float acceptBtnX = declineBtnX - BTN_W - 2f;
                float btnY = y() + (h() - BTN_H) / 2f;
                if (pointerX >= acceptBtnX && pointerX < acceptBtnX + BTN_W && pointerY >= btnY && pointerY < btnY + BTN_H) {
                    FascinatedUtils.SCHEDULED_POOL.execute(() -> {
                        try {
                            boolean accepted = Alumite.INSTANCE.acceptFriendRequest(request.requestId());
                            if (accepted) {
                                Toast.show().message("You're now friends with " + request.user().minecraftName() + "!").success();
                            }
                        } catch (AlumiteApiException exception) {
                            Toast.show().message(SocialErrors.message(exception)).error();
                        } catch (Exception exception) {
                            Toast.show().message(Component.translatable("fascinatedutils.social.error.generic").getString()).error();
                        }
                    });
                    return true;
                }
                if (pointerX >= declineBtnX && pointerX < declineBtnX + BTN_W && pointerY >= btnY && pointerY < btnY + BTN_H) {
                    FascinatedUtils.SCHEDULED_POOL.execute(() -> {
                        try {
                            Alumite.INSTANCE.declineFriendRequest(request.requestId());
                        } catch (AlumiteApiException exception) {
                            Toast.show().message(SocialErrors.message(exception)).error();
                        } catch (Exception exception) {
                            Toast.show().message(Component.translatable("fascinatedutils.social.error.generic").getString()).error();
                        }
                    });
                    return true;
                }
                return false;
            }
        };
    }
}
