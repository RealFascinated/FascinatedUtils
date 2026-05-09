package cc.fascinated.fascinatedutils.oldgui.widgets;

import cc.fascinated.fascinatedutils.oldgui.core.PointerHitKind;
import cc.fascinated.fascinatedutils.oldgui.core.UiFrameContext;
import cc.fascinated.fascinatedutils.oldgui.core.UiPointerCursor;
import cc.fascinated.fascinatedutils.oldgui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.oldgui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.oldgui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.oldgui.theme.UITheme;
import cc.fascinated.fascinatedutils.oldgui.themes.FascinatedGuiTheme;
import net.minecraft.resources.Identifier;

import java.util.function.Supplier;

/**
 * A fixed-size square button containing either a centered sprite/texture icon or a centered text glyph.
 *
 * <p>Override {@link #resolveButtonFillArgb}, {@link #resolveButtonBorderArgb}, and
 * {@link #resolveContentTintArgb} to apply custom hover and active colors. The default
 * colors follow the themed surface/border palette used by the rest of the mod UI.
 */
public class FIconButtonWidget extends FWidget {

    private final float buttonSize;
    private final float iconInset;
    private final float cornerRadius;
    private final Supplier<Identifier> iconSupplier;
    private final boolean isSprite;
    private final Supplier<String> labelSupplier;
    private Runnable onClick = () -> {};
    private boolean drawBorder = true;

    /**
     * Icon mode — renders a sprite ({@code isSprite=true}) or standalone texture ({@code false})
     * centered inside the button with uniform {@code iconInset} on each side.
     *
     * @param size         logical side length of the square button in pixels
     * @param iconInset    gap between the button edge and the icon on each side
     * @param cornerRadius corner rounding in logical pixels
     * @param iconSupplier supplier of the icon {@link Identifier}; returning {@code null} skips drawing
     * @param isSprite     {@code true} to use {@link GuiRenderer#drawSprite}, {@code false} for
     *                     {@link GuiRenderer#drawTexture}
     */
    public FIconButtonWidget(float size, float iconInset, float cornerRadius, Supplier<Identifier> iconSupplier, boolean isSprite) {
        this.buttonSize = size;
        this.iconInset = iconInset;
        this.cornerRadius = cornerRadius;
        this.iconSupplier = iconSupplier;
        this.isSprite = isSprite;
        this.labelSupplier = null;
    }

    /**
     * Label mode — renders a short text glyph (e.g. {@code "+"}, {@code "✕"}) centered inside the button.
     *
     * @param size          logical side length of the square button in pixels
     * @param cornerRadius  corner rounding in logical pixels
     * @param labelSupplier supplier of the glyph string to center; returning {@code null} or empty skips drawing
     */
    public FIconButtonWidget(float size, float cornerRadius, Supplier<String> labelSupplier) {
        this.buttonSize = size;
        this.iconInset = 0f;
        this.cornerRadius = cornerRadius;
        this.iconSupplier = null;
        this.isSprite = false;
        this.labelSupplier = labelSupplier;
    }

    /**
     * Sets the action to invoke when this button is clicked with the primary mouse button.
     *
     * @param onClick click handler; {@code null} clears any existing handler
     */
    public void setOnClick(Runnable onClick) {
        this.onClick = onClick == null ? () -> {} : onClick;
    }

    /**
     * When {@code false}, the button background is drawn with a plain fill instead of a bordered
     * frame. Use this for translucent "ghost" buttons where a distinct 1 px border ring is
     * undesirable.
     *
     * @param drawBorder {@code true} (default) to use {@code fillRoundedRectFrame}; {@code false}
     *                   to use {@code fillRoundedRect}
     */
    public void setDrawBorder(boolean drawBorder) {
        this.drawBorder = drawBorder;
    }

    /**
     * Packed ARGB fill color for the button background.
     *
     * @param hovered whether the pointer currently lies over this button
     * @return fill ARGB
     */
    protected int resolveButtonFillArgb(boolean hovered) {
        return FascinatedGuiTheme.INSTANCE.surface();
    }

    /**
     * Packed ARGB border color.
     *
     * @param hovered whether the pointer currently lies over this button
     * @return border ARGB
     */
    protected int resolveButtonBorderArgb(boolean hovered) {
        return hovered ? FascinatedGuiTheme.INSTANCE.borderHover() : FascinatedGuiTheme.INSTANCE.border();
    }

    /**
     * Packed ARGB tint applied to the icon sprite/texture or text glyph.
     *
     * @param hovered whether the pointer currently lies over this button
     * @return content tint ARGB
     */
    protected int resolveContentTintArgb(boolean hovered) {
        return hovered ? 0xFFFFFFFF : 0xAAFFFFFF;
    }

    @Override
    public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
        setBounds(layoutX, layoutY, buttonSize, buttonSize);
    }

    @Override
    public float intrinsicWidthForRow(UIRenderer measure, float heightBudget) {
        return buttonSize;
    }

    @Override
    public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
        return buttonSize;
    }

    @Override
    public PointerHitKind pointerHitKind() {
        return PointerHitKind.TARGET;
    }

    @Override
    public UiPointerCursor pointerCursor(float pointerX, float pointerY) {
        return UiPointerCursor.HAND;
    }

    @Override
    public boolean click(float pointerX, float pointerY, int button) {
        if (button == 0) {
            onClick.run();
            return true;
        }
        return false;
    }

    @Override
    protected void renderSelf(GuiRenderer graphics, UiFrameContext frame, float deltaSeconds) {
        boolean hovered = frame.isHitTarget(this);
        float clampedCorner = Math.min(cornerRadius, Math.min(w(), h()) * 0.5f - 0.01f);
        if (drawBorder) {
            graphics.fillRoundedRectFrame(x(), y(), w(), h(), clampedCorner,
                    resolveButtonBorderArgb(hovered), resolveButtonFillArgb(hovered),
                    UITheme.BORDER_THICKNESS_PX, UITheme.BORDER_THICKNESS_PX, RectCornerRoundMask.ALL);
        } else {
            graphics.fillRoundedRect(x(), y(), w(), h(), clampedCorner,
                    resolveButtonFillArgb(hovered), RectCornerRoundMask.ALL);
        }
        int tint = resolveContentTintArgb(hovered);
        if (iconSupplier != null) {
            Identifier icon = iconSupplier.get();
            if (icon != null) {
                float inner = w() - 2f * iconInset;
                float iconX = x() + iconInset;
                float iconY = y() + iconInset;
                if (isSprite) {
                    graphics.drawSprite(icon, iconX, iconY, inner, inner, tint);
                } else {
                    graphics.drawTexture(icon, iconX, iconY, inner, inner, tint);
                }
            }
        } else if (labelSupplier != null) {
            String label = labelSupplier.get();
            if (label != null && !label.isEmpty()) {
                graphics.drawCenteredText(label, x() + w() * 0.5f, y() + (h() - graphics.getFontCapHeight()) * 0.5f, tint, false, false);
            }
        }
    }
}
