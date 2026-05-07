package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.AlumiteMod;
import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.AlumiteApiException;
import cc.fascinated.fascinatedutils.api.friend.PendingFriendRequest;
import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.gui.core.*;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.toast.Toast;
import cc.fascinated.fascinatedutils.gui.widgets.FAvatarWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FIconButtonWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FLabelWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import net.minecraft.network.chat.Component;

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
        int badgeColor = BADGE_COLORS[Math.abs(request.user().minecraftName().hashCode()) % BADGE_COLORS.length];

        FLabelWidget nameLabel = new FLabelWidget();
        nameLabel.setText(request.user().minecraftName());
        nameLabel.setColorArgb(FascinatedGuiTheme.INSTANCE.textPrimary());
        nameLabel.setTextBold(true);
        nameLabel.setOverflow(TextOverflow.ELLIPSIS);
        nameLabel.setAlignY(Align.CENTER);

        FAvatarWidget avatar = new FAvatarWidget(AVATAR_SIZE, 4f,
                () -> request.user().minecraftUuid(),
                () -> request.user().minecraftName());
        avatar.setFallbackColor(badgeColor);

        FIconButtonWidget acceptBtn = new FIconButtonWidget(BTN_W, 3f, 4f, ModUiTextures.CHECK::getId, true) {
            @Override
            protected int resolveButtonFillArgb(boolean hovered) {
                return hovered ? 0xAA1F5C1F : 0x22FFFFFF;
            }

            @Override
            protected int resolveButtonBorderArgb(boolean hovered) {
                return resolveButtonFillArgb(hovered);
            }

            @Override
            protected int resolveContentTintArgb(boolean hovered) {
                return hovered ? 0xFF55FF55 : FascinatedGuiTheme.INSTANCE.textMuted();
            }
        };
        acceptBtn.setDrawBorder(false);
        acceptBtn.setOnClick(() -> AlumiteMod.SCHEDULED_POOL.execute(() -> {
            try {
                boolean accepted = Alumite.INSTANCE.acceptFriendRequest(request.requestId());
                if (accepted) {
                    Toast.show().message("You're now friends with " + request.user().minecraftName() + "!").success();
                }
            } catch (AlumiteApiException exception) {
                Toast.show().message(SocialErrors.message(exception)).error();
            } catch (Exception exception) {
                Toast.show().message(Component.translatable("alumite.social.error.generic").getString()).error();
            }
        }));

        FIconButtonWidget declineBtn = new FIconButtonWidget(BTN_W, 3f, 4f, ModUiTextures.CLOSE::getId, true) {
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
        declineBtn.setDrawBorder(false);
        declineBtn.setOnClick(() -> AlumiteMod.SCHEDULED_POOL.execute(() -> {
            try {
                Alumite.INSTANCE.declineFriendRequest(request.requestId());
            } catch (AlumiteApiException exception) {
                Toast.show().message(SocialErrors.message(exception)).error();
            } catch (Exception exception) {
                Toast.show().message(Component.translatable("alumite.social.error.generic").getString()).error();
            }
        }));

        return new FWidget() {
            {
                addChild(avatar);
                addChild(nameLabel);
                addChild(acceptBtn);
                addChild(declineBtn);
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
                float declineBtnX = lx + width - BTN_W - 4f;
                float acceptBtnX = declineBtnX - BTN_W - 2f;
                float btnY = ly + (lh - BTN_H) / 2f;
                acceptBtn.layout(measure, acceptBtnX, btnY, BTN_W, BTN_H);
                declineBtn.layout(measure, declineBtnX, btnY, BTN_W, BTN_H);
                float nameX = lx + 4f + AVATAR_SIZE + 6f;
                nameLabel.layout(measure, nameX, ly, acceptBtnX - nameX - 4f, lh);
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
