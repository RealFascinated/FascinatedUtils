package cc.fascinated.fascinatedutils.gui2.node;

import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class CheckboxNode extends PositionedNode<CheckboxNode> {

    private static final int BOX_SIZE = 12;
    private static final int CORNER_RADIUS = 3;
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
    protected int intrinsicHeight(RenderFrame renderFrame, int parentHeight, int resolvedWidth) {
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
    protected void renderSelf(RenderFrame renderFrame, float deltaSeconds) {
        int bx = bounds().positionX();
        int by = bounds().positionY();
        int bh = bounds().height();

        int boxY = by + (bh - BOX_SIZE) / 2;

        if (checked) {
            renderFrame.drawRoundedRect(bx, boxY, BOX_SIZE, BOX_SIZE, CORNER_RADIUS, renderFrame.theme().accent());
            renderFrame.drawTexture(ModUiTextures.CHECK.getId(), bx + 1, boxY + 1, BOX_SIZE - 2, BOX_SIZE - 2, 0xFFFFFFFF);
        } else {
            int borderColor = hovered || focused ? renderFrame.theme().buttonBorderHover() : renderFrame.theme().inputBorder();
            renderFrame.drawRoundedRectFrame(bx, boxY, BOX_SIZE, BOX_SIZE, CORNER_RADIUS, borderColor, renderFrame.theme().inputFill(), 1);
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
