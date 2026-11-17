package cc.fascinated.fascinatedutils.gui.node.control;

import cc.fascinated.fascinatedutils.gui.node.base.InteractiveNode;

import cc.fascinated.fascinatedutils.common.color.Colors;
import cc.fascinated.fascinatedutils.gui.anim.AnimHandle;
import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

public class SliderNode extends InteractiveNode<SliderNode> {

    private static final int TRACK_HEIGHT = 4;
    private static final int THUMB_SIZE = 10;
    // Pixels the thumb grows by when hovered (3) and pressed (1).
    private static final int MAX_THUMB_GROWTH = 4;
    private static final int DEFAULT_STEP_COUNT = 100;
    private static final double SHIFT_STEP_MULTIPLIER = 10.0;

    private final double min;
    private final double max;
    private final DoubleSupplier valueSupplier;
    private final DoubleConsumer onChange;
    private Runnable onCommit = () -> {};
    private final AnimHandle activeAnim = new AnimHandle(0f);
    private double step;

    /**
     * Creates a horizontal slider.
     *
     * @param min           lowest selectable value
     * @param max           highest selectable value
     * @param valueSupplier supplies the current value each frame
     * @param onChange      receives the value, snapped to the step, whenever the user changes it
     */
    public SliderNode(double min, double max, DoubleSupplier valueSupplier, DoubleConsumer onChange) {
        this.min = min;
        this.max = max;
        this.valueSupplier = valueSupplier;
        this.onChange = onChange;
        this.step = (max - min) / DEFAULT_STEP_COUNT;
    }

    /**
     * Sets the granularity values snap to and the amount a single arrow key press changes the value by.
     *
     * @param step the step, ignored when not positive
     * @return this slider
     */
    public SliderNode setStep(double step) {
        if (step > 0.0) {
            this.step = step;
        }
        return this;
    }

    public SliderNode setOnCommit(Runnable onCommit) {
        this.onCommit = onCommit == null ? () -> {} : onCommit;
        return this;
    }

    @Override
    protected void inheritAnimationState(UiNode previous) {
        activeAnim.snap(((SliderNode) previous).activeAnim.value());
    }

    @Override
    protected int intrinsicHeight(AlumiteGuiRenderer renderFrame, int parentHeight, int resolvedWidth) {
        return THUMB_SIZE + MAX_THUMB_GROWTH + 2 * FOCUS_RING_OFFSET;
    }

    @Override
    public boolean onKeyPress(int keyCode, int modifiers) {
        if (isDisabled()) {
            return false;
        }
        double multiplier = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0 ? SHIFT_STEP_MULTIPLIER : 1.0;
        double nextValue;
        switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_DOWN -> nextValue = valueSupplier.getAsDouble() - step * multiplier;
            case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_UP -> nextValue = valueSupplier.getAsDouble() + step * multiplier;
            case GLFW.GLFW_KEY_HOME -> nextValue = min;
            case GLFW.GLFW_KEY_END -> nextValue = max;
            default -> {
                return false;
            }
        }
        onChange.accept(snap(nextValue));
        onCommit.run();
        return true;
    }

    @Override
    public boolean onPointerPress(float pointerX, float pointerY, int button) {
        if (button != 0 || isDisabled()) {
            return false;
        }
        applyPointer(pointerX);
        return true;
    }

    @Override
    public boolean onPointerMove(float pointerX, float pointerY) {
        if (!isPressed()) {
            return false;
        }
        applyPointer(pointerX);
        return true;
    }

    @Override
    public boolean onPointerRelease(float pointerX, float pointerY, int button) {
        if (button == 0 && isPressed()) {
            onCommit.run();
        }
        return false;
    }

    private void applyPointer(float pointerX) {
        float usableWidth = Math.max(1, bounds().width() - THUMB_SIZE);
        float fraction = Mth.clamp((pointerX - bounds().positionX() - THUMB_SIZE / 2f) / usableWidth, 0f, 1f);
        onChange.accept(snap(min + fraction * (max - min)));
    }

    /**
     * Rounds a value to the nearest step counted from the minimum and clamps it to the range, so repeated key steps
     * never accumulate floating point error.
     */
    private double snap(double rawValue) {
        double steps = Math.round((Math.min(max, Math.max(min, rawValue)) - min) / step);
        return Math.min(max, Math.max(min, min + steps * step));
    }

    @Override
    protected void renderSelf(AlumiteGuiRenderer renderFrame, float deltaSeconds) {
        int positionX = bounds().positionX();
        int positionY = bounds().positionY();
        int width = bounds().width();
        int height = bounds().height();
        boolean disabled = isDisabled();

        double range = max - min;
        float fraction = range <= 0.0 ? 0f : Mth.clamp((float) ((valueSupplier.getAsDouble() - min) / range), 0f, 1f);
        int usableWidth = Math.max(1, width - THUMB_SIZE);
        int thumbX = positionX + Math.round(fraction * usableWidth);
        int trackY = positionY + (height - TRACK_HEIGHT) / 2;
        int thumbY = positionY + (height - THUMB_SIZE) / 2;
        int accent = disabled ? renderFrame.theme().textSubtle() : renderFrame.theme().accent();

        renderFrame.drawRoundedRect(positionX, trackY, width, TRACK_HEIGHT, TRACK_HEIGHT / 2f, renderFrame.theme().inputFill());
        renderFrame.drawRoundedRect(positionX, trackY, thumbX - positionX + THUMB_SIZE / 2f, TRACK_HEIGHT, TRACK_HEIGHT / 2f, accent);
        activeAnim.target((isPressed() || isHovered()) && !disabled ? 1f : 0f);
        activeAnim.tick(deltaSeconds);
        float active = activeAnim.value();
        int thumbColor = Colors.mixArgb(active, renderFrame.theme().textPrimary(), renderFrame.theme().buttonBorderHover());
        float thumbSize = THUMB_SIZE + 3f * active + (isPressed() ? 1f : 0f);
        float thumbOffset = (THUMB_SIZE - thumbSize) / 2f;
        drawFocusRing(renderFrame, positionX, positionY, width, height, height / 2f);
        renderFrame.drawRoundedRect(thumbX + thumbOffset, thumbY + thumbOffset, thumbSize, thumbSize, thumbSize / 2f, disabled ? renderFrame.theme().textSubtle() : thumbColor);
    }
}
