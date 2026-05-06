package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.gui.core.UiFrameContext;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.widgets.FButtonWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FOutlinedTextInputWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class SocialAddFriendFooterWidget extends FWidget {

    private static final float ADD_BTN_W = 36f;
    private static final float BTN_H = 20f;

    private final FOutlinedTextInputWidget friendInput;
    private final FButtonWidget addBtn;

    public SocialAddFriendFooterWidget(FOutlinedTextInputWidget friendInput, Consumer<String> onAdd) {
        this.friendInput = friendInput;
        addBtn = new FButtonWidget(() -> {
            String username = friendInput.value().trim();
            if (username.isEmpty()) {
                return;
            }
            friendInput.setValue("");
            onAdd.accept(username);
        }, () -> Component.translatable("alumite.social.add_button").getString(), ADD_BTN_W, 1, 1f, 4f, 1f, 4f, 3f) {
            @Override
            protected int resolveButtonFillColorArgb(boolean hovered) {
                return hovered ? UITheme.COLOR_ACCENT_HOVER : UITheme.COLOR_ACCENT;
            }

            @Override
            protected int resolveButtonBorderColorArgb(boolean hovered) {
                return hovered ? UITheme.COLOR_ACCENT_HOVER : UITheme.COLOR_ACCENT;
            }
        };
        addChild(friendInput);
        addChild(addBtn);
    }

    @Override
    public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
        setBounds(lx, ly, lw, lh);
        float inputW = lw - ADD_BTN_W - 4f;
        float inputH = friendInput.intrinsicHeightForColumn(measure, inputW);
        friendInput.layout(measure, lx, ly + (lh - inputH) / 2f, inputW, inputH);
        addBtn.layout(measure, lx + inputW + 4f, ly + (lh - BTN_H) / 2f, ADD_BTN_W, BTN_H);
    }

    @Override
    protected void renderSelf(GuiRenderer graphics, UiFrameContext frame, float deltaSeconds) {
    }
}
