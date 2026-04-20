package cc.fascinated.fascinatedutils.gui.widgets;

import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import org.lwjgl.glfw.GLFW;

public abstract class FPopupWidget extends FWidget {
    private final Runnable onClose;
    private float dialogX;
    private float dialogY;
    private float dialogWidth;
    private float dialogHeight;

    protected FPopupWidget(Runnable onClose) {
        this.onClose = onClose;
    }

    @Override
    public boolean fillsVerticalInColumn() {
        return true;
    }

    @Override
    public boolean fillsHorizontalInRow() {
        return true;
    }

    @Override
    public boolean wantsPointer() {
        return true;
    }

    @Override
    public boolean mouseDown(float pointerX, float pointerY, int button) {
        if (button == 0 && !containsDialogPoint(pointerX, pointerY) && onClose != null) {
            onClose.run();
        }
        return false;
    }

    @Override
    public boolean click(float pointerX, float pointerY, int button) {
        return false;
    }

    @Override
    public boolean keyDownCapture(int keyCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && onClose != null) {
            onClose.run();
            return true;
        }
        return false;
    }

    protected void setDialogBounds(float layoutX, float layoutY, float layoutWidth, float layoutHeight, float width, float height) {
        setBounds(layoutX, layoutY, layoutWidth, layoutHeight);
        dialogWidth = width;
        dialogHeight = height;
        dialogX = layoutX + (layoutWidth - dialogWidth) * 0.5f;
        dialogY = layoutY + (layoutHeight - dialogHeight) * 0.5f;
    }

    protected float dialogX() {
        return dialogX;
    }

    protected float dialogY() {
        return dialogY;
    }

    protected float dialogWidth() {
        return dialogWidth;
    }

    protected float dialogHeight() {
        return dialogHeight;
    }

    @Override
    protected void renderSelf(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
        graphics.drawRect(x(), y(), w(), h(), 0x66000000);
        float cornerRadius = Math.max(0.5f, Math.min(8f, Math.min(dialogWidth, dialogHeight) * 0.5f - 0.01f));
        float borderThickness = 1f;
        graphics.fillRoundedRectFrame(dialogX, dialogY, dialogWidth, dialogHeight, cornerRadius, graphics.theme().border(), graphics.theme().surface(), borderThickness, borderThickness, RectCornerRoundMask.ALL);
    }

    private boolean containsDialogPoint(float pointerX, float pointerY) {
        return pointerX >= dialogX && pointerX <= dialogX + dialogWidth && pointerY >= dialogY && pointerY <= dialogY + dialogHeight;
    }
}