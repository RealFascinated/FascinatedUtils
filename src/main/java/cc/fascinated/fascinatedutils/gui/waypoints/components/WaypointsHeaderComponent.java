package cc.fascinated.fascinatedutils.gui.waypoints.components;

import cc.fascinated.fascinatedutils.gui.core.Align;
import cc.fascinated.fascinatedutils.gui.core.TextOverflow;
import cc.fascinated.fascinatedutils.gui.declare.Ui;
import cc.fascinated.fascinatedutils.gui.declare.UiComponent;
import cc.fascinated.fascinatedutils.gui.declare.UiSlot;
import cc.fascinated.fascinatedutils.gui.declare.UiView;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FButtonWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FCellConstraints;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Title row for the waypoints screen: title label, accent add-chip, and close button.
 */
public class WaypointsHeaderComponent extends UiComponent<WaypointsHeaderComponent.Props> {
    private static final float ADD_BUTTON_WIDTH = 64f;
    private static final float ACTION_BUTTON_HEIGHT = 20f;
    private static final float BUTTON_GAP = 3f;

    private final WaypointAccentAddChipButton addChipButton = new WaypointAccentAddChipButton(
            () -> {},
            () -> Component.translatable("fascinatedutils.waypoints.add").getString());

    public static UiView view(Props props) {
        return Ui.component(WaypointsHeaderComponent.class, WaypointsHeaderComponent::new, props);
    }

    @Override
    public UiView render() {
        Props currentProps = props();
        addChipButton.setOnClick(currentProps.onRequestAdd());
        return Ui.row(BUTTON_GAP, Align.CENTER, List.of(
                Ui.slot(new FCellConstraints().setExpandHorizontal(true).setGrowWeight(1f),
                        Ui.label(Component.translatable("fascinatedutils.waypoints.title").getString(),
                                FascinatedGuiTheme.INSTANCE.textPrimary(), true, TextOverflow.ELLIPSIS, Align.START)),
                UiSlot.of(Ui.widgetSlot("waypoints.header.add", addChipButton)),
                UiSlot.of(Ui.buttonClose(currentProps.onClose()))
        ));
    }

    /**
     * Props for {@link WaypointsHeaderComponent}.
     *
     * @param onRequestAdd invoked when the accent add chip is clicked
     * @param onClose      invoked when the close button is clicked
     */
    public record Props(Runnable onRequestAdd, Runnable onClose) {
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
