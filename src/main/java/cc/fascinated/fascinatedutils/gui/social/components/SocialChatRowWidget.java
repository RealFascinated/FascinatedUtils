package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.gui.core.PointerHitKind;
import cc.fascinated.fascinatedutils.gui.core.TextLineLayout;
import cc.fascinated.fascinatedutils.gui.core.UiFrameContext;
import cc.fascinated.fascinatedutils.gui.core.UiPointerCursor;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FAvatarWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FBadgeWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FIconButtonWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;

import java.util.function.BiConsumer;

public class SocialChatRowWidget {
    private static final float AVATAR_SIZE = 32f;
    private static final float CLOSE_BTN_SIZE = 20f;

    public static FWidget build(Props props, float width, float rowHeight) {
        return new FWidget() {
            final FAvatarWidget avatar = new FAvatarWidget(AVATAR_SIZE, 4f,
                    () -> props.avatarMinecraftUuid(),
                    () -> props.displayName());
            {
                avatar.setPresenceDotColorSupplier(() -> props.presenceColor());
                addChild(avatar);
            }

            final FBadgeWidget unreadBadge = new FBadgeWidget(5f, 0xFFCC2222);
            {
                unreadBadge.setVisible(props.showUnreadBadge());
                addChild(unreadBadge);
            }

            final FIconButtonWidget closeBtn = new FIconButtonWidget(CLOSE_BTN_SIZE, 4f, () -> "\u2715") {
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
            {
                closeBtn.setDrawBorder(false);
                closeBtn.setVisible(props.onCloseChannel() != null);
                if (props.onCloseChannel() != null) {
                    closeBtn.setOnClick(props.onCloseChannel());
                }
                addChild(closeBtn);
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
                if (props.showUnreadBadge()) {
                    unreadBadge.layout(measure, lx + width - 14f, ly + 3f, 10f, 10f);
                }
                if (props.onCloseChannel() != null) {
                    float closeBtnX = lx + width - 4f - CLOSE_BTN_SIZE;
                    if (props.showUnreadBadge()) {
                        closeBtnX -= 14f;
                    }
                    closeBtn.layout(measure, closeBtnX, ly + (lh - CLOSE_BTN_SIZE) / 2f, CLOSE_BTN_SIZE, CLOSE_BTN_SIZE);
                }
            }

            @Override
            protected void renderSelf(GuiRenderer graphics, UiFrameContext frame, float deltaSeconds) {
                float mouseX = frame.pointerX();
                float mouseY = frame.pointerY();
                boolean rowHovered = containsPoint(mouseX, mouseY);
                int fill = props.selected() ? 0x334960C8 : rowHovered ? 0x22FFFFFF : UITheme.COLOR_BACKGROUND;
                graphics.fillRoundedRect(x(), y(), w(), h(), UITheme.CORNER_RADIUS_SM, fill, RectCornerRoundMask.ALL);

                float textX = avatar.x() + AVATAR_SIZE + 6f;
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
                return false;
            }

            @Override
            public boolean click(float pointerX, float pointerY, int button) {
                if (button == 0) {
                    props.onSelect().run();
                    return true;
                }
                return false;
            }
        };
    }

    public record Props(String displayName, String avatarMinecraftUuid, String snippet, int presenceColor,
                        boolean selected, boolean showUnreadBadge, Runnable onSelect, Runnable onCloseChannel,
                        BiConsumer<Float, Float> onContextMenu) {}
}
