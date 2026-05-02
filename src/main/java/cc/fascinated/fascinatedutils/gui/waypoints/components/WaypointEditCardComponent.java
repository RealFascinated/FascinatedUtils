package cc.fascinated.fascinatedutils.gui.waypoints.components;

import cc.fascinated.fascinatedutils.common.color.SettingColor;
import cc.fascinated.fascinatedutils.gui.core.Align;
import cc.fascinated.fascinatedutils.gui.core.TextOverflow;
import cc.fascinated.fascinatedutils.gui.declare.Ui;
import cc.fascinated.fascinatedutils.gui.declare.UiComponent;
import cc.fascinated.fascinatedutils.gui.declare.UiSlot;
import cc.fascinated.fascinatedutils.gui.declare.UiView;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FButtonWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FCellConstraints;
import cc.fascinated.fascinatedutils.gui.widgets.FIconCheckboxWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FOutlinedTextInputWidget;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Supplier;

/**
 * Declarative edit-waypoint form card. Inputs and checkboxes are retained by the screen and
 * passed in as props so focus/toggle state survives reconciles.
 */
public class WaypointEditCardComponent extends UiComponent<WaypointEditCardComponent.Props> {
    private final EditColorSwatchButton colorSwatchButton = new EditColorSwatchButton();

    public static UiView view(Props props) {
        return Ui.component(WaypointEditCardComponent.class, WaypointEditCardComponent::new, props);
    }

    @Override
    public UiView render() {
        Props currentProps = props();
        float gap = UITheme.GAP_SM;
        float sectionGap = 8f;
        float pad = UITheme.PADDING_MD;

        colorSwatchButton.configure(currentProps.color(), currentProps.onOpenColorPicker());

        UiView nameInputView = Ui.outlinedPinned(currentProps.nameInput(), null, value -> {});
        UiView xInputView = Ui.outlinedPinned(currentProps.xInput(), null, value -> {});
        UiView yInputView = Ui.outlinedPinned(currentProps.yInput(), null, value -> {});
        UiView zInputView = Ui.outlinedPinned(currentProps.zInput(), null, value -> {});

        UiView coordsRow = Ui.row(gap, Align.START, List.of(
                Ui.slot(coordConstraints(), axisColumn("X", xInputView)),
                Ui.slot(coordConstraints(), axisColumn("Y", yInputView)),
                Ui.slot(coordConstraints(), axisColumn("Z", zInputView))
        ));

        UiView actionsRow = Ui.row(gap, Align.CENTER, List.of(
                Ui.slot(new FCellConstraints().setExpandHorizontal(true).setGrowWeight(1f),
                        Ui.buttonStandard(currentProps.onCancel(),
                                () -> Component.translatable("fascinatedutils.waypoints.popup.cancel").getString(), 100f)),
                Ui.slot(new FCellConstraints().setExpandHorizontal(true).setGrowWeight(1f),
                        Ui.buttonStandard(currentProps.onSubmit(),
                                () -> Component.translatable("fascinatedutils.waypoints.edit.confirm").getString(), 100f))
        ));

        UiView body = Ui.column(0f, Align.START, List.of(
                UiSlot.of(sectionLabel(Component.translatable("fascinatedutils.waypoints.edit.title").getString(),
                        true, FascinatedGuiTheme.INSTANCE.textPrimary())),
                UiSlot.of(Ui.spacer(0f, sectionGap)),
                UiSlot.of(sectionLabel(Component.translatable("fascinatedutils.waypoints.create.name").getString(),
                        false, FascinatedGuiTheme.INSTANCE.textMuted())),
                UiSlot.of(Ui.spacer(0f, gap)),
                UiSlot.of(nameInputView),
                UiSlot.of(Ui.spacer(0f, sectionGap)),
                UiSlot.of(coordsRow),
                UiSlot.of(Ui.spacer(0f, sectionGap)),
                UiSlot.of(sectionLabel(Component.translatable("fascinatedutils.waypoints.create.color").getString(),
                        false, FascinatedGuiTheme.INSTANCE.textMuted())),
                UiSlot.of(Ui.spacer(0f, gap)),
                UiSlot.of(Ui.widgetSlot("waypoint-edit.color-swatch", colorSwatchButton)),
                UiSlot.of(Ui.spacer(0f, sectionGap)),
                UiSlot.of(Ui.widgetSlot("waypoint-edit.beam", currentProps.beamCheckbox())),
                UiSlot.of(Ui.spacer(0f, sectionGap)),
                UiSlot.of(Ui.widgetSlot("waypoint-edit.distance", currentProps.distanceCheckbox())),
                UiSlot.of(Ui.spacer(0f, sectionGap)),
                UiSlot.of(actionsRow)
        ));

        UiView paddedBody = Ui.column(0f, Align.START, List.of(
                Ui.slot(new FCellConstraints().setMargins(pad, pad), body)
        ));

        return Ui.stackLayers(
                UiSlot.of(Ui.rectDecorated(UITheme.COLOR_SURFACE, 6f, UITheme.COLOR_BORDER, 1f)),
                UiSlot.of(paddedBody));
    }

    private static UiView sectionLabel(String text, boolean bold, int colorArgb) {
        return Ui.label(text, colorArgb, bold, TextOverflow.VISIBLE, Align.START);
    }

    private static UiView axisColumn(String axis, UiView inputView) {
        return Ui.column(UITheme.GAP_SM, Align.START, List.of(
                UiSlot.of(sectionLabel(axis, false, FascinatedGuiTheme.INSTANCE.textMuted())),
                UiSlot.of(inputView)
        ));
    }

    private static FCellConstraints coordConstraints() {
        return new FCellConstraints().setExpandHorizontal(true).setGrowWeight(1f);
    }

    /**
     * Props for {@link WaypointEditCardComponent}.
     *
     * @param color             swatch source (picker's live color)
     * @param nameInput         retained name text field
     * @param xInput            retained X coordinate text field
     * @param yInput            retained Y coordinate text field
     * @param zInput            retained Z coordinate text field
     * @param beamCheckbox      retained show-beam toggle
     * @param distanceCheckbox  retained show-distance toggle
     * @param onOpenColorPicker opens the color picker overlay
     * @param onCancel          invoked from the cancel button
     * @param onSubmit          invoked from the submit button
     */
    public record Props(SettingColor color,
                        FOutlinedTextInputWidget nameInput,
                        FOutlinedTextInputWidget xInput,
                        FOutlinedTextInputWidget yInput,
                        FOutlinedTextInputWidget zInput,
                        FIconCheckboxWidget beamCheckbox,
                        FIconCheckboxWidget distanceCheckbox,
                        Runnable onOpenColorPicker,
                        Runnable onCancel,
                        Runnable onSubmit) {
    }

    private static final class EditColorSwatchButton extends FButtonWidget {
        private SettingColor boundColor;

        EditColorSwatchButton() {
            super(() -> {}, changeColorLabel(), 0f, 1, 2f, 6f, 1f, 8f);
        }

        void configure(SettingColor color, Runnable onClick) {
            this.boundColor = color;
            setOnClick(onClick);
        }

        private static Supplier<String> changeColorLabel() {
            return () -> Component.translatable("fascinatedutils.waypoints.create.change_color").getString();
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
