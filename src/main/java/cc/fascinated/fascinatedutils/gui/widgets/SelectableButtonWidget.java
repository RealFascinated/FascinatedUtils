package cc.fascinated.fascinatedutils.gui.widgets;

import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;

import java.util.function.Supplier;

/**
 * A {@link FButtonWidget} variant that toggles between a "selected" and "unselected" visual state
 * based on a runtime {@code isActive} supplier.
 *
 * <p>When active the fill uses {@code moduleListRowSelected} and the border uses {@code borderMuted}.
 * When inactive both fall back to the standard row colours. In both states a hovered pointer
 * switches the fill to {@code moduleListRowHover}.
 */
public class SelectableButtonWidget extends FButtonWidget {
    private final Supplier<Boolean> isActive;

    public SelectableButtonWidget(Runnable onClick, Supplier<String> labelSupplier, float layoutWidthLogical, int maxLabelLines, float labelLineGapDesign, float verticalPadDesign, float heightScale, float horizontalTextPadDesign, float cornerRadiusDesign, Supplier<Boolean> isActive) {
        super(onClick, labelSupplier, layoutWidthLogical, maxLabelLines, labelLineGapDesign, verticalPadDesign, heightScale, horizontalTextPadDesign, cornerRadiusDesign);
        this.isActive = isActive;
    }

    @Override
    protected int resolveButtonFillColorArgb(boolean hovered) {
        boolean active = Boolean.TRUE.equals(isActive.get());
        if (active) {
            return hovered ? FascinatedGuiTheme.INSTANCE.moduleListRowHover() : FascinatedGuiTheme.INSTANCE.moduleListRowSelected();
        }
        return hovered ? FascinatedGuiTheme.INSTANCE.moduleListRowHover() : FascinatedGuiTheme.INSTANCE.moduleListRow();
    }

    @Override
    protected int resolveButtonBorderColorArgb(boolean hovered) {
        boolean active = Boolean.TRUE.equals(isActive.get());
        if (active) {
            return hovered ? FascinatedGuiTheme.INSTANCE.borderHover() : FascinatedGuiTheme.INSTANCE.borderMuted();
        }
        return super.resolveButtonBorderColorArgb(hovered);
    }
}
