package cc.fascinated.fascinatedutils.gui.social.components;

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
import net.minecraft.resources.Identifier;

import java.util.function.BiConsumer;

public class SocialChatRowWidget {
    private static final float AVATAR_SIZE = 32f;
    private static final float CLOSE_BTN_SIZE = 20f;

    public static FWidget build(Props props, float width, float rowHeight) {
        return new FWidget() {
            @Override
            public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
                return rowHeight;
            }

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, width, lh);
            }

            @Override
            protected void renderSelf(GuiRenderer graphics, UiFrameContext frame, float deltaSeconds) {
                float mouseX = frame.pointerX();
                float mouseY = frame.pointerY();
                boolean rowHovered = containsPoint(mouseX, mouseY);
                int fill = props.selected() ? 0x334960C8 : rowHovered ? 0x22FFFFFF : UITheme.COLOR_BACKGROUND;
                graphics.fillRoundedRect(x(), y(), w(), h(), UITheme.CORNER_RADIUS_SM, fill, RectCornerRoundMask.ALL);

                float avatarX = x() + 4f;
                float avatarY = y() + (h() - AVATAR_SIZE) / 2f;
                Identifier avatarTexture = props.avatarMinecraftUuid() == null || props.avatarMinecraftUuid().isBlank() ? null : AvatarTextureCache.INSTANCE.get(props.avatarMinecraftUuid(), () -> {});
                String first = props.displayName() == null || props.displayName().isBlank() ? "?" : String.valueOf(Character.toUpperCase(props.displayName().charAt(0)));
                if (avatarTexture != null) {
                    graphics.fillRoundedRect(avatarX, avatarY, AVATAR_SIZE, AVATAR_SIZE, 4f, 0xFF000000, RectCornerRoundMask.ALL);
                    graphics.drawTexture(avatarTexture, avatarX, avatarY, AVATAR_SIZE, AVATAR_SIZE, 0xFFFFFFFF);
                }
                else {
                    graphics.fillRoundedRect(avatarX, avatarY, AVATAR_SIZE, AVATAR_SIZE, 4f, 0xFF3B445A, RectCornerRoundMask.ALL);
                    graphics.drawCenteredText(first, avatarX + AVATAR_SIZE / 2f, avatarY + (AVATAR_SIZE - graphics.getFontCapHeight()) / 2f, 0xFFFFFFFF, false, true);
                }

                float textX = avatarX + AVATAR_SIZE + 6f;
                float reservedRight = 6f;
                if (props.showUnreadBadge()) {
                    reservedRight += 18f;
                }
                if (props.onCloseChannel() != null) {
                    reservedRight += CLOSE_BTN_SIZE + 4f;
                }
                float maxLineWidth = Math.max(0f, x() + w() - textX - reservedRight);
                float titleY = y() + 8f;
                String title = props.displayName() == null || props.displayName().isBlank() ? "Direct Message" : props.displayName();
                String titleDraw = TextLineLayout.ellipsize(title, maxLineWidth, segment -> graphics.measureTextWidth(segment, true));
                graphics.drawText(titleDraw, textX, titleY, FascinatedGuiTheme.INSTANCE.textPrimary(), true, false);
                String previewDraw = TextLineLayout.ellipsize(normalizeSnippet(props.snippet()), maxLineWidth, segment -> graphics.measureTextWidth(segment, false));
                graphics.drawText(previewDraw, textX, titleY + graphics.getFontCapHeight() + 3f, FascinatedGuiTheme.INSTANCE.textMuted(), false, false);

                float dotX = avatarX + 24f;
                float dotY = avatarY + 24f;
                graphics.fillRoundedRect(dotX - 1f, dotY - 1f, 8f, 8f, 4f, 0xFF1A1E24, RectCornerRoundMask.ALL);
                graphics.fillRoundedRect(dotX, dotY, 6f, 6f, 3f, props.presenceColor(), RectCornerRoundMask.ALL);

                if (props.showUnreadBadge()) {
                    float badgeRadius = 5f;
                    float badgeCenterX = x() + w() - badgeRadius - 4f;
                    float badgeCenterY = y() + badgeRadius + 3f;
                    graphics.fillRoundedRect(badgeCenterX - badgeRadius, badgeCenterY - badgeRadius, badgeRadius * 2f, badgeRadius * 2f, badgeRadius, 0xFFCC2222, RectCornerRoundMask.ALL);
                }

                if (props.onCloseChannel() != null) {
                    float closeBtnX = x() + w() - 4f - CLOSE_BTN_SIZE;
                    if (props.showUnreadBadge()) {
                        closeBtnX -= 14f;
                    }
                    float closeBtnY = y() + (h() - CLOSE_BTN_SIZE) / 2f;
                    boolean closeHovered = mouseX >= closeBtnX && mouseX < closeBtnX + CLOSE_BTN_SIZE && mouseY >= closeBtnY && mouseY < closeBtnY + CLOSE_BTN_SIZE;
                    graphics.fillRoundedRect(closeBtnX, closeBtnY, CLOSE_BTN_SIZE, CLOSE_BTN_SIZE, 4f, closeHovered ? 0xAA5C1F1F : 0x22FFFFFF, RectCornerRoundMask.ALL);
                    graphics.drawCenteredText("\u2715", closeBtnX + CLOSE_BTN_SIZE / 2f, closeBtnY + (CLOSE_BTN_SIZE - graphics.getFontCapHeight()) / 2f, closeHovered ? 0xFFFF5555 : FascinatedGuiTheme.INSTANCE.textMuted(), false, false);
                }
            }

            private String normalizeSnippet(String value) {
                if (value == null || value.isBlank()) {
                    return "";
                }
                return value.replace('\n', ' ').trim();
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
                if (button == 1) {
                    if (props.onContextMenu() != null) {
                        props.onContextMenu().accept(pointerX, pointerY);
                    }
                    return true;
                }
                if (button != 0) {
                    return false;
                }
                if (props.onCloseChannel() != null) {
                    float closeBtnX = x() + w() - 4f - CLOSE_BTN_SIZE;
                    if (props.showUnreadBadge()) {
                        closeBtnX -= 14f;
                    }
                    float closeBtnY = y() + (h() - CLOSE_BTN_SIZE) / 2f;
                    if (pointerX >= closeBtnX && pointerX < closeBtnX + CLOSE_BTN_SIZE && pointerY >= closeBtnY && pointerY < closeBtnY + CLOSE_BTN_SIZE) {
                        props.onCloseChannel().run();
                        return true;
                    }
                }
                props.onSelect().run();
                return true;
            }
        };
    }

    public record Props(String displayName, String avatarMinecraftUuid, String snippet, int presenceColor,
                        boolean selected, boolean showUnreadBadge, Runnable onSelect, Runnable onCloseChannel,
                        BiConsumer<Float, Float> onContextMenu) {}
}
