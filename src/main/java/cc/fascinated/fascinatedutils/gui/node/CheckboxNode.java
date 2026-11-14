package cc.fascinated.fascinatedutils.gui.node;

import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.gui.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui.render.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UiTheme;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class CheckboxNode extends PositionedNode<CheckboxNode> {

    private static final int BOX_SIZE = 14;
    private static final int CORNER_RADIUS = 4;
    private static final int LABEL_GAP = 6;

    private boolean checked;
    private boolean hovered;
    private boolean focused;
    private Supplier<String> labelSupplier;
    private Consumer<Boolean> onChange = ignored -> {};

    public CheckboxNode setChecked(boolean checked) {
        this.checked = checked;
        return this;
    }

    public boolean isChecked() {
        return checked;
    }

    public CheckboxNode setLabel(String label) {
        this.labelSupplier = label == null ? () -> "" : () -> label;
        return this;
    }

    public CheckboxNode setLabel(Supplier<String> labelSupplier) {
        this.labelSupplier = labelSupplier == null ? () -> "" : labelSupplier;
        return this;
    }

    public CheckboxNode setOnChange(Consumer<Boolean> onChange) {
        this.onChange = onChange == null ? ignored -> {} : onChange;
        return this;
    }

    @Override
    protected int intrinsicHeight(GuiRenderer renderFrame, int parentHeight, int resolvedWidth) {
        return Math.max(BOX_SIZE, renderFrame.fontHeight());
    }

    @Override
    public boolean blocksHitWhenEmpty() {
        return true;
    }

    @Override
    public boolean focusable() {
        return true;
    }

    @Override
    public void onFocusGained() {
        focused = true;
    }

    @Override
    public void onFocusLost() {
        focused = false;
    }

    @Override
    public boolean onPointerEnter(float pointerX, float pointerY) {
        hovered = true;
        return false;
    }

    @Override
    public boolean onPointerLeave(float pointerX, float pointerY) {
        hovered = false;
        return false;
    }

    @Override
    public boolean onClick(float pointerX, float pointerY, int button) {
        if (button != 0) return false;
        checked = !checked;
        onChange.accept(checked);
        return true;
    }

    @Override
    public boolean onKeyPress(int keyCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_SPACE) {
            checked = !checked;
            onChange.accept(checked);
            return true;
        }
        return false;
    }

    @Override
    protected void renderSelf(GuiRenderer renderFrame, float deltaSeconds) {
        int bx = bounds().positionX();
        int by = bounds().positionY();
        int bh = bounds().height();

        int boxY = by + (bh - BOX_SIZE) / 2;

        UiTheme theme = renderFrame.theme();
        if (checked) {
            renderFrame.drawRoundedRect(bx, boxY, BOX_SIZE, BOX_SIZE, CORNER_RADIUS, hovered ? theme.accentHover() : theme.accent());
            renderFrame.drawTexture(ModUiTextures.CHECK.getId(), bx + 1, boxY + 1, BOX_SIZE - 2, BOX_SIZE - 2, theme.onAccent());
        } else {
            int borderColor = hovered ? theme.buttonBorderHover() : theme.borderStrong();
            renderFrame.drawRoundedRectFrame(bx, boxY, BOX_SIZE, BOX_SIZE, CORNER_RADIUS, borderColor, theme.inputFill(), 1);
        }
        if (focused) {
            renderFrame.drawRoundedRectFrame(bx - 3, boxY - 3, BOX_SIZE + 6, BOX_SIZE + 6, CORNER_RADIUS + 3, theme.focusRing(), 0, 2);
        }

        if (labelSupplier != null) {
            String label = labelSupplier.get();
            if (!label.isEmpty()) {
                int textY = by + (bh - renderFrame.fontHeight()) / 2;
                renderFrame.drawText(label, bx + BOX_SIZE + LABEL_GAP, textY, renderFrame.theme().textPrimary(), false, false);
            }
        }
    }
}
