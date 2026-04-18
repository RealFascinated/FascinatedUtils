package cc.fascinated.fascinatedutils.gui.widgets.settings;

import cc.fascinated.fascinatedutils.gui.GuiDesignSpace;
import cc.fascinated.fascinatedutils.gui.core.UiPointerCursor;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.Icons;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import net.minecraft.util.Mth;

public class FBackIconButtonWidget extends FWidget {
    private final Runnable onActivate;
    private final float buttonWidth;
    private final float buttonHeight;
    private boolean hovered;

    public FBackIconButtonWidget(Runnable onActivate, float buttonWidth, float buttonHeight) {
        this.onActivate = onActivate == null ? () -> {
        } : onActivate;
        this.buttonWidth = Math.max(1f, buttonWidth);
        this.buttonHeight = Math.max(1f, buttonHeight);
    }

    @Override
    public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
        setBounds(layoutX, layoutY, layoutWidth, layoutHeight);
    }

    @Override
    public float intrinsicWidthForRow(UIRenderer measure, float heightBudget) {
        return buttonWidth;
    }

    @Override
    public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
        return buttonHeight;
    }

    @Override
    public boolean wantsPointer() {
        return true;
    }

    @Override
    public UiPointerCursor pointerCursor(float pointerX, float pointerY) {
        return UiPointerCursor.HAND;
    }

    @Override
    public boolean mouseEnter(float pointerX, float pointerY) {
        hovered = true;
        return false;
    }

    @Override
    public boolean mouseLeave(float pointerX, float pointerY) {
        hovered = false;
        return false;
    }

    @Override
    public boolean click(float pointerX, float pointerY, int button) {
        if (button == 0) {
            onActivate.run();
            return true;
        }
        return false;
    }

    @Override
    protected void renderSelf(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
        int fillColor = hovered ? graphics.theme().moduleListRowHover() : graphics.theme().surface();
        int borderColor = hovered ? graphics.theme().borderHover() : graphics.theme().border();
        int iconColor = hovered ? graphics.theme().textPrimary() : graphics.theme().textMuted();
        float borderThickness = GuiDesignSpace.pxUniform(UITheme.BORDER_THICKNESS_PX);
        int drawX = Mth.floor(x());
        int drawY = Mth.floor(y());
        int size = Math.max(1, Math.min(Mth.floor(w()), Mth.floor(h())));
        float drawSize = size;
        float cornerRadius = drawSize * 0.5f - borderThickness * 0.5f - 0.01f;
        graphics.fillRoundedRectFrame(drawX, drawY, drawSize, drawSize, Math.max(0.5f, cornerRadius), borderColor, fillColor, borderThickness, borderThickness, RectCornerRoundMask.ALL);
        Icons.paintModSettingsBackIcon(graphics, drawX, drawY, drawSize, drawSize, iconColor);
    }
}
