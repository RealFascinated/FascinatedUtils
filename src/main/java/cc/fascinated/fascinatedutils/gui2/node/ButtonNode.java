package cc.fascinated.fascinatedutils.gui2.node;

import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;
import cc.fascinated.fascinatedutils.gui2.render.UiText;
import cc.fascinated.fascinatedutils.gui2.theme.UiTheme;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.function.Function;
import java.util.function.Supplier;

public class ButtonNode extends PositionedNode {

    public enum ButtonVariant {
        DEFAULT, GHOST, DANGER
    }

    private static final int ICON_SIZE = 12;
    private static final int ICON_PADDING = 4;
    private static final int ICON_TEXT_GAP = 6;
    private static final int CORNER_RADIUS = 4;
    private Supplier<String> labelSupplier;
    private Runnable onPress = () -> {};
    private boolean hovered;
    private boolean focused;
    private boolean rounded;
    private Supplier<ButtonVariant> variantSupplier = () -> ButtonVariant.DEFAULT;
    private boolean leftAlignLabel;
    private Integer labelColorArgb;
    private Function<UiTheme, Integer> labelColorResolver;
    private Identifier leftIcon;
    private Identifier rightIcon;
    private Identifier centerIcon;
    private Integer iconTintArgb;

    public ButtonNode(String label) {
        this.labelSupplier = label == null ? () -> "" : () -> label;
        size(120, 20);
    }

    public ButtonNode setLabel(String label) {
        this.labelSupplier = label == null ? () -> "" : () -> label;
        return this;
    }

    public ButtonNode setLabel(Supplier<String> labelSupplier) {
        this.labelSupplier = labelSupplier == null ? () -> "" : labelSupplier;
        return this;
    }

    public String label() {
        return labelSupplier.get();
    }

    public ButtonNode setOnPress(Runnable onPress) {
        this.onPress = onPress == null ? () -> {} : onPress;
        return this;
    }

    public ButtonNode setLeftIcon(Identifier leftIcon) {
        this.leftIcon = leftIcon;
        return this;
    }

    public ButtonNode setRightIcon(Identifier rightIcon) {
        this.rightIcon = rightIcon;
        return this;
    }

    public ButtonNode setIconTintArgb(int iconTintArgb) {
        this.iconTintArgb = iconTintArgb;
        return this;
    }

    public ButtonNode setLabelColorArgb(Integer labelColorArgb) {
        this.labelColorArgb = labelColorArgb;
        this.labelColorResolver = null;
        return this;
    }

    public ButtonNode setLabelColorResolver(Function<UiTheme, Integer> labelColorResolver) {
        this.labelColorResolver = labelColorResolver;
        this.labelColorArgb = null;
        return this;
    }

    public ButtonNode setRounded(boolean rounded) {
        this.rounded = rounded;
        return this;
    }

    public ButtonNode setVariant(ButtonVariant variant) {
        this.variantSupplier = () -> variant == null ? ButtonVariant.DEFAULT : variant;
        return this;
    }

    public ButtonNode setVariant(Supplier<ButtonVariant> variantSupplier) {
        this.variantSupplier = variantSupplier == null ? () -> ButtonVariant.DEFAULT : variantSupplier;
        return this;
    }

    public ButtonNode setLeftAlignLabel(boolean leftAlignLabel) {
        this.leftAlignLabel = leftAlignLabel;
        return this;
    }

    public ButtonNode setIconCenter(Identifier centerIcon) {
        this.centerIcon = centerIcon;
        return this;
    }

    public float minimumWidth(RenderFrame renderFrame) {
        int textWidth = renderFrame.measureTextWidth(label(), false);
        int leftWidth = leftIcon != null ? ICON_PADDING + ICON_SIZE + ICON_TEXT_GAP : ICON_PADDING;
        int rightWidth = rightIcon != null ? ICON_TEXT_GAP + ICON_SIZE + ICON_PADDING : ICON_PADDING;
        return textWidth + leftWidth + rightWidth;
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
        onPress.run();
        return true;
    }

    @Override
    public boolean onKeyPress(int keyCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_SPACE) {
            onPress.run();
            return true;
        }
        return false;
    }

    @Override
    protected void renderSelf(RenderFrame renderFrame, float deltaSeconds) {
        int bx = bounds().positionX();
        int by = bounds().positionY();
        int bw = bounds().width();
        int bh = bounds().height();
        int textColor = labelColorArgb != null ? labelColorArgb
                : labelColorResolver != null ? labelColorResolver.apply(renderFrame.theme())
                : renderFrame.theme().textPrimary();
        ButtonVariant resolvedVariant = variantSupplier.get();

        if (resolvedVariant == ButtonVariant.GHOST) {
            if (hovered || focused) {
                renderFrame.drawRoundedRect(bx, by, bw, bh, CORNER_RADIUS, renderFrame.theme().buttonFillHover());
            }
        } else {
            int fillColor;
            int borderColor;
            if (resolvedVariant == ButtonVariant.DANGER) {
                fillColor = hovered ? renderFrame.theme().dangerFillHover() : renderFrame.theme().dangerFill();
                borderColor = hovered ? renderFrame.theme().buttonBorderHover() : renderFrame.theme().buttonBorder();
            } else {
                fillColor = hovered ? renderFrame.theme().buttonFillHover() : renderFrame.theme().buttonFill();
                if (focused) {
                    borderColor = renderFrame.theme().buttonBorderFocus();
                } else if (hovered) {
                    borderColor = renderFrame.theme().buttonBorderHover();
                } else {
                    borderColor = renderFrame.theme().buttonBorder();
                }
            }
            if (rounded) {
                renderFrame.drawRoundedRect(bx, by, bw, bh, CORNER_RADIUS, fillColor);
                renderFrame.drawRoundedRectFrame(bx, by, bw, bh, CORNER_RADIUS, borderColor, 0, 1);
            } else {
                renderFrame.drawRect(bx, by, bw, bh, fillColor);
                renderFrame.drawBorder(bx, by, bw, bh, 1, borderColor);
            }
        }

        UiText labelText = UiText.of(label()).color(textColor);
        int textWidth = labelText.width(renderFrame);
        int iconY = by + (bh - ICON_SIZE) / 2;
        if (centerIcon != null) {
            int iconTint = iconTintArgb != null ? iconTintArgb : renderFrame.theme().textPrimary();
            renderFrame.drawTexture(centerIcon, bx + (bw - ICON_SIZE) / 2, iconY, ICON_SIZE, ICON_SIZE, iconTint);
        }
        if (leftIcon != null) {
            int iconTint = iconTintArgb != null ? iconTintArgb : renderFrame.theme().textPrimary();
            renderFrame.drawTexture(leftIcon, bx + ICON_PADDING, iconY, ICON_SIZE, ICON_SIZE, iconTint);
        }

        int leftTextInset = leftIcon != null ? ICON_PADDING + ICON_SIZE + ICON_TEXT_GAP : 0;
        int textAreaX = bx + leftTextInset;
        int textAreaWidth = Math.max(0, bw - leftTextInset);
        int textX = leftAlignLabel ? textAreaX + ICON_PADDING : textAreaX + (textAreaWidth - textWidth) / 2;
        int textY = by + (bh - renderFrame.fontHeight()) / 2;
        labelText.draw(renderFrame, textX, textY);

        if (rightIcon != null) {
            int iconTint = iconTintArgb != null ? iconTintArgb : renderFrame.theme().textPrimary();
            renderFrame.drawTexture(rightIcon, bx + bw - ICON_PADDING - ICON_SIZE, iconY, ICON_SIZE, ICON_SIZE, iconTint);
        }
    }
}
