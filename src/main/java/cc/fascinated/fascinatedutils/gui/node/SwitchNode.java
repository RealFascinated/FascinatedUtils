package cc.fascinated.fascinatedutils.gui.node;

import cc.fascinated.fascinatedutils.common.Colors;
import cc.fascinated.fascinatedutils.gui.anim.AnimHandle;
import cc.fascinated.fascinatedutils.gui.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.gui.render.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UiTheme;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

/**
 * An on/off switch for settings that take effect immediately. Use {@link CheckboxNode} for choices that are only
 * confirmed later, such as a list selection.
 */
public class SwitchNode extends PositionedNode<SwitchNode> {
    public static final int TRACK_WIDTH = 30;
    public static final int TRACK_HEIGHT = 16;

    private static final int KNOB_INSET = 2;
    private static final float SLIDE_SPEED = 14f;
    private static final float KNOB_STRETCH = 5f;

    private final AnimHandle hoverAnim = new AnimHandle(0f);

    private boolean checked;
    private boolean hovered;
    private boolean focused;
    private float knobProgress = Float.NaN;
    private Consumer<Boolean> onChange = ignored -> {};

    public SwitchNode setChecked(boolean checked) {
        this.checked = checked;
        return this;
    }

    public boolean isChecked() {
        return checked;
    }

    public SwitchNode setOnChange(Consumer<Boolean> onChange) {
        this.onChange = onChange == null ? ignored -> {} : onChange;
        return this;
    }

    @Override
    protected void inheritAnimationState(UiNode previous) {
        SwitchNode other = (SwitchNode) previous;
        knobProgress = other.knobProgress;
        hoverAnim.snap(other.hoverAnim.value());
    }

    @Override
    protected int intrinsicWidth(GuiRenderer renderFrame, int parentWidth) {
        return TRACK_WIDTH;
    }

    @Override
    protected int intrinsicHeight(GuiRenderer renderFrame, int parentHeight, int resolvedWidth) {
        return TRACK_HEIGHT;
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
        if (button != 0) {
            return false;
        }
        toggle();
        return true;
    }

    @Override
    public boolean onKeyPress(int keyCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_SPACE) {
            toggle();
            return true;
        }
        return false;
    }

    private void toggle() {
        checked = !checked;
        onChange.accept(checked);
    }

    @Override
    protected void renderSelf(GuiRenderer renderFrame, float deltaSeconds) {
        UiTheme theme = renderFrame.theme();
        float target = checked ? 1f : 0f;
        if (Float.isNaN(knobProgress)) {
            knobProgress = target;
        } else {
            float step = Math.min(1f, SLIDE_SPEED * deltaSeconds);
            knobProgress += (target - knobProgress) * step;
            if (Math.abs(target - knobProgress) < 0.01f) {
                knobProgress = target;
            }
        }
        hoverAnim.target(hovered ? 1f : 0f);
        hoverAnim.tick(deltaSeconds);
        float hover = hoverAnim.value();
        float travelStretch = 1f - Math.abs(2f * knobProgress - 1f);
        int trackX = bounds().positionX() + (bounds().width() - TRACK_WIDTH) / 2;
        int trackY = bounds().positionY() + (bounds().height() - TRACK_HEIGHT) / 2;
        int radius = TRACK_HEIGHT / 2;
        int offTrack = Colors.mixArgb(hover, theme.surfaceRaised(), theme.surfaceActive());
        int onTrack = Colors.mixArgb(hover, theme.accent(), theme.accentHover());
        renderFrame.drawRoundedRect(trackX, trackY, TRACK_WIDTH, TRACK_HEIGHT, radius, Colors.mixArgb(knobProgress, offTrack, onTrack));
        if (knobProgress < 1f) {
            int borderColor = Colors.mixArgb(knobProgress, theme.borderStrong(), theme.borderStrong() & 0x00FFFFFF);
            renderFrame.drawRoundedRectFrame(trackX, trackY, TRACK_WIDTH, TRACK_HEIGHT, radius, borderColor, 0, 1);
        }
        int knobSize = TRACK_HEIGHT - KNOB_INSET * 2;
        float knobWidth = knobSize + KNOB_STRETCH * travelStretch;
        float knobTravel = TRACK_WIDTH - KNOB_INSET * 2 - knobSize;
        float knobX = trackX + KNOB_INSET + (knobTravel - (knobWidth - knobSize)) * knobProgress;
        float knobY = trackY + (TRACK_HEIGHT - knobSize) / 2f;
        renderFrame.drawRoundedRect(knobX, knobY, knobWidth, knobSize, knobSize / 2f, Colors.mixArgb(knobProgress, theme.textMuted(), theme.onAccent()));
        if (focused) {
            renderFrame.drawRoundedRectFrame(trackX - 3, trackY - 3, TRACK_WIDTH + 6, TRACK_HEIGHT + 6, radius + 3, theme.focusRing(), 0, 2);
        }
    }
}
