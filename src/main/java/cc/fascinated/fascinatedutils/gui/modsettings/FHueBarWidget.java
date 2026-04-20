package cc.fascinated.fascinatedutils.gui.modsettings;

import cc.fascinated.fascinatedutils.common.color.SettingColor;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;

import java.util.function.Consumer;

/**
 * Interactive hue selection bar for the HSV color picker.
 *
 * <p>Renders a row of colour segments covering 0–360° hue. Handles its own drag state and reports
 * the updated hue value (in degrees, 0–360) to the parent via {@code onHueChanged}.
 */
public class FHueBarWidget extends FWidget {
    private final Consumer<Float> onHueChanged;
    private float hue;
    private boolean dragging;

    public FHueBarWidget(float initialHue, Consumer<Float> onHueChanged) {
        this.hue = initialHue;
        this.onHueChanged = onHueChanged;
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
            updateFromPointer(pointerX);
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
            updateFromPointer(pointerX);
            return true;
        }
        return false;
    }

    @Override
    protected void renderSelf(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
        int segments = 48;
        float segmentWidth = w() / segments;
        for (int segment = 0; segment < segments; segment++) {
            float segmentHue = (segment / (float) segments) * 360f;
            SettingColor segmentColor = SettingColor.fromHsv(segmentHue, 1f, 1f);
            int segmentArgb = segmentColor.getPackedArgb() | 0xFF000000;
            graphics.drawRect(x() + segment * segmentWidth, y(), segmentWidth + 0.5f, h(), segmentArgb);
        }

        float markerX = x() + (hue / 360f) * w();
        float markerW = 2f;
        graphics.drawRect(markerX - markerW * 0.5f, y() - 1f, markerW, h() + 2f, 0xFFFFFFFF);
        graphics.drawBorder(x(), y(), w(), h(), 1f, graphics.theme().border());
    }

    private void updateFromPointer(float pointerX) {
        hue = clamp01((pointerX - x()) / Math.max(1f, w())) * 360f;
        onHueChanged.accept(hue);
    }

    private static float clamp01(float val) {
        return Math.max(0f, Math.min(1f, val));
    }
}
