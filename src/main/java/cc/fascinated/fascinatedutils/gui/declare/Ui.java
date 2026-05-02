package cc.fascinated.fascinatedutils.gui.declare;

import cc.fascinated.fascinatedutils.gui.core.Align;
import cc.fascinated.fascinatedutils.gui.core.Callback;
import cc.fascinated.fascinatedutils.gui.core.Ref;
import cc.fascinated.fascinatedutils.gui.core.TextOverflow;
import cc.fascinated.fascinatedutils.gui.widgets.FCellConstraints;
import cc.fascinated.fascinatedutils.gui.widgets.FOutlinedTextInputWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Ergonomic factories for {@link UiView} trees.
 */
public final class Ui {
    private Ui() {
    }

    public static UiView column(float gap, Align horizontalAlign, UiSlot... children) {
        return new UiView.UiColumn(new UiView.ColumnSpec(gap, horizontalAlign), List.of(children));
    }

    public static UiView column(float gap, Align horizontalAlign, List<UiSlot> children) {
        return new UiView.UiColumn(new UiView.ColumnSpec(gap, horizontalAlign), children);
    }

    public static UiView row(float gap, Align verticalAlign, UiSlot... children) {
        return new UiView.UiRow(new UiView.RowSpec(gap, verticalAlign), List.of(children));
    }

    public static UiView row(float gap, Align verticalAlign, List<UiSlot> children) {
        return new UiView.UiRow(new UiView.RowSpec(gap, verticalAlign), children);
    }

    public static UiView stackLayers(UiSlot... layers) {
        return new UiView.UiStack(List.of(layers));
    }

    public static UiView stackLayers(List<UiSlot> layers) {
        return new UiView.UiStack(List.copyOf(layers));
    }

    public static UiView scrollBody(float clipRowGap, float bodyColumnGap, boolean fillVertical, List<UiSlot> body) {
        return new UiView.UiScroll(new UiView.ScrollSpec(clipRowGap, bodyColumnGap, fillVertical, null, null), body);
    }

    public static UiView scrollTracked(float clipRowGap, float bodyColumnGap, boolean fillVertical,
                                       @Nullable Consumer<Float> scrollOffsetChangeListener,
                                       @Nullable Ref<Float> scrollOffsetRef,
                                       List<UiSlot> bodyChildren) {
        return new UiView.UiScroll(new UiView.ScrollSpec(clipRowGap, bodyColumnGap, fillVertical,
                scrollOffsetChangeListener, scrollOffsetRef), bodyChildren);
    }

    public static UiView spacer(float width, float height) {
        return new UiView.UiSpacer(width, height);
    }

    public static UiView minWidth(float minimumWidth, UiView inner) {
        return new UiView.UiMinWidth(minimumWidth, inner);
    }

    public static UiView centerMax(float insetHorizontal, float insetVertical, float maxInnerWidth, float maxInnerHeight, UiView inner) {
        return new UiView.UiMaxCenter(insetHorizontal, insetVertical, maxInnerWidth, maxInnerHeight, inner);
    }

    public static UiView rectPlain(int fillArgb) {
        return new UiView.UiRect(new UiView.RectSpec(fillArgb, 0f, null, null));
    }

    public static UiView rectDecorated(int fillArgb, float cornerRadius, Integer borderArgb, Float borderThickness) {
        return new UiView.UiRect(new UiView.RectSpec(fillArgb, cornerRadius, borderArgb, borderThickness));
    }

    public static UiView label(UiView.LabelSpec spec) {
        return new UiView.UiLabel(spec);
    }

    public static UiView label(String text, int colorArgb, boolean bold, TextOverflow overflow, Align alignX) {
        return new UiView.UiLabel(new UiView.LabelSpec(text, colorArgb, bold, overflow, alignX));
    }

    public static UiView buttonClose(Runnable onClick) {
        return new UiView.UiButton(new UiView.ButtonSpec(onClick, () -> "\u2715", 22f, 1, 1f, 4f, 1f, 4f, -1f));
    }

    public static UiView buttonStandard(Runnable onClick, java.util.function.Supplier<String> label, float layoutWidthLogical) {
        return new UiView.UiButton(new UiView.ButtonSpec(onClick, label, layoutWidthLogical, 1, 1f, 8f, 1f, 8f, -1f));
    }

    public static UiView outlinedPinned(FOutlinedTextInputWidget field, @Nullable String valueFromModel, Callback<String> onChange) {
        return new UiView.UiOutlinedPinned(field, valueFromModel, onChange);
    }

    public static UiView custom(UiBacking backing) {
        return new UiView.UiCustom(backing);
    }

    public static UiView widgetSlot(String key, FWidget widget) {
        return new UiView.UiWidgetSlot(key, widget);
    }

    /**
     * Mounts a stateful {@link UiComponent} with typed props; the reconciler preserves the
     * instance across frames as long as {@code type} matches the prior mount.
     *
     * @param type    component class (used for identity-based reuse)
     * @param factory zero-argument constructor invoked on first mount
     * @param props   props value bound before each render pass
     * @param <P>     props type
     * @return component view node
     */
    public static <P> UiView component(Class<? extends UiComponent<P>> type,
                                       Supplier<? extends UiComponent<P>> factory,
                                       P props) {
        return new UiView.UiComponentNode<>(type, factory, props);
    }

    public static List<UiSlot> slots(UiSlot... items) {
        return Arrays.asList(items);
    }

    public static ArrayList<UiSlot> mutableSlots() {
        return new ArrayList<>();
    }

    public static UiSlot slot(UiView node) {
        return UiSlot.of(node);
    }

    public static UiSlot slot(FCellConstraints constraints, UiView node) {
        return new UiSlot(null, constraints, node);
    }

    public static UiSlot keyed(String key, UiView node) {
        return UiSlot.keyed(key, node);
    }
}
