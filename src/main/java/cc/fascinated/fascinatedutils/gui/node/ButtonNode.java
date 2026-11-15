package cc.fascinated.fascinatedutils.gui.node;

import cc.fascinated.fascinatedutils.common.Colors;
import cc.fascinated.fascinatedutils.gui.anim.AnimHandle;
import cc.fascinated.fascinatedutils.gui.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui.core.UiKeys;
import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.gui.render.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.render.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.render.UiText;
import cc.fascinated.fascinatedutils.gui.theme.UiMetrics;
import cc.fascinated.fascinatedutils.gui.theme.UiTheme;
import net.minecraft.resources.Identifier;

import java.util.function.Function;
import java.util.function.Supplier;

public class ButtonNode extends PositionedNode<ButtonNode> {

    public enum ButtonVariant {
        DEFAULT, PRIMARY, GHOST, DANGER, SUCCESS
    }

    private static final int ICON_SIZE = 12;
    private static final int ICON_PADDING = UiMetrics.SPACE_SM;
    private static final int ICON_TEXT_GAP = 6;
    private static final int CORNER_RADIUS = UiMetrics.RADIUS;
    private static final int FOCUS_RING_GAP = 1;
    static final int FOCUS_RING_THICKNESS = 2;
    static final int FOCUS_RING_OFFSET = FOCUS_RING_GAP + FOCUS_RING_THICKNESS;
    private Supplier<String> labelSupplier;
    private Runnable onPress = () -> {};
    private Supplier<Boolean> disabledSupplier = () -> false;
    private final AnimHandle hoverAnim = new AnimHandle(0f);
    private boolean hovered;
    private boolean focused;
    private int cornerMask = RectCornerRoundMask.ALL;
    private Supplier<ButtonVariant> variantSupplier = () -> ButtonVariant.DEFAULT;
    private boolean leftAlignLabel;
    private Integer labelColorArgb;
    private Function<UiTheme, Integer> labelColorResolver;
    private Identifier leftIcon;
    private Identifier rightIcon;
    private Identifier centerIcon;
    private int iconSize = ICON_SIZE;

    public ButtonNode() {
        this.labelSupplier = () -> "";
    }

    public ButtonNode(Supplier<String> labelSupplier) {
        this.labelSupplier = labelSupplier == null ? () -> "" : labelSupplier;
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

    public ButtonNode setDisabled(Supplier<Boolean> disabled) {
        this.disabledSupplier = disabled == null ? () -> false : disabled;
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

    /**
     * Chooses which corners are rounded, so stacked buttons can stay square where they touch.
     *
     * @param cornerMask a {@link RectCornerRoundMask} value
     * @return this button
     */
    public ButtonNode setCornerMask(int cornerMask) {
        this.cornerMask = cornerMask;
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

    public ButtonNode setIconSize(int iconSize) {
        this.iconSize = Math.max(1, iconSize);
        return this;
    }

    @Override
    protected int intrinsicWidth(GuiRenderer renderFrame, int parentWidth) {
        return Math.max((int) minimumWidth(renderFrame), intrinsicHeight(renderFrame, parentWidth, 0));
    }

    @Override
    protected int intrinsicHeight(GuiRenderer renderFrame, int parentHeight, int resolvedWidth) {
        return UiMetrics.CONTROL_HEIGHT;
    }

    public float minimumWidth(GuiRenderer renderFrame) {
        int textWidth = renderFrame.measureTextWidth(label(), false);
        int leftWidth = leftIcon != null ? ICON_PADDING + iconSize + ICON_TEXT_GAP : ICON_PADDING;
        int rightWidth = rightIcon != null ? ICON_TEXT_GAP + iconSize + ICON_PADDING : ICON_PADDING;
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
        if (button != 0 || disabledSupplier.get()) {
            return false;
        }
        onPress.run();
        return true;
    }

    @Override
    public boolean onKeyPress(int keyCode, int modifiers) {
        if (disabledSupplier.get()) {
            return false;
        }
        if (UiKeys.isActivationKey(keyCode)) {
            onPress.run();
            return true;
        }
        return false;
    }

    @Override
    protected void inheritAnimationState(UiNode previous) {
        hoverAnim.snap(((ButtonNode) previous).hoverAnim.value());
    }

    @Override
    protected void renderSelf(GuiRenderer renderFrame, float deltaSeconds) {
        int bx = bounds().positionX();
        int by = bounds().positionY();
        int bw = bounds().width();
        int bh = bounds().height();
        int cornerRadius = cornerMask == RectCornerRoundMask.NONE ? 0 : CORNER_RADIUS;
        boolean isDisabled = disabledSupplier.get();
        UiTheme theme = renderFrame.theme();
        ButtonVariant resolvedVariant = variantSupplier.get();
        boolean active = !isDisabled && hovered;
        hoverAnim.target(active || (focused && !isDisabled) ? 1f : 0f);
        hoverAnim.tick(deltaSeconds);
        float hover = hoverAnim.value();
        int defaultTextColor = switch (resolvedVariant) {
            case PRIMARY -> theme.onAccent();
            case DANGER, SUCCESS -> theme.onDanger();
            default -> theme.textPrimary();
        };
        int textColor = isDisabled ? theme.textSubtle()
                : labelColorArgb != null ? labelColorArgb
                : labelColorResolver != null ? labelColorResolver.apply(theme)
                : defaultTextColor;

        if (isDisabled) {
            if (resolvedVariant != ButtonVariant.GHOST) {
                renderFrame.fillRoundedRect(bx, by, bw, bh, cornerRadius, theme.surfaceCard(), cornerMask);
                renderFrame.fillRoundedRectFrame(bx, by, bw, bh, cornerRadius, theme.borderSubtle(), 0, 1, 1, cornerMask);
            }
        } else if (resolvedVariant == ButtonVariant.GHOST) {
            if (hover > 0.01f) {
                renderFrame.fillRoundedRect(bx, by, bw, bh, cornerRadius, Colors.mixArgb(hover, theme.surfaceRaised() & 0x00FFFFFF, theme.surfaceRaised()), cornerMask);
            }
        } else if (resolvedVariant == ButtonVariant.PRIMARY) {
            renderFrame.fillRoundedRect(bx, by, bw, bh, cornerRadius, Colors.mixArgb(hover, theme.accent(), theme.accentHover()), cornerMask);
        } else if (resolvedVariant == ButtonVariant.SUCCESS) {
            renderFrame.fillRoundedRect(bx, by, bw, bh, cornerRadius, Colors.mixArgb(hover, theme.successFill(), theme.successFillHover()), cornerMask);
        } else if (resolvedVariant == ButtonVariant.DANGER) {
            renderFrame.fillRoundedRect(bx, by, bw, bh, cornerRadius, Colors.mixArgb(hover, theme.dangerFill(), theme.dangerFillHover()), cornerMask);
        } else {
            renderFrame.fillRoundedRect(bx, by, bw, bh, cornerRadius, Colors.mixArgb(hover, theme.buttonFill(), theme.buttonFillHover()), cornerMask);
            renderFrame.fillRoundedRectFrame(bx, by, bw, bh, cornerRadius, Colors.mixArgb(hover, theme.buttonBorder(), theme.buttonBorderHover()), 0, 1, 1, cornerMask);
        }
        if (focused && !isDisabled) {
            renderFrame.drawRoundedRectFrame(bx - FOCUS_RING_OFFSET, by - FOCUS_RING_OFFSET, bw + FOCUS_RING_OFFSET * 2, bh + FOCUS_RING_OFFSET * 2, cornerRadius + FOCUS_RING_OFFSET, theme.focusRing(), 0, FOCUS_RING_THICKNESS);
        }

        UiText labelText = UiText.of(label()).color(textColor);
        int textWidth = labelText.width(renderFrame);
        int iconY = by + (bh - iconSize) / 2;
        if (centerIcon != null) {
            int iconTint = renderFrame.theme().textPrimary();
            renderFrame.drawTexture(centerIcon, bx + (bw - iconSize) / 2, iconY, iconSize, iconSize, iconTint);
        }
        if (leftIcon != null) {
            int iconTint = renderFrame.theme().textPrimary();
            renderFrame.drawTexture(leftIcon, bx + ICON_PADDING, iconY, iconSize, iconSize, iconTint);
        }

        int leftTextInset = leftIcon != null ? ICON_PADDING + iconSize + ICON_TEXT_GAP : 0;
        int textAreaX = bx + leftTextInset;
        int textAreaWidth = Math.max(0, bw - leftTextInset);
        int textX;
        if (leftIcon != null) {
            textX = textAreaX;
        } else if (leftAlignLabel || rightIcon != null) {
            textX = textAreaX + ICON_PADDING;
        } else {
            textX = textAreaX + (textAreaWidth - textWidth) / 2;
        }
        int textY = by + (bh - renderFrame.fontHeight()) / 2;
        labelText.draw(renderFrame, textX, textY);

        if (rightIcon != null) {
            int iconTint = renderFrame.theme().textPrimary();
            renderFrame.drawTexture(rightIcon, bx + bw - ICON_PADDING - iconSize, iconY, iconSize, iconSize, iconTint);
        }
    }
}
