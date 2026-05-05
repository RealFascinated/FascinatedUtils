package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.api.user.Presence;
import cc.fascinated.fascinatedutils.api.user.User;
import cc.fascinated.fascinatedutils.gui.AvatarTextureCache;
import cc.fascinated.fascinatedutils.gui.core.PointerHitKind;
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

public class SocialFriendRowWidget {
    public static FWidget build(Props props, float width, float rowHeight) {
        return new FWidget() {
            private static final float BTN_W = 20f;
            private static final float BTN_H = 20f;

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
                boolean hovered = containsPoint(mouseX, mouseY);
                int fill = props.selected() ? 0x334960C8 : hovered ? 0x22FFFFFF : UITheme.COLOR_BACKGROUND;
                graphics.fillRoundedRect(x(), y(), w(), h(), UITheme.CORNER_RADIUS_SM, fill, RectCornerRoundMask.ALL);

                float avatarX = x() + 4f;
                float avatarY = y() + (h() - 32f) / 2f;
                Identifier avatar = userMinecraftUuid() == null ? null : AvatarTextureCache.INSTANCE.get(userMinecraftUuid(), () -> {});
                if (avatar != null) {
                    graphics.fillRoundedRect(avatarX, avatarY, 32f, 32f, 4f, 0xFF000000, RectCornerRoundMask.ALL);
                    graphics.drawTexture(avatar, avatarX, avatarY, 32f, 32f, 0xFFFFFFFF);
                }
                else {
                    graphics.fillRoundedRect(avatarX, avatarY, 32f, 32f, 4f, 0xFF3B445A, RectCornerRoundMask.ALL);
                    String initials = displayName().isBlank() ? "?" : String.valueOf(Character.toUpperCase(displayName().charAt(0)));
                    graphics.drawCenteredText(initials, avatarX + 16f, avatarY + (32f - graphics.getFontCapHeight()) / 2f, 0xFFFFFFFF, false, true);
                }

                float textX = avatarX + 38f;
                float textY = y() + 8f;
                graphics.drawText(displayName(), textX, textY, FascinatedGuiTheme.INSTANCE.textPrimary(), true, false);
                graphics.drawText(props.statusText(), textX, textY + graphics.getFontCapHeight() + 3f, presenceColor(), false, false);

                float dotX = avatarX + 24f;
                float dotY = avatarY + 24f;
                graphics.fillRoundedRect(dotX - 1f, dotY - 1f, 8f, 8f, 4f, 0xFF1A1E24, RectCornerRoundMask.ALL);
                graphics.fillRoundedRect(dotX, dotY, 6f, 6f, 3f, presenceColor(), RectCornerRoundMask.ALL);

                if (hovered) {
                    float removeX = x() + w() - BTN_W - 4f;
                    float removeY = y() + (h() - BTN_H) / 2f;
                    boolean removeHovered = mouseX >= removeX && mouseX < removeX + BTN_W && mouseY >= removeY && mouseY < removeY + BTN_H;
                    graphics.fillRoundedRect(removeX, removeY, BTN_W, BTN_H, 4f, removeHovered ? 0xAA5C1F1F : 0x22FFFFFF, RectCornerRoundMask.ALL);
                    graphics.drawCenteredText("✕", removeX + BTN_W / 2f, removeY + (BTN_H - graphics.getFontCapHeight()) / 2f, removeHovered ? 0xFFFF5555 : FascinatedGuiTheme.INSTANCE.textMuted(), false, false);
                }
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
                float removeX = x() + w() - BTN_W - 4f;
                float removeY = y() + (h() - BTN_H) / 2f;
                if (pointerX >= removeX && pointerX < removeX + BTN_W && pointerY >= removeY && pointerY < removeY + BTN_H) {
                    props.onRemove().run();
                    return true;
                }
                props.onSelect().run();
                return true;
            }

            private String displayName() {
                return props.user() == null || props.user().minecraftName() == null || props.user().minecraftName().isBlank() ? "..." : props.user().minecraftName();
            }

            private String userMinecraftUuid() {
                return props.user() == null ? null : props.user().minecraftUuid();
            }

            private int presenceColor() {
                Presence presence = props.user() == null || props.user().presence() == null ? Presence.OFFLINE : props.user().presence();
                return presence.color();
            }
        };
    }

    public record Props(User user, String statusText, boolean selected, Runnable onSelect, Runnable onRemove, BiConsumer<Float, Float> onContextMenu) {}
}
