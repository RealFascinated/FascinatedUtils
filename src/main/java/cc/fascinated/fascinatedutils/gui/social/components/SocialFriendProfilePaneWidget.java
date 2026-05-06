package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.api.user.User;
import cc.fascinated.fascinatedutils.gui.core.PointerHitKind;
import cc.fascinated.fascinatedutils.gui.core.TextLineLayout;
import cc.fascinated.fascinatedutils.gui.core.UiFrameContext;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FAvatarWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FButtonWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import net.minecraft.network.chat.Component;

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

            final FAvatarWidget avatarWidget = new FAvatarWidget(AVATAR_SIZE, UITheme.CORNER_RADIUS_SM,
                    () -> userMinecraftUuid(), () -> displayName());
            {
                avatarWidget.setFallbackColor(AVATAR_FALLBACK);
                avatarWidget.setTextureBackgroundArgb(AVATAR_FILL);
                avatarWidget.setBorderArgb(AVATAR_BORDER, 1f);
            }

            final FButtonWidget msgBtn = new FButtonWidget(props.onMessage(),
                    () -> Component.translatable("alumite.social.friend_profile.message").getString(),
                    0f, 1, 0f, 8f, 1f, 8f, UITheme.CORNER_RADIUS_MD) {
                @Override
                protected int resolveButtonFillColorArgb(boolean hovered) {
                    return hovered ? 0xFF8E6DD1 : UITheme.COLOR_ACCENT;
                }

                @Override
                protected int resolveButtonBorderColorArgb(boolean hovered) {
                    return hovered ? UITheme.COLOR_ACCENT_HOVER : UITheme.COLOR_ACCENT;
                }

                @Override
                protected int resolveButtonLabelColorArgb(boolean hovered) {
                    return 0xFFFFFFFF;
                }
            };

            final FButtonWidget removeBtn = new FButtonWidget(props.onRemove(),
                    () -> Component.translatable("alumite.social.confirm_remove_friend.confirm").getString(),
                    0f, 1, 0f, 8f, 1f, 8f, UITheme.CORNER_RADIUS_MD) {
                @Override
                protected int resolveButtonFillColorArgb(boolean hovered) {
                    return hovered ? 0xFF242A38 : 0xFF1E2230;
                }

                @Override
                protected int resolveButtonBorderColorArgb(boolean hovered) {
                    return hovered ? 0xFF6C7098 : UITheme.COLOR_BORDER;
                }

                @Override
                protected int resolveButtonLabelColorArgb(boolean hovered) {
                    return hovered ? 0xFFFF8C8C : FascinatedGuiTheme.INSTANCE.textMuted();
                }
            };

            {
                addChild(avatarWidget);
                addChild(msgBtn);
                addChild(removeBtn);
            }

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
                float desiredHeight = CARD_PADDING * 2f + HEADER_HEIGHT + 10f + ACTION_HEIGHT;
                panelHeight = Math.min(lh, desiredHeight);
                avatarWidget.layout(measure, lx + CARD_PADDING, ly + CARD_PADDING, AVATAR_SIZE, AVATAR_SIZE);
                float actionWidth = Math.max(0f, (lw - CARD_PADDING * 2f - ACTION_GAP) / 2f);
                float messageX = lx + CARD_PADDING;
                float removeX = messageX + actionWidth + ACTION_GAP;
                float actionY = ly + panelHeight - CARD_PADDING - ACTION_HEIGHT;
                msgBtn.layout(measure, messageX, actionY, actionWidth, ACTION_HEIGHT);
                removeBtn.layout(measure, removeX, actionY, actionWidth, ACTION_HEIGHT);
            }

            @Override
            protected void renderSelf(GuiRenderer graphics, UiFrameContext frame, float deltaSeconds) {
                graphics.fillRoundedRectFrame(x(), y(), w(), panelHeight, CARD_CORNER, CARD_BORDER, CARD_FILL, 1f, 1f, RectCornerRoundMask.ALL);

                float textX = x() + CARD_PADDING + AVATAR_SIZE + 10f;
                float textWidth = Math.max(0f, x() + w() - textX - CARD_PADDING);
                graphics.drawText(ellipsize(graphics, displayName(), textWidth, true), textX, y() + CARD_PADDING + 3f, FascinatedGuiTheme.INSTANCE.textPrimary(), true, false);

                graphics.drawText(ellipsize(graphics, uuidText(), textWidth, false), textX, y() + CARD_PADDING + 19f, FascinatedGuiTheme.INSTANCE.textMuted(), false, false);

                float dividerY = msgBtn.y() - 8f;
                graphics.drawRect(x() + CARD_PADDING, dividerY, w() - CARD_PADDING * 2f, 1f, 0x22FFFFFF);
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

            private String ellipsize(GuiRenderer graphics, String value, float maxWidth, boolean bold) {
                String safeValue = value == null ? "" : value;
                return TextLineLayout.ellipsize(safeValue, Math.max(0f, maxWidth), segment -> graphics.measureTextWidth(segment, bold));
            }

            @Override
            public PointerHitKind pointerHitKind() {
                return PointerHitKind.TARGET;
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