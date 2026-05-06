package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.user.UserStatus;
import cc.fascinated.fascinatedutils.gui.core.PointerHitKind;
import cc.fascinated.fascinatedutils.gui.core.UiFrameContext;
import cc.fascinated.fascinatedutils.gui.core.UiPointerCursor;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;

import java.util.function.Consumer;

public class SocialUserStatusMenuWidget extends FWidget {

    static final UserStatus[] SELECTABLE_STATUSES = {UserStatus.ONLINE, UserStatus.AWAY, UserStatus.DO_NOT_DISTURB, UserStatus.INVISIBLE};

    private static final float PAD = 6f;
    private static final float ROW_H = 30f;
    private static final float ROW_GAP = 4f;
    private static final float DOT = 8f;
    private static final float MIN_W = 176f;
    private static final float MAX_W = 220f;

    private final Consumer<UserStatus> onSelect;

    public SocialUserStatusMenuWidget(Consumer<UserStatus> onSelect) {
        this.onSelect = onSelect;
    }

    public float preferredHeight() {
        return PAD * 2f + SELECTABLE_STATUSES.length * ROW_H + Math.max(0, SELECTABLE_STATUSES.length - 1) * ROW_GAP;
    }

    public float preferredWidth(UIRenderer measure) {
        int widestText = 0;
        for (UserStatus status : SELECTABLE_STATUSES) {
            widestText = Math.max(widestText, measure.measureTextWidth(status.label(), false));
        }
        float checkmarkW = measure.measureTextWidth("\u2713", false);
        float contentW = 12f + DOT + 8f + widestText + 8f + checkmarkW + 12f;
        return Math.max(MIN_W, Math.min(MAX_W, contentW));
    }

    @Override
    public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
        setBounds(lx, ly, lw, lh);
    }

    @Override
    protected void renderSelf(GuiRenderer graphics, UiFrameContext frame, float deltaSeconds) {
        float mouseX = frame.pointerX();
        float mouseY = frame.pointerY();
        graphics.fillRoundedRectFrame(x(), y(), w(), h(), UITheme.CORNER_RADIUS_MD, 0xFF454A60, 0xFF171B24, 1f, 1f, RectCornerRoundMask.ALL);

        float checkmarkWidth = graphics.measureTextWidth("\u2713", false);
        float rowY = y() + PAD;
        UserStatus current = Alumite.INSTANCE.users().selfUser().preferredUserStatus();
        for (UserStatus status : SELECTABLE_STATUSES) {
            boolean hovered = mouseX >= x() + 4f && mouseX < x() + w() - 4f && mouseY >= rowY && mouseY < rowY + ROW_H;
            boolean selected = current == status;
            int rowColor = selected ? 0x334960C8 : hovered ? 0x22FFFFFF : 0x00000000;
            if (rowColor != 0) {
                graphics.fillRoundedRect(x() + 4f, rowY, w() - 8f, ROW_H, UITheme.CORNER_RADIUS_SM, rowColor, RectCornerRoundMask.ALL);
            }
            float dotX = x() + 12f;
            float dotY = rowY + (ROW_H - DOT) * 0.5f;
            graphics.fillRoundedRect(dotX, dotY, DOT, DOT, DOT / 2f, status.color(), RectCornerRoundMask.ALL);
            float textY = rowY + (ROW_H - graphics.getFontCapHeight()) * 0.5f;
            graphics.drawText(status.label(), dotX + DOT + 8f, textY, FascinatedGuiTheme.INSTANCE.textPrimary(), false, false);
            if (selected) {
                graphics.drawText("\u2713", x() + w() - 12f - checkmarkWidth, textY, 0xFF9DB4FF, false, false);
            }
            rowY += ROW_H + ROW_GAP;
        }
    }

    @Override
    public PointerHitKind pointerHitKind() {
        return PointerHitKind.TARGET;
    }

    @Override
    public UiPointerCursor pointerCursor(float pointerX, float pointerY) {
        return pointerX >= x() + 4f && pointerX < x() + w() - 4f
                && pointerY >= y() + PAD && pointerY < y() + h() - PAD ? UiPointerCursor.HAND : UiPointerCursor.DEFAULT;
    }

    @Override
    public boolean mouseDown(float pointerX, float pointerY, int button) {
        if (button != 0) {
            return false;
        }
        float rowY = y() + PAD;
        for (UserStatus status : SELECTABLE_STATUSES) {
            if (pointerX >= x() + 4f && pointerX < x() + w() - 4f && pointerY >= rowY && pointerY < rowY + ROW_H) {
                onSelect.accept(status);
                return true;
            }
            rowY += ROW_H + ROW_GAP;
        }
        return false;
    }
}
