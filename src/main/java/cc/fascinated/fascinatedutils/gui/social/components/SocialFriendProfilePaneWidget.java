package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.api.user.User;
import cc.fascinated.fascinatedutils.gui.AvatarTextureCache;
import cc.fascinated.fascinatedutils.gui.core.PointerHitKind;
import cc.fascinated.fascinatedutils.gui.core.TextLineLayout;
import cc.fascinated.fascinatedutils.gui.core.UiFrameContext;
import cc.fascinated.fascinatedutils.gui.core.UiPointerCursor;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Objects;

public class SocialFriendProfilePaneWidget {
    private static final float CARD_PADDING = 12f;
    private static final float CARD_CORNER = UITheme.CORNER_RADIUS_MD;
    private static final float AVATAR_SIZE = 42f;
    private static final float HEADER_HEIGHT = 54f;
    private static final float ACTION_HEIGHT = 22f;
    private static final float ACTION_GAP = 8f;
    private static final int CARD_BORDER = 0xFF454A60;
    private static final int CARD_FILL = 0xFF171B24;
    private static final int AVATAR_BORDER = 0xFF2F3748;
    private static final int AVATAR_FILL = 0xFF0F1318;
    private static final int AVATAR_FALLBACK = 0xFF3B445A;

    public static FWidget build(Props props) {
        return new FWidget() {
            private float panelHeight;
            private float messageX;
            private float removeX;
            private float actionY;
            private float actionWidth;

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
                float desiredHeight = CARD_PADDING * 2f + HEADER_HEIGHT + 10f + ACTION_HEIGHT;
                panelHeight = Math.min(lh, desiredHeight);
                actionWidth = Math.max(0f, (lw - CARD_PADDING * 2f - ACTION_GAP) / 2f);
                messageX = lx + CARD_PADDING;
                removeX = messageX + actionWidth + ACTION_GAP;
                actionY = ly + panelHeight - CARD_PADDING - ACTION_HEIGHT;
            }

            @Override
            protected void renderSelf(GuiRenderer graphics, UiFrameContext frame, float deltaSeconds) {
                float mouseX = frame.pointerX();
                float mouseY = frame.pointerY();

                graphics.fillRoundedRectFrame(x(), y(), w(), panelHeight, CARD_CORNER, CARD_BORDER, CARD_FILL, 1f, 1f, RectCornerRoundMask.ALL);

                float avatarX = x() + CARD_PADDING;
                float avatarY = y() + CARD_PADDING;
                drawAvatar(graphics, avatarX, avatarY);

                float textX = avatarX + AVATAR_SIZE + 10f;
                float textWidth = Math.max(0f, x() + w() - textX - CARD_PADDING);
                graphics.drawText(ellipsize(graphics, displayName(), textWidth, true), textX, avatarY + 3f, FascinatedGuiTheme.INSTANCE.textPrimary(), true, false);

                graphics.drawText(ellipsize(graphics, uuidText(), textWidth, false), textX, avatarY + 19f, FascinatedGuiTheme.INSTANCE.textMuted(), false, false);

                float dividerY = actionY - 8f;
                graphics.drawRect(x() + CARD_PADDING, dividerY, w() - CARD_PADDING * 2f, 1f, 0x22FFFFFF);

                drawButton(graphics, mouseX, mouseY, messageX, actionY, actionWidth, ACTION_HEIGHT, Component.translatable("fascinatedutils.social.friend_profile.message").getString(), true);
                drawButton(graphics, mouseX, mouseY, removeX, actionY, actionWidth, ACTION_HEIGHT, Component.translatable("fascinatedutils.social.confirm_remove_friend.confirm").getString(), false);
            }

            private void drawAvatar(GuiRenderer graphics, float avatarX, float avatarY) {
                graphics.fillRoundedRectFrame(avatarX, avatarY, AVATAR_SIZE, AVATAR_SIZE, UITheme.CORNER_RADIUS_SM, AVATAR_BORDER, AVATAR_FILL, 1f, 1f, RectCornerRoundMask.ALL);
                Identifier avatar = AvatarTextureCache.INSTANCE.get(userMinecraftUuid(), () -> {});
                if (avatar != null) {
                    graphics.drawTexture(avatar, avatarX + 1f, avatarY + 1f, AVATAR_SIZE - 2f, AVATAR_SIZE - 2f, 0xFFFFFFFF);
                }
                else {
                    graphics.fillRoundedRect(avatarX + 1f, avatarY + 1f, AVATAR_SIZE - 2f, AVATAR_SIZE - 2f, UITheme.CORNER_RADIUS_SM, AVATAR_FALLBACK, RectCornerRoundMask.ALL);
                    graphics.drawCenteredText(initials(), avatarX + AVATAR_SIZE / 2f, avatarY + (AVATAR_SIZE - graphics.getFontCapHeight()) / 2f, 0xFFFFFFFF, false, true);
                }
            }

            private void drawButton(GuiRenderer graphics, float mouseX, float mouseY, float buttonX, float buttonY, float buttonWidth, float buttonHeight, String label, boolean primary) {
                boolean hovered = inside(mouseX, mouseY, buttonX, buttonY, buttonWidth, buttonHeight);
                int border = primary ? hovered ? UITheme.COLOR_ACCENT_HOVER : UITheme.COLOR_ACCENT : hovered ? 0xFF6C7098 : UITheme.COLOR_BORDER;
                int fill = primary ? hovered ? 0xFF8E6DD1 : UITheme.COLOR_ACCENT : hovered ? 0xFF242A38 : 0xFF1E2230;
                int text = primary ? 0xFFFFFFFF : hovered ? 0xFFFF8C8C : FascinatedGuiTheme.INSTANCE.textMuted();
                graphics.fillRoundedRectFrame(buttonX, buttonY, buttonWidth, buttonHeight, UITheme.CORNER_RADIUS_MD, border, fill, 1f, 1f, RectCornerRoundMask.ALL);
                graphics.drawCenteredText(label, buttonX + buttonWidth / 2f, buttonY + (buttonHeight - graphics.getFontCapHeight()) / 2f, text, false, false);
            }

            private String displayName() {
                return props.user().minecraftName();
            }

            private String uuidText() {
                return userMinecraftUuid();
            }

            private String userMinecraftUuid() {
                return props.user().minecraftUuid();
            }

            private String initials() {
                return String.valueOf(Character.toUpperCase(displayName().charAt(0)));
            }

            private String ellipsize(GuiRenderer graphics, String value, float maxWidth, boolean bold) {
                String safeValue = value == null ? "" : value;
                return TextLineLayout.ellipsize(safeValue, Math.max(0f, maxWidth), segment -> graphics.measureTextWidth(segment, bold));
            }

            @Override
            public PointerHitKind pointerHitKind() {
                return PointerHitKind.TARGET;
            }

            @Override
            public UiPointerCursor pointerCursor(float pointerX, float pointerY) {
                if (inside(pointerX, pointerY, messageX, actionY, actionWidth, ACTION_HEIGHT) || inside(pointerX, pointerY, removeX, actionY, actionWidth, ACTION_HEIGHT)) {
                    return UiPointerCursor.HAND;
                }
                return UiPointerCursor.DEFAULT;
            }

            @Override
            public boolean mouseDown(float pointerX, float pointerY, int button) {
                if (button != 0) {
                    return false;
                }
                if (inside(pointerX, pointerY, messageX, actionY, actionWidth, ACTION_HEIGHT)) {
                    props.onMessage().run();
                    return true;
                }
                if (inside(pointerX, pointerY, removeX, actionY, actionWidth, ACTION_HEIGHT)) {
                    props.onRemove().run();
                    return true;
                }
                return false;
            }

            private boolean inside(float pointerX, float pointerY, float buttonX, float buttonY, float buttonWidth, float buttonHeight) {
                return pointerX >= buttonX && pointerX < buttonX + buttonWidth && pointerY >= buttonY && pointerY < buttonY + buttonHeight;
            }

        };
    }

    public record Props(User user, Runnable onMessage, Runnable onRemove) {
        public Props {
            Objects.requireNonNull(user, "user");
            Objects.requireNonNull(user.minecraftName(), "user.minecraftName");
            Objects.requireNonNull(user.minecraftUuid(), "user.minecraftUuid");
        }
    }
}