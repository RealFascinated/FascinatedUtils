package cc.fascinated.fascinatedutils.gui.modsettings;

import cc.fascinated.fascinatedutils.common.color.SettingColor;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;

import java.util.function.BiConsumer;

/**
 * Interactive saturation/value gradient area for the HSV color picker.
 *
 * <p>Renders a two-dimensional gradient: saturation increases left-to-right and value increases
 * bottom-to-top, blended against the current hue. Handles its own drag state and reports the
 * updated {@code (saturation, value)} pair to the parent via {@code onSvChanged}.
 */
public class FSvPickerAreaWidget extends FWidget {
    private final BiConsumer<Float, Float> onSvChanged;
    private float hue;
    private float saturation;
    private float value;
    private boolean dragging;

    public FSvPickerAreaWidget(float initialHue, float initialSaturation, float initialValue, BiConsumer<Float, Float> onSvChanged) {
        this.hue = initialHue;
        this.saturation = initialSaturation;
        this.value = initialValue;
        this.onSvChanged = onSvChanged;
    }

    public void setHue(float hue) {
        this.hue = hue;
    }

    public void setSaturationValue(float saturation, float value) {
        this.saturation = saturation;
        this.value = value;
    }

    @Override
    public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
        setBounds(layoutX, layoutY, layoutWidth, layoutHeight);
    }

    @Override
    public boolean wantsPointer() {
        return true;
    }

    @Override
    public boolean mouseDown(float pointerX, float pointerY, int button) {
        if (button == 0) {
            dragging = true;
            updateFromPointer(pointerX, pointerY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseUp(float pointerX, float pointerY, int button) {
        if (button == 0) {
            dragging = false;
        }
        return false;
    }

    @Override
    public boolean mouseMove(float pointerX, float pointerY) {
        if (dragging) {
            updateFromPointer(pointerX, pointerY);
            return true;
        }
        return false;
    }

    @Override
    protected void renderSelf(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
        int columns = 48;
        float columnWidth = w() / columns;
        for (int column = 0; column < columns; column++) {
            float normalizedSaturation = column / (float) (columns - 1);
            SettingColor topColor = SettingColor.fromHsv(hue, normalizedSaturation, 1f);
            int topArgb = topColor.getPackedArgb() | 0xFF000000;
            int bottomArgb = 0xFF000000;
            graphics.fillGradientVertical(x() + column * columnWidth, y(), columnWidth + 0.5f, h(), topArgb, bottomArgb);
        }

        float crossX = x() + saturation * w();
        float crossY = y() + (1f - value) * h();
        float outerR = 4f;
        float innerR = 3f;
        graphics.fillRoundedRect(crossX - outerR, crossY - outerR, outerR * 2f, outerR * 2f, outerR, 0xFF000000, RectCornerRoundMask.ALL);
        graphics.fillRoundedRect(crossX - innerR, crossY - innerR, innerR * 2f, innerR * 2f, innerR, 0xFFFFFFFF, RectCornerRoundMask.ALL);
        graphics.drawBorder(x(), y(), w(), h(), 1f, graphics.theme().border());
    }

    private void updateFromPointer(float pointerX, float pointerY) {
        saturation = clamp01((pointerX - x()) / Math.max(1f, w()));
        value = 1f - clamp01((pointerY - y()) / Math.max(1f, h()));
        onSvChanged.accept(saturation, value);
    }

    private static float clamp01(float val) {
        return Math.max(0f, Math.min(1f, val));
    }
}
