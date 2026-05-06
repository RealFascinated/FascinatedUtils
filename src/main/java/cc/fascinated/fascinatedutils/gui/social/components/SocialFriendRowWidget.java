package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.api.user.Presence;
import cc.fascinated.fascinatedutils.api.user.User;
import cc.fascinated.fascinatedutils.gui.core.PointerHitKind;
import cc.fascinated.fascinatedutils.gui.core.UiFrameContext;
import cc.fascinated.fascinatedutils.gui.core.UiPointerCursor;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FAvatarWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FIconButtonWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;

import java.util.function.BiConsumer;

public class SocialFriendRowWidget {

    public static FWidget build(Props props, float width, float rowHeight) {
        return new FWidget() {
            private static final float AVATAR_SIZE = 32f;
            private static final float BTN_SIZE = 20f;

            final FAvatarWidget avatar = new FAvatarWidget(AVATAR_SIZE, 4f,
                    () -> props.user() == null ? null : props.user().minecraftUuid(),
                    () -> displayName());
            {
                avatar.setPresenceDotColorSupplier(() -> presenceColor());
                addChild(avatar);
            }

            final FIconButtonWidget removeBtn = new FIconButtonWidget(BTN_SIZE, 4f, () -> "✕") {
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
                removeBtn.setDrawBorder(false);
                removeBtn.setOnClick(props.onRemove());
                addChild(removeBtn);
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
                float removeBtnY = ly + (lh - BTN_SIZE) / 2f;
                removeBtn.layout(measure, lx + width - BTN_SIZE - 4f, removeBtnY, BTN_SIZE, BTN_SIZE);
            }

            @Override
            protected void renderSelf(GuiRenderer graphics, UiFrameContext frame, float deltaSeconds) {
                float mouseX = frame.pointerX();
                float mouseY = frame.pointerY();
                boolean hovered = containsPoint(mouseX, mouseY);
                int fill = props.selected() ? 0x334960C8 : hovered ? 0x22FFFFFF : UITheme.COLOR_BACKGROUND;
                graphics.fillRoundedRect(x(), y(), w(), h(), UITheme.CORNER_RADIUS_SM, fill, RectCornerRoundMask.ALL);
                removeBtn.setVisible(hovered);
                float textX = avatar.x() + AVATAR_SIZE + 6f;
                float textY = y() + 8f;
                graphics.drawText(displayName(), textX, textY, FascinatedGuiTheme.INSTANCE.textPrimary(), true, false);
                graphics.drawText(props.statusText(), textX, textY + graphics.getFontCapHeight() + 3f, presenceColor(), false, false);
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

            private String displayName() {
                return props.user() == null || props.user().minecraftName() == null || props.user().minecraftName().isBlank() ? "..." : props.user().minecraftName();
            }

            private int presenceColor() {
                Presence presence = props.user() == null || props.user().presence() == null ? Presence.OFFLINE : props.user().presence();
                return presence.color();
            }
        };
    }

    public record Props(User user, String statusText, boolean selected, Runnable onSelect, Runnable onRemove, BiConsumer<Float, Float> onContextMenu) {}
}
