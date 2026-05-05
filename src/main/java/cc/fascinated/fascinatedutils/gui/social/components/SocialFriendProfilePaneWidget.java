package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.gui.core.UiFrameContext;

import cc.fascinated.fascinatedutils.gui.AvatarTextureCache;
import cc.fascinated.fascinatedutils.gui.core.PointerHitKind;
import cc.fascinated.fascinatedutils.gui.core.UiPointerCursor;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import net.minecraft.resources.Identifier;

public class SocialFriendProfilePaneWidget {
    public record Props(
            String minecraftName,
            String minecraftUuid,
            String statusLine,
            String lastSeenLine,
            Runnable onMessage,
            Runnable onRemove
    ) {
    }

    public static FWidget build(Props props) {
        return new FWidget() {
            private float messageX;
            private float removeX;
            private float buttonY;
            private static final float BTN_W = 82f;
            private static final float BTN_H = 20f;

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
                messageX = lx + 2f;
                removeX = lx + BTN_W + 6f;
                buttonY = ly + 66f;
            }

            @Override
            protected void renderSelf(GuiRenderer graphics, UiFrameContext frame, float deltaSeconds) {
        float mouseX = frame.pointerX();
        float mouseY = frame.pointerY();
                graphics.fillRoundedRect(x(), y(), w(), Math.min(h(), 120f), 6f, 0x221E2230, RectCornerRoundMask.ALL);

                float avatarX = x() + 8f;
                float avatarY = y() + 10f;
                Identifier avatar = props.minecraftUuid() == null ? null : AvatarTextureCache.INSTANCE.get(props.minecraftUuid(), () -> {});
                if (avatar != null) {
                    graphics.fillRoundedRect(avatarX, avatarY, 42f, 42f, 4f, 0xFF000000, RectCornerRoundMask.ALL);
                    graphics.drawTexture(avatar, avatarX, avatarY, 42f, 42f, 0xFFFFFFFF);
                } else {
                    graphics.fillRoundedRect(avatarX, avatarY, 42f, 42f, 4f, 0xFF3B445A, RectCornerRoundMask.ALL);
                    String initials = props.minecraftName() == null || props.minecraftName().isBlank()
                            ? "?"
                            : String.valueOf(Character.toUpperCase(props.minecraftName().charAt(0)));
                    graphics.drawCenteredText(initials, avatarX + 21f, avatarY + (42f - graphics.getFontCapHeight()) / 2f,
                            0xFFFFFFFF, false, true);
                }

                float textX = avatarX + 50f;
                graphics.drawText(props.minecraftName(), textX, y() + 14f, FascinatedGuiTheme.INSTANCE.textPrimary(), true, false);
                graphics.fillRoundedRect(textX, y() + 30f, 64f, 14f, 4f, 0x223E84F8, RectCornerRoundMask.ALL);
                graphics.drawCenteredText(props.statusLine(), textX + 32f, y() + 33f, 0xFF9EC3FF, false, false);
                if (props.lastSeenLine() != null && !props.lastSeenLine().isBlank()) {
                    graphics.drawText(props.lastSeenLine(), textX, y() + 49f, FascinatedGuiTheme.INSTANCE.textMuted(), false, false);
                }
                drawButton(graphics, mouseX, mouseY, messageX, buttonY, "Message");
                drawButton(graphics, mouseX, mouseY, removeX, buttonY, "Remove");
            }

            private void drawButton(GuiRenderer graphics, float mouseX, float mouseY, float buttonX, float buttonY, String label) {
                boolean hovered = mouseX >= buttonX && mouseX < buttonX + BTN_W
                        && mouseY >= buttonY && mouseY < buttonY + BTN_H;
                graphics.fillRoundedRect(buttonX, buttonY, BTN_W, BTN_H, 4f,
                        hovered ? 0xFF2A2F3E : 0xFF1E2230, RectCornerRoundMask.ALL);
                graphics.drawCenteredText(label, buttonX + BTN_W / 2f, buttonY + (BTN_H - graphics.getFontCapHeight()) / 2f,
                        FascinatedGuiTheme.INSTANCE.textPrimary(), false, false);
            }

            @Override
            public PointerHitKind pointerHitKind() {
                return PointerHitKind.TARGET;
            }

            @Override
            public UiPointerCursor pointerCursor(float pointerX, float pointerY) {
                if (inside(pointerX, pointerY, messageX, buttonY) || inside(pointerX, pointerY, removeX, buttonY)) {
                    return UiPointerCursor.HAND;
                }
                return UiPointerCursor.DEFAULT;
            }

            @Override
            public boolean mouseDown(float pointerX, float pointerY, int button) {
                if (button != 0) {
                    return false;
                }
                if (inside(pointerX, pointerY, messageX, buttonY)) {
                    props.onMessage().run();
                    return true;
                }
                if (inside(pointerX, pointerY, removeX, buttonY)) {
                    props.onRemove().run();
                    return true;
                }
                return false;
            }

            private boolean inside(float pointerX, float pointerY, float buttonX, float actionY) {
                return pointerX >= buttonX && pointerX < buttonX + BTN_W
                        && pointerY >= actionY && pointerY < actionY + BTN_H;
            }
        };
    }
}
