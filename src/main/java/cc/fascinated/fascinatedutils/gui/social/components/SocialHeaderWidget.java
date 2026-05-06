package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.gui.core.PointerHitKind;
import cc.fascinated.fascinatedutils.gui.core.UiFrameContext;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FIconButtonWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import cc.fascinated.fascinatedutils.gui.widgets.WTooltip;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

public class SocialHeaderWidget {
    public static FWidget build(Props props) {
        return new FWidget() {
            private static final float BUTTON_SIZE = 20f;
            private static final float BUTTON_GAP = 4f;
            private static final float SPRITE_INSET = 4f;

            final FIconButtonWidget addFriendBtn = createAddFriendBtn();
            final FIconButtonWidget newChatBtn = createNewChatBtn();
            final FIconButtonWidget searchBtn = createSearchBtn();

            {
                addChild(addFriendBtn);
                addChild(newChatBtn);
                addChild(searchBtn);
            }

            FIconButtonWidget createAddFriendBtn() {
                FIconButtonWidget btn = new FIconButtonWidget(BUTTON_SIZE, 4f, () -> "+") {
                    @Override
                    protected int resolveButtonFillArgb(boolean hovered) {
                        return hovered ? UITheme.COLOR_ACCENT_HOVER : UITheme.COLOR_ACCENT;
                    }

                    @Override
                    protected int resolveButtonBorderArgb(boolean hovered) {
                        return hovered ? UITheme.COLOR_ACCENT_HOVER : UITheme.COLOR_ACCENT;
                    }

                    @Override
                    protected int resolveContentTintArgb(boolean hovered) {
                        return 0xFFFFFFFF;
                    }
                };
                btn.setOnClick(props.onAddFriend());
                return btn;
            }

            FIconButtonWidget createNewChatBtn() {
                FIconButtonWidget btn = new FIconButtonWidget(BUTTON_SIZE, SPRITE_INSET, 4f, ModUiTextures.CHEVRON_RIGHT::getId, true) {
                    @Override
                    protected int resolveButtonFillArgb(boolean hovered) {
                        return hovered ? 0xFF2A2F3E : 0xFF1E2230;
                    }

                    @Override
                    protected int resolveButtonBorderArgb(boolean hovered) {
                        return hovered ? 0xFF6C7098 : UITheme.COLOR_BORDER;
                    }

                    @Override
                    protected int resolveContentTintArgb(boolean hovered) {
                        return 0xFFFFFFFF;
                    }
                };
                btn.setOnClick(props.onNewChat());
                return btn;
            }

            FIconButtonWidget createSearchBtn() {
                FIconButtonWidget btn = new FIconButtonWidget(BUTTON_SIZE, SPRITE_INSET, 4f, ModUiTextures.SEARCH::getId, true) {
                    @Override
                    protected int resolveButtonFillArgb(boolean hovered) {
                        boolean active = props.searchActive().getAsBoolean();
                        return active ? 0xFF2B3142 : hovered ? 0xFF2A2F3E : 0xFF1E2230;
                    }

                    @Override
                    protected int resolveButtonBorderArgb(boolean hovered) {
                        boolean active = props.searchActive().getAsBoolean();
                        return active ? UITheme.COLOR_BORDER_FOCUS : hovered ? 0xFF6C7098 : UITheme.COLOR_BORDER;
                    }

                    @Override
                    protected int resolveContentTintArgb(boolean hovered) {
                        return props.searchActive().getAsBoolean() ? 0xFFFFFFFF : 0xE0FFFFFF;
                    }
                };
                btn.setOnClick(props.onSearch());
                return btn;
            }

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
                float buttonY = ly + 2f;
                float searchX = lx + lw - BUTTON_SIZE;
                float newChatX = searchX - BUTTON_GAP - BUTTON_SIZE;
                float addFriendX = newChatX - BUTTON_GAP - BUTTON_SIZE;
                searchBtn.layout(measure, searchX, buttonY, BUTTON_SIZE, BUTTON_SIZE);
                newChatBtn.layout(measure, newChatX, buttonY, BUTTON_SIZE, BUTTON_SIZE);
                addFriendBtn.layout(measure, addFriendX, buttonY, BUTTON_SIZE, BUTTON_SIZE);
            }

            @Override
            protected void renderSelf(GuiRenderer graphics, UiFrameContext frame, float deltaSeconds) {
                graphics.drawText(Component.translatable("alumite.social.title").getString(), x(), y() + 3f, FascinatedGuiTheme.INSTANCE.textPrimary(), true, false);
            }

            @Override
            public void renderOverlayAfterChildren(GuiRenderer graphics, UiFrameContext frame, float deltaSeconds) {
                float mouseX = frame.pointerX();
                float mouseY = frame.pointerY();
                if (addFriendBtn.containsPoint(mouseX, mouseY)) {
                    WTooltip.draw(graphics, mouseX, mouseY, Component.translatable("alumite.social.header_tooltip_add_friend").getString());
                } else if (newChatBtn.containsPoint(mouseX, mouseY)) {
                    WTooltip.draw(graphics, mouseX, mouseY, Component.translatable("alumite.social.header_tooltip_new_chat").getString());
                } else if (searchBtn.containsPoint(mouseX, mouseY)) {
                    WTooltip.draw(graphics, mouseX, mouseY, Component.translatable("alumite.social.header_tooltip_search").getString());
                }
            }

            @Override
            public PointerHitKind pointerHitKind() {
                return PointerHitKind.TARGET;
            }
        };
    }

    public record Props(Runnable onAddFriend, Runnable onNewChat, Runnable onSearch, BooleanSupplier searchActive) {}
}
