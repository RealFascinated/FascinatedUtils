package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.gui.core.PointerHitKind;
import cc.fascinated.fascinatedutils.gui.core.UiFrameContext;
import cc.fascinated.fascinatedutils.gui.core.UiPointerCursor;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FBadgeWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;

import java.util.function.BooleanSupplier;

public class SocialModeTabsWidget {

    public static FWidget build(Props props) {
        int count = props.incomingRequestCount();
        FBadgeWidget incomingBadge = new FBadgeWidget(5f, 0xFFCC2222);
        incomingBadge.setVisible(count > 0);
        if (count > 0) {
            incomingBadge.setText(count > 9 ? "9+" : String.valueOf(count));
        }
        return new FWidget() {
            {
                addChild(incomingBadge);
            }

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
                if (count > 0) {
                    incomingBadge.layout(measure, lx + lw - 13f, ly + 2f, 10f, 10f);
                }
            }

            @Override
            protected void renderSelf(GuiRenderer graphics, UiFrameContext frame, float deltaSeconds) {
                float mouseX = frame.pointerX();
                float mouseY = frame.pointerY();
                float halfW = w() / 2f;
                boolean chatSelected = props.chatActive().getAsBoolean();
                boolean chatHovered = mouseX >= x() && mouseX < x() + halfW && mouseY >= y() && mouseY < y() + h();
                boolean friendsHovered = mouseX >= x() + halfW && mouseX < x() + w() && mouseY >= y() && mouseY < y() + h();
                int chatBg = chatSelected ? 0x447C5CBF : (chatHovered ? 0x22FFFFFF : 0x00000000);
                int friendsBg = !chatSelected ? 0x447C5CBF : (friendsHovered ? 0x22FFFFFF : 0x00000000);
                graphics.drawRect(x(), y(), halfW, h(), chatBg);
                graphics.drawRect(x() + halfW, y(), halfW, h(), friendsBg);
                float underlineH = 2f;
                if (chatSelected) {
                    graphics.fillRoundedRect(x() + 8f, y() + h() - underlineH, halfW - 16f, underlineH, underlineH / 2f, UITheme.COLOR_ACCENT, RectCornerRoundMask.ALL);
                }
                else {
                    graphics.fillRoundedRect(x() + halfW + 8f, y() + h() - underlineH, halfW - 16f, underlineH, underlineH / 2f, UITheme.COLOR_ACCENT, RectCornerRoundMask.ALL);
                }
                int chatTextColor = chatSelected ? FascinatedGuiTheme.INSTANCE.textPrimary() : FascinatedGuiTheme.INSTANCE.textMuted();
                int friendsTextColor = chatSelected ? FascinatedGuiTheme.INSTANCE.textMuted() : FascinatedGuiTheme.INSTANCE.textPrimary();
                float textY = y() + (h() - graphics.getFontCapHeight()) / 2f;
                graphics.drawCenteredText(props.chatLabel(), x() + halfW / 2f, textY, chatTextColor, false, false);
                graphics.drawCenteredText(props.friendsLabel(), x() + halfW + halfW / 2f, textY, friendsTextColor, false, false);


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
                if (button != 0) {
                    return false;
                }
                float halfW = w() / 2f;
                if (pointerX < x() + halfW) {
                    props.onSelectChat().run();
                }
                else {
                    props.onSelectFriends().run();
                }
                return true;
            }
        };
    }

    public record Props(String chatLabel, String friendsLabel, int incomingRequestCount, BooleanSupplier chatActive,
                        Runnable onSelectChat, Runnable onSelectFriends) {}
}
