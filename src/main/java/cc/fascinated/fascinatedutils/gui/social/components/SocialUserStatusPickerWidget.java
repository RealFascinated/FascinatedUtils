package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.user.UserStatus;
import cc.fascinated.fascinatedutils.gui.core.PointerHitKind;
import cc.fascinated.fascinatedutils.gui.core.UiFrameContext;
import cc.fascinated.fascinatedutils.gui.core.UiPointerCursor;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

public class SocialUserStatusPickerWidget extends FWidget {

    private static final float DOT = 8f;

    private final BooleanSupplier menuOpen;
    private final BooleanSupplier updatePending;
    private final Runnable onToggleMenu;

    public SocialUserStatusPickerWidget(BooleanSupplier menuOpen, BooleanSupplier updatePending, Runnable onToggleMenu) {
        this.menuOpen = menuOpen;
        this.updatePending = updatePending;
        this.onToggleMenu = onToggleMenu;
    }

    @Override
    public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
        setBounds(lx, ly, lw, lh);
    }

    @Override
    protected void renderSelf(GuiRenderer graphics, UiFrameContext frame, float deltaSeconds) {
        float mouseX = frame.pointerX();
        float mouseY = frame.pointerY();
        boolean interactive = !updatePending.getAsBoolean();
        int fillColor = menuOpen.getAsBoolean() ? 0xFF2B3142 : containsPoint(mouseX, mouseY) && interactive ? 0xFF242A38 : 0xFF202531;
        int borderColor = menuOpen.getAsBoolean() ? 0xFF6E7897 : containsPoint(mouseX, mouseY) && interactive ? 0xFF59617A : 0xFF454A60;
        graphics.fillRoundedRectFrame(x(), y(), w(), h(), 6f, borderColor, fillColor, 1f, 1f, RectCornerRoundMask.ALL);

        UserStatus userStatus = Alumite.INSTANCE.users().selfUser().preferredUserStatus();
        float dotX = x() + 8f;
        float dotY = y() + (h() - DOT) / 2f;
        graphics.fillRoundedRect(dotX, dotY, DOT, DOT, DOT / 2f, userStatus.color(), RectCornerRoundMask.ALL);

        String label = updatePending.getAsBoolean() ? Component.translatable("alumite.social.user_status.updating").getString() : userStatus.label();
        float textY = y() + (h() - graphics.getFontCapHeight()) / 2f;
        graphics.drawText(label, dotX + DOT + 6f, textY, updatePending.getAsBoolean() ? FascinatedGuiTheme.INSTANCE.textMuted() : FascinatedGuiTheme.INSTANCE.textPrimary(), false, false);

        if (!updatePending.getAsBoolean()) {
            graphics.drawText(menuOpen.getAsBoolean() ? "\u25B4" : "\u25BE", x() + w() - 12f, textY, FascinatedGuiTheme.INSTANCE.textMuted(), false, false);
        }
    }

    @Override
    public PointerHitKind pointerHitKind() {
        return PointerHitKind.TARGET;
    }

    @Override
    public UiPointerCursor pointerCursor(float pointerX, float pointerY) {
        return updatePending.getAsBoolean() ? UiPointerCursor.DEFAULT : UiPointerCursor.HAND;
    }

    @Override
    public boolean mouseDown(float pointerX, float pointerY, int button) {
        if (button != 0) {
            return false;
        }
        if (updatePending.getAsBoolean()) {
            return true;
        }
        onToggleMenu.run();
        return true;
    }
}
