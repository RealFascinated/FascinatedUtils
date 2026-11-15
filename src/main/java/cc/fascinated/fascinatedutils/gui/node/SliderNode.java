package cc.fascinated.fascinatedutils.gui.node;

import cc.fascinated.fascinatedutils.common.Colors;
import cc.fascinated.fascinatedutils.gui.anim.AnimHandle;
import cc.fascinated.fascinatedutils.gui.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class SliderNode extends PositionedNode<SliderNode> {

    private static final int TRACK_HEIGHT = 4;
    private static final int THUMB_SIZE = 10;
    private static final int DEFAULT_STEP_COUNT = 100;
    private static final float SHIFT_STEP_MULTIPLIER = 10f;

    private final float min;
    private final float max;
    private final Supplier<Float> valueSupplier;
    private final Consumer<Float> onChange;
    private Runnable onCommit = () -> {};
    private Supplier<Boolean> disabled = () -> false;
    private final AnimHandle activeAnim = new AnimHandle(0f);
    private boolean dragging;
    private boolean hovered;
    private boolean focused;
    private float step;

    /**
     * Creates a horizontal slider.
     *
     * @param min           lowest selectable value
     * @param max           highest selectable value
     * @param valueSupplier supplies the current value each frame
     * @param onChange      receives the raw (unsnapped) value while the pointer is pressed
     */
    public SliderNode(float min, float max, Supplier<Float> valueSupplier, Consumer<Float> onChange) {
        this.min = min;
        this.max = max;
        this.valueSupplier = valueSupplier;
        this.onChange = onChange;
        this.step = (max - min) / DEFAULT_STEP_COUNT;
    }

    /**
     * Sets the amount a single arrow key press changes the value by.
     *
     * @param step the keyboard step, ignored when not positive
     * @return this slider
     */
    public SliderNode setStep(float step) {
        if (step > 0f) {
            this.step = step;
        }
        return this;
    }

    public SliderNode setOnCommit(Runnable onCommit) {
        this.onCommit = onCommit == null ? () -> {} : onCommit;
        return this;
    }

    public SliderNode setDisabled(Supplier<Boolean> disabled) {
        this.disabled = disabled == null ? () -> false : disabled;
        return this;
    }

    @Override
    protected void inheritAnimationState(UiNode previous) {
        activeAnim.snap(((SliderNode) previous).activeAnim.value());
    }

    @Override
    protected int intrinsicHeight(AlumiteGuiRenderer renderFrame, int parentHeight, int resolvedWidth) {
        return THUMB_SIZE + 2;
    }

    @Override
    public boolean blocksHitWhenEmpty() {
        return true;
    }

    @Override
    public boolean focusable() {
        return !disabled.get();
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
    public boolean onKeyPress(int keyCode, int modifiers) {
        if (disabled.get()) {
            return false;
        }
        float multiplier = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0 ? SHIFT_STEP_MULTIPLIER : 1f;
        float nextValue;
        switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_DOWN -> nextValue = valueSupplier.get() - step * multiplier;
            case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_UP -> nextValue = valueSupplier.get() + step * multiplier;
            case GLFW.GLFW_KEY_HOME -> nextValue = min;
            case GLFW.GLFW_KEY_END -> nextValue = max;
            default -> {
                return false;
            }
        }
        onChange.accept(Mth.clamp(nextValue, min, max));
        onCommit.run();
        return true;
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
    public boolean onPointerPress(float pointerX, float pointerY, int button) {
        if (button != 0 || disabled.get()) {
            return false;
        }
        dragging = true;
        applyPointer(pointerX);
        return true;
    }

    @Override
    public boolean onPointerMove(float pointerX, float pointerY) {
        if (!dragging) {
            return false;
        }
        applyPointer(pointerX);
        return true;
    }

    @Override
    public boolean onPointerRelease(float pointerX, float pointerY, int button) {
        if (button == 0 && dragging) {
            dragging = false;
            onCommit.run();
        }
        return false;
    }

    private void applyPointer(float pointerX) {
        float usableWidth = Math.max(1, bounds().width() - THUMB_SIZE);
        float fraction = Mth.clamp((pointerX - bounds().positionX() - THUMB_SIZE / 2f) / usableWidth, 0f, 1f);
        onChange.accept(min + fraction * (max - min));
    }

    @Override
    protected void renderSelf(AlumiteGuiRenderer renderFrame, float deltaSeconds) {
        int positionX = bounds().positionX();
        int positionY = bounds().positionY();
        int width = bounds().width();
        int height = bounds().height();
        boolean isDisabled = disabled.get();

        float range = max - min;
        float fraction = range <= 0f ? 0f : Mth.clamp((valueSupplier.get() - min) / range, 0f, 1f);
        int usableWidth = Math.max(1, width - THUMB_SIZE);
        int thumbX = positionX + Math.round(fraction * usableWidth);
        int trackY = positionY + (height - TRACK_HEIGHT) / 2;
        int thumbY = positionY + (height - THUMB_SIZE) / 2;
        int accent = isDisabled ? renderFrame.theme().textSubtle() : renderFrame.theme().accent();

        renderFrame.drawRoundedRect(positionX, trackY, width, TRACK_HEIGHT, TRACK_HEIGHT / 2, renderFrame.theme().inputFill());
        renderFrame.drawRoundedRect(positionX, trackY, thumbX - positionX + THUMB_SIZE / 2, TRACK_HEIGHT, TRACK_HEIGHT / 2, accent);
        activeAnim.target((dragging || hovered) && !isDisabled ? 1f : 0f);
        activeAnim.tick(deltaSeconds);
        float active = activeAnim.value();
        int thumbColor = Colors.mixArgb(active, renderFrame.theme().textPrimary(), renderFrame.theme().buttonBorderHover());
        float thumbSize = THUMB_SIZE + 3f * active + (dragging ? 1f : 0f);
        float thumbOffset = (THUMB_SIZE - thumbSize) / 2f;
        if (focused && !isDisabled) {
            int ringOffset = ButtonNode.FOCUS_RING_OFFSET;
            renderFrame.drawRoundedRectFrame(positionX - ringOffset, positionY - ringOffset, width + ringOffset * 2, height + ringOffset * 2, height / 2f + ringOffset, renderFrame.theme().focusRing(), 0, ButtonNode.FOCUS_RING_THICKNESS);
        }
        renderFrame.drawRoundedRect(thumbX + thumbOffset, thumbY + thumbOffset, thumbSize, thumbSize, thumbSize / 2f, isDisabled ? renderFrame.theme().textSubtle() : thumbColor);
    }
}
