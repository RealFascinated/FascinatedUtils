package cc.fascinated.fascinatedutils.gui.declare;

import cc.fascinated.fascinatedutils.gui.core.Align;
import cc.fascinated.fascinatedutils.gui.core.Callback;
import cc.fascinated.fascinatedutils.gui.core.TextOverflow;
import cc.fascinated.fascinatedutils.gui.widgets.FOutlinedTextInputWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;

import cc.fascinated.fascinatedutils.gui.core.Ref;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Immutable description of widget tree content; reconciler maps this to retained {@link FWidget} instances.
 */
public sealed interface UiView permits UiView.UiColumn, UiView.UiRow, UiView.UiStack, UiView.UiScroll, UiView.UiSpacer,
        UiView.UiMinWidth, UiView.UiMaxCenter, UiView.UiRect, UiView.UiLabel, UiView.UiButton, UiView.UiOutlinedPinned,
        UiView.UiCustom, UiView.UiWidgetSlot, UiView.UiComponentNode {

    record ColumnSpec(float gap, Align horizontalAlign) {
    }

    record RowSpec(float gap, Align verticalAlign) {
    }

    record ScrollSpec(float scrollClipRowGap, float bodyColumnGap, boolean fillVertical,
                      @Nullable Consumer<Float> scrollOffsetChangeListener, @Nullable Ref<Float> scrollOffsetRef) {
    }

    record RectSpec(int fillArgb, float cornerRadius, Integer borderArgb, Float borderThickness) {
    }

    record LabelSpec(String text, int colorArgb, boolean bold, TextOverflow overflow, Align alignX) {
    }

    record ButtonSpec(Runnable onClick, Supplier<String> label, float layoutWidthLogical, int maxLabelLines,
                      float labelLineGapDesign, float verticalPadDesign, float heightScale, float horizontalTextPadDesign,
                      float cornerRadiusDesign) {
    }

    record UiColumn(ColumnSpec spec, List<UiSlot> children) implements UiView {
        public UiColumn {
            children = List.copyOf(children);
        }
    }

    record UiRow(RowSpec spec, List<UiSlot> children) implements UiView {
        public UiRow {
            children = List.copyOf(children);
        }
    }

    record UiStack(List<UiSlot> layers) implements UiView {
        public UiStack {
            layers = List.copyOf(layers);
        }
    }

    record UiScroll(ScrollSpec spec, List<UiSlot> bodyChildren) implements UiView {
        public UiScroll {
            bodyChildren = List.copyOf(bodyChildren);
        }
    }

    record UiSpacer(float width, float height) implements UiView {
    }

    record UiMinWidth(float minimumWidth, UiView inner) implements UiView {
        public UiMinWidth {
            minimumWidth = Math.max(0f, minimumWidth);
        }
    }

    record UiMaxCenter(float insetHorizontal, float insetVertical, float maxWidth, float maxHeight, UiView inner) implements UiView {
    }

    record UiRect(RectSpec spec) implements UiView {
        public UiRect fill(int argb) {
            return new UiRect(new RectSpec(argb, 0f, null, null));
        }
    }

    record UiLabel(LabelSpec spec) implements UiView {
    }

    record UiButton(ButtonSpec spec) implements UiView {
    }

    /**
     * Mounts an existing outlined text field instance (stable focus + IME).
     *
     * @param widget          retained widget
     * @param valueFromModel  optional external value override (for example search ref); {@code null} skips adopt
     * @param onChange        optional change callback (re-applied each reconcile)
     */
    record UiOutlinedPinned(FOutlinedTextInputWidget widget, @Nullable String valueFromModel,
                            Callback<String> onChange) implements UiView {
    }

    /**
     * Custom retained subtree with user-controlled reuse.
     */
    record UiCustom(UiBacking backing) implements UiView {
    }

    /**
     * Declarative bridge for pre-built {@code FWidget} instances (for example module cards).
     */
    record UiWidgetSlot(String slotKey, FWidget widget) implements UiView {
    }

    /**
     * Retained {@link UiComponent} instance with typed props.
     *
     * @param <P>     props type
     * @param type    component class for identity-based reuse across reconciles
     * @param factory zero-argument constructor used on first mount
     * @param props   props record/value forwarded to the component before each render
     */
    record UiComponentNode<P>(Class<? extends UiComponent<P>> type,
                              Supplier<? extends UiComponent<P>> factory,
                              P props) implements UiView {
    }
}
