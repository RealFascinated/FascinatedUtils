package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.gui.core.UiFrameContext;

import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.gui.core.PointerHitKind;
import cc.fascinated.fascinatedutils.gui.core.UiPointerCursor;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import cc.fascinated.fascinatedutils.gui.widgets.WTooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.function.BooleanSupplier;

public class SocialHeaderWidget {
    public record Props(
            Runnable onAddFriend,
            Runnable onNewChat,
            Runnable onSearch,
            BooleanSupplier searchActive
    ) {
    }

    public static FWidget build(Props props) {
        return new FWidget() {
            private float addFriendX;
            private float newChatX;
            private float searchX;
            private float buttonY;
            private static final float BUTTON_W = 20f;
            private static final float BUTTON_H = 20f;
            private static final float BUTTON_GAP = 4f;
            private static final float SPRITE_INSET = 4f;

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
                buttonY = ly + 2f;
                searchX = lx + lw - BUTTON_W;
                newChatX = searchX - BUTTON_GAP - BUTTON_W;
                addFriendX = newChatX - BUTTON_GAP - BUTTON_W;
            }

            @Override
            protected void renderSelf(GuiRenderer graphics, UiFrameContext frame, float deltaSeconds) {
        float mouseX = frame.pointerX();
        float mouseY = frame.pointerY();
                float textY = y() + 3f;
                graphics.drawText(Component.translatable("fascinatedutils.social.title").getString(),
                        x(), textY, FascinatedGuiTheme.INSTANCE.textPrimary(), true, false);

                boolean addHovered = inside(mouseX, mouseY, addFriendX, buttonY);
                boolean newChatHovered = inside(mouseX, mouseY, newChatX, buttonY);
                boolean searchHovered = inside(mouseX, mouseY, searchX, buttonY);
                boolean searchOn = props.searchActive().getAsBoolean();

                drawAccentIconButton(graphics, addFriendX, buttonY, addHovered, "+");
                drawSurfaceSpriteButton(graphics, newChatX, buttonY, newChatHovered, ModUiTextures.CHEVRON_RIGHT.getId());
                drawSearchButton(graphics, searchX, buttonY, searchHovered, searchOn);

                if (addHovered) {
                    WTooltip.draw(graphics, mouseX, mouseY,
                            Component.translatable("fascinatedutils.social.header_tooltip_add_friend").getString());
                } else if (newChatHovered) {
                    WTooltip.draw(graphics, mouseX, mouseY,
                            Component.translatable("fascinatedutils.social.header_tooltip_new_chat").getString());
                } else if (searchHovered) {
                    WTooltip.draw(graphics, mouseX, mouseY,
                            Component.translatable("fascinatedutils.social.header_tooltip_search").getString());
                }
            }

            private void drawAccentIconButton(GuiRenderer graphics, float buttonX, float iconY, boolean hovered, String icon) {
                int fill = hovered ? UITheme.COLOR_ACCENT_HOVER : UITheme.COLOR_ACCENT;
                int border = hovered ? UITheme.COLOR_ACCENT_HOVER : UITheme.COLOR_ACCENT;
                graphics.fillRoundedRectFrame(buttonX, iconY, BUTTON_W, BUTTON_H, 4f, border, fill, 1f, 1f, RectCornerRoundMask.ALL);
                graphics.drawCenteredText(icon, buttonX + BUTTON_W / 2f, iconY + (BUTTON_H - graphics.getFontCapHeight()) / 2f,
                        0xFFFFFFFF, false, false);
            }

            private void drawSurfaceSpriteButton(GuiRenderer graphics, float buttonX, float iconY, boolean hovered,
                    Identifier spriteId) {
                int fill = hovered ? 0xFF2A2F3E : 0xFF1E2230;
                int border = hovered ? 0xFF6C7098 : UITheme.COLOR_BORDER;
                graphics.fillRoundedRectFrame(buttonX, iconY, BUTTON_W, BUTTON_H, 4f, border, fill, 1f, 1f, RectCornerRoundMask.ALL);
                float inner = BUTTON_W - 2f * SPRITE_INSET;
                graphics.drawSprite(spriteId, buttonX + SPRITE_INSET, iconY + SPRITE_INSET, inner, inner, 0xFFFFFFFF);
            }

            private void drawSearchButton(GuiRenderer graphics, float buttonX, float iconY, boolean hovered, boolean active) {
                int fill = active ? 0xFF2B3142 : hovered ? 0xFF2A2F3E : 0xFF1E2230;
                int border = active ? UITheme.COLOR_BORDER_FOCUS : hovered ? 0xFF6C7098 : UITheme.COLOR_BORDER;
                graphics.fillRoundedRectFrame(buttonX, iconY, BUTTON_W, BUTTON_H, 4f, border, fill, 1f, 1f, RectCornerRoundMask.ALL);
                float inner = BUTTON_W - 2f * SPRITE_INSET;
                int tint = active ? 0xFFFFFFFF : 0xE0FFFFFF;
                graphics.drawSprite(ModUiTextures.SEARCH.getId(), buttonX + SPRITE_INSET, iconY + SPRITE_INSET, inner, inner, tint);
            }

            @Override
            public PointerHitKind pointerHitKind() {
                return PointerHitKind.TARGET;
            }

            @Override
            public UiPointerCursor pointerCursor(float pointerX, float pointerY) {
                if (inside(pointerX, pointerY, addFriendX, buttonY)
                        || inside(pointerX, pointerY, newChatX, buttonY)
                        || inside(pointerX, pointerY, searchX, buttonY)) {
                    return UiPointerCursor.HAND;
                }
                return UiPointerCursor.DEFAULT;
            }

            @Override
            public boolean mouseDown(float pointerX, float pointerY, int button) {
                if (button != 0) {
                    return false;
                }
                if (inside(pointerX, pointerY, addFriendX, buttonY)) {
                    props.onAddFriend().run();
                    return true;
                }
                if (inside(pointerX, pointerY, newChatX, buttonY)) {
                    props.onNewChat().run();
                    return true;
                }
                if (inside(pointerX, pointerY, searchX, buttonY)) {
                    props.onSearch().run();
                    return true;
                }
                return false;
            }

            private boolean inside(float pointerX, float pointerY, float buttonX, float iconY) {
                return pointerX >= buttonX && pointerX < buttonX + BUTTON_W
                        && pointerY >= iconY && pointerY < iconY + BUTTON_H;
            }
        };
    }
}
