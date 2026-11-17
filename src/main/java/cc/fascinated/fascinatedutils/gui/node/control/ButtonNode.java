package cc.fascinated.fascinatedutils.gui.node.control;

import cc.fascinated.fascinatedutils.gui.node.base.InteractiveNode;

import cc.fascinated.fascinatedutils.common.color.Colors;
import cc.fascinated.fascinatedutils.gui.anim.AnimHandle;
import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
import cc.fascinated.fascinatedutils.gui.render.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.theme.UiMetrics;
import cc.fascinated.fascinatedutils.gui.theme.UiTheme;
import net.minecraft.resources.Identifier;

import java.util.function.Supplier;
import java.util.function.ToIntFunction;

public class ButtonNode extends InteractiveNode<ButtonNode> {

    public enum ButtonVariant {
        DEFAULT, PRIMARY, GHOST, DANGER, SUCCESS
    }

    private static final int ICON_SIZE = 12;
    private static final int ICON_PADDING = UiMetrics.SPACE_SM;
    private static final int CORNER_RADIUS = UiMetrics.RADIUS;

    private Supplier<String> labelSupplier;
    private Runnable onPress = () -> {};
    private final AnimHandle hoverAnim = new AnimHandle(0f);
    private int cornerMask = RectCornerRoundMask.ALL;
    private Supplier<ButtonVariant> variantSupplier = () -> ButtonVariant.DEFAULT;
    private boolean leftAlignLabel;
    private ToIntFunction<UiTheme> labelColorResolver;
    private Identifier leftIcon;
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
        String label = labelSupplier.get();
        return label == null ? "" : label;
    }

    public ButtonNode setOnPress(Runnable onPress) {
        this.onPress = onPress == null ? () -> {} : onPress;
        return this;
    }

    public ButtonNode setLeftIcon(Identifier leftIcon) {
        this.leftIcon = leftIcon;
        return this;
    }

    /**
     * Sets a theme-dependent label colour.
     *
     * @param labelColorResolver resolves the label colour from the active theme; null restores the variant default
     * @return this button
     */
    public ButtonNode setLabelColorResolver(ToIntFunction<UiTheme> labelColorResolver) {
        this.labelColorResolver = labelColorResolver;
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
    protected int intrinsicWidth(AlumiteGuiRenderer renderFrame, int parentWidth) {
        return Math.max(minimumWidth(renderFrame), intrinsicHeight(renderFrame, parentWidth, 0));
    }

    @Override
    protected int intrinsicHeight(AlumiteGuiRenderer renderFrame, int parentHeight, int resolvedWidth) {
        return UiMetrics.CONTROL_HEIGHT;
    }

    /**
     * The narrowest width that still fits the label and icons.
     *
     * @param renderFrame the active renderer, used to measure the label
     * @return the minimum width in pixels
     */
    public int minimumWidth(AlumiteGuiRenderer renderFrame) {
        int leftWidth = leftIcon != null ? ICON_PADDING + iconSize + UiMetrics.ICON_GAP : ICON_PADDING;
        return renderFrame.measureTextWidth(label(), false) + leftWidth + ICON_PADDING;
    }

    @Override
    protected boolean activate() {
        onPress.run();
        return true;
    }

    @Override
    protected void inheritAnimationState(UiNode previous) {
        hoverAnim.snap(((ButtonNode) previous).hoverAnim.value());
    }

    @Override
    protected void renderSelf(AlumiteGuiRenderer renderFrame, float deltaSeconds) {
        int positionX = bounds().positionX();
        int positionY = bounds().positionY();
        int width = bounds().width();
        int height = bounds().height();
        int cornerRadius = cornerMask == RectCornerRoundMask.NONE ? 0 : CORNER_RADIUS;
        boolean disabled = isDisabled();
        UiTheme theme = renderFrame.theme();
        ButtonVariant variant = variantSupplier.get();
        hoverAnim.target(!disabled && (isHovered() || isFocusVisible()) ? 1f : 0f);
        hoverAnim.tick(deltaSeconds);
        float hover = hoverAnim.value();
        int defaultTextColor = switch (variant) {
            case PRIMARY -> theme.onAccent();
            case DANGER -> theme.onDanger();
            case SUCCESS -> theme.onSuccess();
            default -> theme.textPrimary();
        };
        int textColor = disabled ? theme.textSubtle() : labelColorResolver != null ? labelColorResolver.applyAsInt(theme) : defaultTextColor;

        if (disabled) {
            if (variant != ButtonVariant.GHOST) {
                renderFrame.fillRoundedRect(positionX, positionY, width, height, cornerRadius, theme.surfaceCard(), cornerMask);
                renderFrame.fillRoundedRectFrame(positionX, positionY, width, height, cornerRadius, theme.borderSubtle(), 0, 1, 1, cornerMask);
            }
        } else if (variant == ButtonVariant.GHOST) {
            if (hover > 0.01f) {
                renderFrame.fillRoundedRect(positionX, positionY, width, height, cornerRadius, Colors.mixArgb(hover, theme.surfaceRaised() & 0x00FFFFFF, theme.surfaceRaised()), cornerMask);
            }
        } else if (variant == ButtonVariant.PRIMARY) {
            renderFrame.fillRoundedRect(positionX, positionY, width, height, cornerRadius, Colors.mixArgb(hover, theme.accent(), theme.accentHover()), cornerMask);
        } else if (variant == ButtonVariant.SUCCESS) {
            renderFrame.fillRoundedRect(positionX, positionY, width, height, cornerRadius, Colors.mixArgb(hover, theme.successFill(), theme.successFillHover()), cornerMask);
        } else if (variant == ButtonVariant.DANGER) {
            renderFrame.fillRoundedRect(positionX, positionY, width, height, cornerRadius, Colors.mixArgb(hover, theme.dangerFill(), theme.dangerFillHover()), cornerMask);
        } else {
            renderFrame.fillRoundedRect(positionX, positionY, width, height, cornerRadius, Colors.mixArgb(hover, theme.buttonFill(), theme.buttonFillHover()), cornerMask);
            renderFrame.fillRoundedRectFrame(positionX, positionY, width, height, cornerRadius, Colors.mixArgb(hover, theme.buttonBorder(), theme.buttonBorderHover()), 0, 1, 1, cornerMask);
        }
        drawFocusRing(renderFrame, positionX, positionY, width, height, cornerRadius);

        String label = label();
        int iconY = positionY + (height - iconSize) / 2;
        if (centerIcon != null) {
            renderFrame.drawTexture(centerIcon, positionX + (width - iconSize) / 2, iconY, iconSize, iconSize, textColor);
        }
        if (leftIcon != null) {
            renderFrame.drawTexture(leftIcon, positionX + ICON_PADDING, iconY, iconSize, iconSize, textColor);
        }
        if (label.isEmpty()) {
            return;
        }
        int textWidth = renderFrame.measureTextWidth(label, false);
        int leftTextInset = leftIcon != null ? ICON_PADDING + iconSize + UiMetrics.ICON_GAP : 0;
        int textAreaWidth = Math.max(0, width - leftTextInset);
        int textX;
        if (leftIcon != null) {
            textX = positionX + leftTextInset;
        } else if (leftAlignLabel) {
            textX = positionX + ICON_PADDING;
        } else {
            textX = positionX + (textAreaWidth - textWidth) / 2;
        }
        renderFrame.drawText(label, textX, positionY + (height - renderFrame.fontHeight()) / 2, textColor, false, false);
    }
}
