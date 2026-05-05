package cc.fascinated.fascinatedutils.gui.waypoints.components;

import cc.fascinated.fascinatedutils.common.color.SettingColor;
import cc.fascinated.fascinatedutils.gui.core.Align;
import cc.fascinated.fascinatedutils.gui.core.TextOverflow;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FAbsoluteStackWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FButtonWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FCellConstraints;
import cc.fascinated.fascinatedutils.gui.widgets.FColumnWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FLabelWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FOutlinedTextInputWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FRectWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FRowWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FSpacerWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

/**
 * Builds the create-waypoint form card. Input widgets are passed in by the caller so focus/IME
 * state persists across rebuilds.
 */
public class WaypointCreateCardComponent {

    public static FWidget build(String dimension, SettingColor color, FOutlinedTextInputWidget nameInput,
                                FOutlinedTextInputWidget xInput, FOutlinedTextInputWidget yInput,
                                FOutlinedTextInputWidget zInput, Runnable onOpenColorPicker,
                                Runnable onCancel, Runnable onSubmit) {
        float gap = UITheme.GAP_SM;
        float sectionGap = 8f;
        float pad = UITheme.PADDING_MD;

        ColorSwatchButton colorSwatchButton = new ColorSwatchButton();
        colorSwatchButton.configure(color, onOpenColorPicker);

        FRowWidget coordsRow = new FRowWidget(gap, Align.START);
        coordsRow.addChild(axisColumn("X", xInput), new FCellConstraints().setExpandHorizontal(true).setGrowWeight(1f));
        coordsRow.addChild(axisColumn("Y", yInput), new FCellConstraints().setExpandHorizontal(true).setGrowWeight(1f));
        coordsRow.addChild(axisColumn("Z", zInput), new FCellConstraints().setExpandHorizontal(true).setGrowWeight(1f));

        FCellConstraints expandH = new FCellConstraints().setExpandHorizontal(true).setGrowWeight(1f);
        FButtonWidget cancelButton = new FButtonWidget(onCancel,
                () -> Component.translatable("fascinatedutils.waypoints.popup.cancel").getString(), 100f, 1, 1f, 8f, 1f, 8f, -1f);
        FButtonWidget confirmButton = new FButtonWidget(onSubmit,
                () -> Component.translatable("fascinatedutils.waypoints.create.confirm").getString(), 100f, 1, 1f, 8f, 1f, 8f, -1f);
        FRowWidget actionsRow = new FRowWidget(gap, Align.CENTER);
        actionsRow.addChild(cancelButton, expandH);
        actionsRow.addChild(confirmButton, new FCellConstraints().setExpandHorizontal(true).setGrowWeight(1f));

        FColumnWidget body = new FColumnWidget(0f, Align.START);
        body.addChild(sectionLabel(Component.translatable("fascinatedutils.waypoints.create.title").getString(), true, FascinatedGuiTheme.INSTANCE.textPrimary()));
        body.addChild(new FSpacerWidget(0f, gap));
        body.addChild(sectionLabel(dimension, false, FascinatedGuiTheme.INSTANCE.textMuted()));
        body.addChild(new FSpacerWidget(0f, sectionGap));
        body.addChild(sectionLabel(Component.translatable("fascinatedutils.waypoints.create.name").getString(), false, FascinatedGuiTheme.INSTANCE.textMuted()));
        body.addChild(new FSpacerWidget(0f, gap));
        body.addChild(nameInput);
        body.addChild(new FSpacerWidget(0f, sectionGap));
        body.addChild(coordsRow);
        body.addChild(new FSpacerWidget(0f, sectionGap));
        body.addChild(sectionLabel(Component.translatable("fascinatedutils.waypoints.create.color").getString(), false, FascinatedGuiTheme.INSTANCE.textMuted()));
        body.addChild(new FSpacerWidget(0f, gap));
        body.addChild(colorSwatchButton);
        body.addChild(new FSpacerWidget(0f, sectionGap));
        body.addChild(actionsRow);

        FColumnWidget paddedBody = new FColumnWidget(0f, Align.START);
        paddedBody.addChild(body, new FCellConstraints().setMargins(pad, pad));

        FRectWidget cardBg = new FRectWidget();
        cardBg.setFillColorArgb(UITheme.COLOR_SURFACE);
        cardBg.setCornerRadius(6f);
        cardBg.setBorder(UITheme.COLOR_BORDER, 1f);

        FAbsoluteStackWidget card = new FAbsoluteStackWidget();
        card.addChild(cardBg);
        card.addChild(paddedBody);
        return card;
    }

    private static FLabelWidget sectionLabel(String text, boolean bold, int colorArgb) {
        FLabelWidget label = new FLabelWidget();
        label.setText(text);
        label.setColorArgb(colorArgb);
        label.setTextBold(bold);
        label.setOverflow(TextOverflow.VISIBLE);
        label.setAlignX(Align.START);
        return label;
    }

    private static FWidget axisColumn(String axis, FOutlinedTextInputWidget input) {
        FColumnWidget col = new FColumnWidget(UITheme.GAP_SM, Align.START);
        col.addChild(sectionLabel(axis, false, FascinatedGuiTheme.INSTANCE.textMuted()));
        col.addChild(input);
        return col;
    }

    private static final class ColorSwatchButton extends FButtonWidget {
        private SettingColor boundColor;

        ColorSwatchButton() {
            super(() -> {}, changeColorLabel(), 0f, 1, 2f, 6f, 1f, 8f);
        }

        private static Supplier<String> changeColorLabel() {
            return () -> Component.translatable("fascinatedutils.waypoints.create.change_color").getString();
        }

        void configure(SettingColor color, Runnable onClick) {
            this.boundColor = color;
            setOnClick(onClick);
        }

        @Override
        protected int resolveButtonFillColorArgb(boolean hoveredState) {
            int argb = boundColor == null ? 0 : boundColor.getPackedArgb();
            if (!hoveredState) {
                return argb;
            }
            int red = Math.min(255, ((argb >> 16) & 0xFF) + 20);
            int green = Math.min(255, ((argb >> 8) & 0xFF) + 20);
            int blue = Math.min(255, (argb & 0xFF) + 20);
            return (argb & 0xFF000000) | (red << 16) | (green << 8) | blue;
        }

        @Override
        protected int resolveButtonBorderColorArgb(boolean hoveredState) {
            return UITheme.COLOR_BORDER;
        }
    }
}
