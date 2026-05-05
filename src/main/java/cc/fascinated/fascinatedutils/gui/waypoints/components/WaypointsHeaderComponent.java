package cc.fascinated.fascinatedutils.gui.waypoints.components;

import cc.fascinated.fascinatedutils.gui.core.Align;
import cc.fascinated.fascinatedutils.gui.core.TextOverflow;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FButtonWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FCellConstraints;
import cc.fascinated.fascinatedutils.gui.widgets.FLabelWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FRowWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import net.minecraft.network.chat.Component;

/**
 * Title row for the waypoints screen: title label, accent add-chip, and close button.
 */
public class WaypointsHeaderComponent {
    private static final float ADD_BUTTON_WIDTH = 64f;
    private static final float ACTION_BUTTON_HEIGHT = 20f;
    private static final float BUTTON_GAP = 3f;

    public static FWidget build(Runnable onRequestAdd, Runnable onClose) {
        FLabelWidget titleLabel = new FLabelWidget();
        titleLabel.setText(Component.translatable("fascinatedutils.waypoints.title").getString());
        titleLabel.setColorArgb(FascinatedGuiTheme.INSTANCE.textPrimary());
        titleLabel.setTextBold(true);
        titleLabel.setOverflow(TextOverflow.ELLIPSIS);
        titleLabel.setAlignX(Align.START);

        WaypointAccentAddChipButton addChipButton = new WaypointAccentAddChipButton(
                onRequestAdd, () -> Component.translatable("fascinatedutils.waypoints.add").getString());

        FButtonWidget closeButton = new FButtonWidget(onClose, () -> "\u2715", 22f, 1, 1f, 4f, 1f, 4f, -1f);

        FRowWidget row = new FRowWidget(BUTTON_GAP, Align.CENTER);
        row.addChild(titleLabel, new FCellConstraints().setExpandHorizontal(true).setGrowWeight(1f));
        row.addChild(addChipButton);
        row.addChild(closeButton);
        return row;
    }

    private static final class WaypointAccentAddChipButton extends FButtonWidget {
        WaypointAccentAddChipButton(Runnable onClick, java.util.function.Supplier<String> labelSupplier) {
            super(onClick, labelSupplier, ADD_BUTTON_WIDTH, 1, 1f, 4f, 1f, 8f, 2f);
        }

        @Override
        protected int resolveButtonFillColorArgb(boolean hoveredState) {
            return hoveredState ? UITheme.COLOR_ACCENT_HOVER : UITheme.COLOR_ACCENT;
        }

        @Override
        protected int resolveButtonBorderColorArgb(boolean hoveredState) {
            return hoveredState ? UITheme.COLOR_ACCENT_HOVER : UITheme.COLOR_ACCENT;
        }

        @Override
        public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
            return ACTION_BUTTON_HEIGHT;
        }
    }
}
