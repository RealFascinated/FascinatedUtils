package cc.fascinated.fascinatedutils.gui.node.control;

import cc.fascinated.fascinatedutils.gui.node.base.ToggleNode;

import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.common.color.Colors;
import cc.fascinated.fascinatedutils.gui.anim.Easing;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UiMetrics;
import cc.fascinated.fascinatedutils.gui.theme.UiTheme;

import java.util.function.Supplier;

/**
 * A checkbox with an optional label, for choices that are confirmed later. Use {@link SwitchNode} for settings that
 * take effect immediately.
 */
public class CheckboxNode extends ToggleNode<CheckboxNode> {

    private static final int BOX_SIZE = 14;
    private static final float CHECK_SPEED = 22f;

    private Supplier<String> labelSupplier = () -> "";

    public CheckboxNode setLabel(String label) {
        this.labelSupplier = label == null ? () -> "" : () -> label;
        return this;
    }

    public CheckboxNode setLabel(Supplier<String> labelSupplier) {
        this.labelSupplier = labelSupplier == null ? () -> "" : labelSupplier;
        return this;
    }

    @Override
    protected int intrinsicWidth(AlumiteGuiRenderer renderFrame, int parentWidth) {
        String label = labelSupplier.get();
        return label.isEmpty() ? BOX_SIZE : BOX_SIZE + UiMetrics.ICON_GAP + renderFrame.measureTextWidth(label, false);
    }

    @Override
    protected int intrinsicHeight(AlumiteGuiRenderer renderFrame, int parentHeight, int resolvedWidth) {
        return Math.max(BOX_SIZE, renderFrame.fontHeight());
    }

    @Override
    protected void renderSelf(AlumiteGuiRenderer renderFrame, float deltaSeconds) {
        int boxX = bounds().positionX();
        int boxY = bounds().positionY() + (bounds().height() - BOX_SIZE) / 2;
        UiTheme theme = renderFrame.theme();
        boolean disabled = isDisabled();
        float hover = tickHover(deltaSeconds);
        float check = tickChecked(deltaSeconds, CHECK_SPEED);

        int borderColor = disabled ? theme.borderSubtle() : Colors.mixArgb(hover, theme.borderStrong(), theme.buttonBorderHover());
        renderFrame.drawRoundedRectFrame(boxX, boxY, BOX_SIZE, BOX_SIZE, UiMetrics.CHECKBOX_RADIUS, borderColor, theme.inputFill(), 1);
        if (check > 0.01f) {
            float fillSize = BOX_SIZE * Easing.easeOutCubic(check);
            float fillOffset = (BOX_SIZE - fillSize) / 2f;
            int fillColor = disabled ? theme.textSubtle() : Colors.mixArgb(hover, theme.accent(), theme.accentHover());
            renderFrame.drawRoundedRect(boxX + fillOffset, boxY + fillOffset, fillSize, fillSize, UiMetrics.CHECKBOX_RADIUS * check, fillColor);
            float checkSize = (BOX_SIZE - 2) * Easing.easeOutBack(check);
            float checkOffset = (BOX_SIZE - checkSize) / 2f;
            int checkColor = Colors.mixArgb(check, theme.onAccent() & 0x00FFFFFF, theme.onAccent());
            renderFrame.drawTexture(ModUiTextures.CHECK.getId(), boxX + checkOffset, boxY + checkOffset, checkSize, checkSize, checkColor);
        }
        drawFocusRing(renderFrame, boxX, boxY, BOX_SIZE, BOX_SIZE, UiMetrics.CHECKBOX_RADIUS);

        String label = labelSupplier.get();
        if (!label.isEmpty()) {
            int textY = bounds().positionY() + (bounds().height() - renderFrame.fontHeight()) / 2;
            renderFrame.drawText(label, boxX + BOX_SIZE + UiMetrics.ICON_GAP, textY, disabled ? theme.textSubtle() : theme.textPrimary(), false, false);
        }
    }
}
