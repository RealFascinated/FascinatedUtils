package cc.fascinated.fascinatedutils.oldgui.widgets;

import cc.fascinated.fascinatedutils.oldgui.core.PointerHitKind;
import cc.fascinated.fascinatedutils.oldgui.core.UiFrameContext;
import cc.fascinated.fascinatedutils.oldgui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.oldgui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.oldgui.renderer.UIRenderer;
import lombok.Setter;

@Setter
public class FRectWidget extends FWidget {
    private int fillColorArgb;
    private float cornerRadius;
    private int cornerRoundMask = RectCornerRoundMask.ALL;
    private Integer borderColorArgb;
    private float borderThickness = 1f;

    public void setBorder(Integer borderColorArgb, float borderThickness) {
        this.borderColorArgb = borderColorArgb;
        this.borderThickness = borderThickness;
    }

    public void clearBorder() {
        this.borderColorArgb = null;
    }

    @Override
    public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
        setBounds(layoutX, layoutY, layoutWidth, layoutHeight);
    }

    @Override
    public PointerHitKind pointerHitKind() {
        return PointerHitKind.BLOCK;
    }

    @Override
    protected void renderSelf(GuiRenderer graphics, UiFrameContext frame, float deltaSeconds) {
        float mouseX = frame.pointerX();
        float mouseY = frame.pointerY();
        if (cornerRadius > 0.5f && borderColorArgb != null) {
            graphics.fillRoundedRectFrame(x(), y(), w(), h(), cornerRadius, borderColorArgb, fillColorArgb, borderThickness, borderThickness, cornerRoundMask);
        }
        else if (cornerRadius > 0.5f) {
            graphics.fillRoundedRect(x(), y(), w(), h(), cornerRadius, fillColorArgb, cornerRoundMask);
        }
        else {
            graphics.drawRect(x(), y(), w(), h(), fillColorArgb);
            if (borderColorArgb != null) {
                graphics.drawBorder(x(), y(), w(), h(), borderThickness, borderColorArgb);
            }
        }
    }
}
