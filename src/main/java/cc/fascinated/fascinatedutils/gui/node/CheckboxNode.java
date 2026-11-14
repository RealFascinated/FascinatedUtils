package cc.fascinated.fascinatedutils.gui.node;

import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.common.Colors;
import cc.fascinated.fascinatedutils.gui.anim.AnimHandle;
import cc.fascinated.fascinatedutils.gui.anim.Easing;
import cc.fascinated.fascinatedutils.gui.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.gui.render.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UiTheme;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class CheckboxNode extends PositionedNode<CheckboxNode> {

    private static final int BOX_SIZE = 14;
    private static final int CORNER_RADIUS = 4;
    private static final int LABEL_GAP = 6;

    private final AnimHandle hoverAnim = new AnimHandle(0f);
    private final AnimHandle checkAnim = new AnimHandle(Float.NaN);
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
    protected void inheritAnimationState(UiNode previous) {
        CheckboxNode other = (CheckboxNode) previous;
        hoverAnim.snap(other.hoverAnim.value());
        checkAnim.snap(other.checkAnim.value());
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
        hoverAnim.target(hovered ? 1f : 0f);
        hoverAnim.tick(deltaSeconds);
        if (Float.isNaN(checkAnim.value())) {
            checkAnim.snap(checked ? 1f : 0f);
        }
        checkAnim.target(checked ? 1f : 0f);
        checkAnim.tick(deltaSeconds);
        float hover = hoverAnim.value();
        float check = checkAnim.value();
        int borderColor = Colors.mixArgb(hover, theme.borderStrong(), theme.buttonBorderHover());
        renderFrame.drawRoundedRectFrame(bx, boxY, BOX_SIZE, BOX_SIZE, CORNER_RADIUS, borderColor, theme.inputFill(), 1);
        if (check > 0.01f) {
            float fillSize = BOX_SIZE * Easing.easeOutCubic(check);
            float fillOffset = (BOX_SIZE - fillSize) / 2f;
            int fillColor = Colors.mixArgb(hover, theme.accent(), theme.accentHover());
            renderFrame.drawRoundedRect(bx + fillOffset, boxY + fillOffset, fillSize, fillSize, CORNER_RADIUS * check, fillColor);
            float checkSize = (BOX_SIZE - 2) * Easing.easeOutBack(check);
            float checkOffset = (BOX_SIZE - checkSize) / 2f;
            int checkColor = Colors.mixArgb(check, theme.onAccent() & 0x00FFFFFF, theme.onAccent());
            renderFrame.drawTexture(ModUiTextures.CHECK.getId(), bx + checkOffset, boxY + checkOffset, checkSize, checkSize, checkColor);
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
