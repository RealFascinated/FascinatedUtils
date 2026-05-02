package cc.fascinated.fascinatedutils.gui.declare;

import cc.fascinated.fascinatedutils.gui.core.Align;
import cc.fascinated.fascinatedutils.gui.core.Ref;
import cc.fascinated.fascinatedutils.gui.widgets.FAbsoluteStackWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FButtonWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FCellConstraints;
import cc.fascinated.fascinatedutils.gui.widgets.FColumnWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FLabelWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FMaxCenterInsetsWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FMinWidthHostWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FOutlinedTextInputWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FRectWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FReconcileRoot;
import cc.fascinated.fascinatedutils.gui.widgets.FRowWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FScrollColumnWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FSpacerWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Maps immutable {@link UiView} trees onto retained {@link FWidget} instances.
 */
public final class UiReconciler {

    public void sync(FReconcileRoot root, MountNode rootMount, UiView view) {
        FWidget surface = reconcileInto(rootMount, view);
        root.clearChildren();
        if (surface != null) {
            root.addChild(surface);
        }
    }

    private FWidget reconcileInto(MountNode mount, UiView view) {
        if (!isCompatibleMount(mount, view)) {
            resetMountSubtree(mount);
        }
        return switch (view) {
            case UiView.UiColumn column -> reconcileColumn(mount, column);
            case UiView.UiRow row -> reconcileRow(mount, row);
            case UiView.UiStack stack -> reconcileStack(mount, stack);
            case UiView.UiScroll scroll -> reconcileScroll(mount, scroll);
            case UiView.UiSpacer spacer -> reconcileSpacer(mount, spacer);
            case UiView.UiMinWidth minWidth -> reconcileMinWidth(mount, minWidth);
            case UiView.UiMaxCenter maxCenter -> reconcileMaxCenter(mount, maxCenter);
            case UiView.UiRect rect -> reconcileRect(mount, rect);
            case UiView.UiLabel label -> reconcileLabel(mount, label);
            case UiView.UiButton button -> reconcileButton(mount, button);
            case UiView.UiOutlinedPinned outlined -> reconcileOutlined(mount, outlined);
            case UiView.UiCustom custom -> reconcileCustom(mount, custom);
            case UiView.UiWidgetSlot slot -> reconcileWidgetSlot(mount, slot);
            case UiView.UiComponentNode<?> component -> reconcileComponent(mount, component);
        };
    }

    private static boolean isCompatibleMount(MountNode mount, UiView view) {
        if (view instanceof UiView.UiComponentNode<?> componentNode) {
            return mount.component != null && mount.component.getClass() == componentNode.type();
        }
        if (mount.component != null) {
            return false;
        }
        if (mount.widget == null) {
            return true;
        }
        return isCompatibleWidget(mount.widget, view);
    }

    private static boolean isCompatibleWidget(FWidget widget, UiView view) {
        return switch (view) {
            case UiView.UiColumn ignored -> widget instanceof FColumnWidget;
            case UiView.UiRow ignored -> widget instanceof FRowWidget;
            case UiView.UiStack ignored -> widget instanceof FAbsoluteStackWidget;
            case UiView.UiScroll ignored -> widget instanceof FScrollColumnWidget;
            case UiView.UiSpacer ignored -> widget instanceof FSpacerWidget;
            case UiView.UiMinWidth ignored -> widget instanceof FMinWidthHostWidget;
            case UiView.UiMaxCenter ignored -> widget instanceof FMaxCenterInsetsWidget;
            case UiView.UiRect ignored -> widget instanceof FRectWidget;
            case UiView.UiLabel ignored -> widget instanceof FLabelWidget;
            case UiView.UiButton ignored -> widget instanceof FButtonWidget;
            case UiView.UiOutlinedPinned outlined -> widget == outlined.widget();
            case UiView.UiCustom ignored -> true;
            case UiView.UiWidgetSlot slot -> widget == slot.widget();
            case UiView.UiComponentNode<?> ignored -> false;
        };
    }

    private FWidget reconcileColumn(MountNode mount, UiView.UiColumn column) {
        FColumnWidget columnWidget;
        if (!(mount.widget instanceof FColumnWidget existing)) {
            resetMountSubtree(mount);
            columnWidget = new FColumnWidget(column.spec().gap(), column.spec().horizontalAlign());
            mount.widget = columnWidget;
        }
        else {
            columnWidget = existing;
            columnWidget.setGap(column.spec().gap());
            columnWidget.setHorizontalAlign(column.spec().horizontalAlign());
        }
        reconcileSlotChildren(columnWidget, mount, column.children());
        return columnWidget;
    }

    private FWidget reconcileRow(MountNode mount, UiView.UiRow row) {
        FRowWidget rowWidget;
        if (!(mount.widget instanceof FRowWidget existing)) {
            resetMountSubtree(mount);
            rowWidget = new FRowWidget(row.spec().gap(), row.spec().verticalAlign());
            mount.widget = rowWidget;
        }
        else {
            rowWidget = existing;
            rowWidget.setGap(row.spec().gap());
            rowWidget.setVerticalAlign(row.spec().verticalAlign());
        }
        reconcileSlotChildren(rowWidget, mount, row.children());
        return rowWidget;
    }

    private FWidget reconcileStack(MountNode mount, UiView.UiStack stack) {
        FAbsoluteStackWidget stackWidget;
        if (!(mount.widget instanceof FAbsoluteStackWidget existing)) {
            resetMountSubtree(mount);
            stackWidget = new FAbsoluteStackWidget();
            mount.widget = stackWidget;
        }
        else {
            stackWidget = existing;
        }
        reconcileSlotChildren(stackWidget, mount, stack.layers());
        return stackWidget;
    }

    private FWidget reconcileScroll(MountNode mount, UiView.UiScroll scroll) {
        UiView.ScrollSpec spec = scroll.spec();
        boolean compatible = mount.widget instanceof FScrollColumnWidget scrollWidget
                && scrollWidget.scrollBodyRoot() instanceof FColumnWidget
                && Math.abs(scrollWidget.scrollClipRowGap() - spec.scrollClipRowGap()) < 1e-3f;
        if (!compatible) {
            resetMountSubtree(mount);
            FColumnWidget bodyColumn = new FColumnWidget(spec.bodyColumnGap(), Align.START);
            FScrollColumnWidget freshScroll = FTheme.components().createScrollColumn(bodyColumn, spec.scrollClipRowGap());
            freshScroll.setFillVerticalInColumn(spec.fillVertical());
            mount.widget = freshScroll;
            mount.childMounts.add(new MountNode());
        }
        if (mount.childMounts.isEmpty()) {
            mount.childMounts.add(new MountNode());
        }
        FScrollColumnWidget scrollWidget = (FScrollColumnWidget) mount.widget;
        scrollWidget.setFillVerticalInColumn(spec.fillVertical());
        FColumnWidget bodyColumn = (FColumnWidget) scrollWidget.scrollBodyRoot();
        bodyColumn.setGap(spec.bodyColumnGap());
        bodyColumn.setHorizontalAlign(Align.START);
        MountNode bodyMount = mount.childMounts.get(0);
        reconcileSlotChildren(bodyColumn, bodyMount, scroll.bodyChildren());
        if (spec.scrollOffsetChangeListener() != null) {
            scrollWidget.setScrollOffsetChangeListener(spec.scrollOffsetChangeListener());
        }
        Ref<Float> offsetRef = spec.scrollOffsetRef();
        if (offsetRef != null) {
            Float saved = offsetRef.getValue();
            scrollWidget.setScrollOffsetY(saved == null ? 0f : saved);
        }
        return scrollWidget;
    }

    private FWidget reconcileSpacer(MountNode mount, UiView.UiSpacer spacer) {
        if (!(mount.widget instanceof FSpacerWidget existing)
                || Math.abs(existing.spacerWidth() - spacer.width()) > 1e-3f
                || Math.abs(existing.spacerHeight() - spacer.height()) > 1e-3f) {
            resetMountSubtree(mount);
            mount.widget = new FSpacerWidget(spacer.width(), spacer.height());
        }
        return mount.widget;
    }

    private FWidget reconcileMinWidth(MountNode mount, UiView.UiMinWidth minWidth) {
        MountNode innerMount = childMountAt(mount, 0);
        FWidget innerSurface = reconcileInto(innerMount, minWidth.inner());
        boolean needFreshHost = !(mount.widget instanceof FMinWidthHostWidget host)
                || host.minimumWidth() != minWidth.minimumWidth()
                || host.innerChild() != innerSurface;
        if (needFreshHost) {
            resetMountSubtree(mount);
            mount.widget = new FMinWidthHostWidget(minWidth.minimumWidth(), innerSurface);
            mount.childMounts.add(innerMount);
        }
        else {
            mount.childMounts.clear();
            mount.childMounts.add(innerMount);
        }
        return Objects.requireNonNull(mount.widget);
    }

    private FWidget reconcileMaxCenter(MountNode mount, UiView.UiMaxCenter maxCenter) {
        MountNode innerMount = childMountAt(mount, 0);
        FWidget innerSurface = reconcileInto(innerMount, maxCenter.inner());
        boolean needFreshHost = !(mount.widget instanceof FMaxCenterInsetsWidget host)
                || !host.matchesSpec(maxCenter.insetHorizontal(), maxCenter.insetVertical(), maxCenter.maxWidth(), maxCenter.maxHeight())
                || host.inner() != innerSurface;
        if (needFreshHost) {
            resetMountSubtree(mount);
            mount.widget = new FMaxCenterInsetsWidget(maxCenter.insetHorizontal(), maxCenter.insetVertical(), maxCenter.maxWidth(), maxCenter.maxHeight(), innerSurface);
            mount.childMounts.add(innerMount);
        }
        else {
            mount.childMounts.clear();
            mount.childMounts.add(innerMount);
        }
        return Objects.requireNonNull(mount.widget);
    }

    private FWidget reconcileRect(MountNode mount, UiView.UiRect rectView) {
        FRectWidget rect;
        if (!(mount.widget instanceof FRectWidget existing)) {
            resetMountSubtree(mount);
            rect = new FRectWidget();
            mount.widget = rect;
        }
        else {
            rect = existing;
        }
        UiView.RectSpec spec = rectView.spec();
        rect.setFillColorArgb(spec.fillArgb());
        rect.setCornerRadius(spec.cornerRadius());
        Integer borderArgb = spec.borderArgb();
        Float borderThickness = spec.borderThickness();
        if (borderArgb != null && borderThickness != null) {
            rect.setBorder(borderArgb, borderThickness);
        }
        else {
            rect.clearBorder();
        }
        return rect;
    }

    private FWidget reconcileLabel(MountNode mount, UiView.UiLabel labelView) {
        FLabelWidget label;
        if (!(mount.widget instanceof FLabelWidget existing)) {
            resetMountSubtree(mount);
            label = new FLabelWidget();
            mount.widget = label;
        }
        else {
            label = existing;
        }
        UiView.LabelSpec spec = labelView.spec();
        label.setText(spec.text());
        label.setColorArgb(spec.colorArgb());
        label.setTextBold(spec.bold());
        label.setOverflow(spec.overflow());
        label.setAlignX(spec.alignX());
        return label;
    }

    private FWidget reconcileButton(MountNode mount, UiView.UiButton buttonView) {
        FButtonWidget button;
        if (!(mount.widget instanceof FButtonWidget existing)) {
            resetMountSubtree(mount);
            UiView.ButtonSpec spec = buttonView.spec();
            button = new FButtonWidget(spec.onClick(), spec.label(), spec.layoutWidthLogical(), spec.maxLabelLines(),
                    spec.labelLineGapDesign(), spec.verticalPadDesign(), spec.heightScale(), spec.horizontalTextPadDesign(),
                    spec.cornerRadiusDesign());
            mount.widget = button;
        }
        else {
            button = existing;
            UiView.ButtonSpec spec = buttonView.spec();
            button.setOnClick(spec.onClick());
            button.setLabelSupplier(spec.label());
        }
        return button;
    }

    private FWidget reconcileOutlined(MountNode mount, UiView.UiOutlinedPinned outlined) {
        mount.childMounts.clear();
        FOutlinedTextInputWidget field = outlined.widget();
        mount.widget = field;
        field.setOnChange(outlined.onChange());
        String valueFromModel = outlined.valueFromModel();
        if (valueFromModel != null) {
            field.adoptExternalValueWithoutCallbacks(valueFromModel);
        }
        return field;
    }

    private FWidget reconcileCustom(MountNode mount, UiView.UiCustom custom) {
        FWidget previous = mount.widget;
        FWidget next = custom.backing().reconcilePrevious(previous);
        mount.widget = Objects.requireNonNull(next, "custom backing returned null widget");
        mount.childMounts.clear();
        return next;
    }

    private FWidget reconcileWidgetSlot(MountNode mount, UiView.UiWidgetSlot slot) {
        mount.childMounts.clear();
        mount.widget = slot.widget();
        return slot.widget();
    }

    private <P> FWidget reconcileComponent(MountNode mount, UiView.UiComponentNode<P> componentNode) {
        @SuppressWarnings("unchecked")
        UiComponent<P> component = (UiComponent<P>) mount.component;
        if (component == null) {
            component = componentNode.factory().get();
            mount.component = component;
            mount.widget = null;
        }
        component.bindProps(componentNode.props());
        component.dispatchMount();

        MountNode childMount = childMountAt(mount, 0);
        UiView renderedView = component.render();
        FWidget surface = reconcileInto(childMount, renderedView);
        mount.widget = surface;
        return surface;
    }

    private void reconcileSlotChildren(FWidget container, MountNode containerMount, List<UiSlot> slots) {
        Map<String, MountNode> pool = new HashMap<>();
        for (MountNode priorChild : containerMount.childMounts) {
            if (priorChild.lastEffectiveKey != null) {
                pool.put(priorChild.lastEffectiveKey, priorChild);
            }
        }

        List<MountNode> nextMounts = new ArrayList<>();
        for (int index = 0; index < slots.size(); index++) {
            UiSlot slot = slots.get(index);
            String effectiveKey = DeclarativeKeys.effectiveKey(slot.key(), index);
            MountNode childMount = pool.remove(effectiveKey);
            if (childMount == null) {
                childMount = new MountNode();
            }
            childMount.lastEffectiveKey = effectiveKey;
            FWidget childWidget = reconcileInto(childMount, slot.node());
            nextMounts.add(childMount);
        }

        container.clearChildren();
        containerMount.childMounts.clear();
        for (int index = 0; index < nextMounts.size(); index++) {
            MountNode childMount = nextMounts.get(index);
            UiSlot slot = slots.get(index);
            containerMount.childMounts.add(childMount);
            container.addChild(childMount.widget, FCellConstraints.copyNullable(slot.constraints()));
        }
    }

    private static MountNode childMountAt(MountNode parent, int index) {
        while (parent.childMounts.size() <= index) {
            parent.childMounts.add(new MountNode());
        }
        return parent.childMounts.get(index);
    }

    private static void resetMountSubtree(MountNode mount) {
        for (MountNode child : mount.childMounts) {
            resetMountSubtree(child);
        }
        mount.childMounts.clear();
        if (mount.component != null) {
            mount.component.dispatchUnmount();
            mount.component = null;
        }
        mount.widget = null;
        mount.lastEffectiveKey = null;
    }

    /**
     * Recursively disposes every component in the subtree by firing {@code onUnmount}. Intended
     * for screen teardown paths (for example {@link DeclarativeMountHost#dispose()}).
     *
     * @param rootMount root mount node to tear down
     */
    public void disposeAll(MountNode rootMount) {
        resetMountSubtree(rootMount);
    }

    public static final class MountNode {
        @Nullable FWidget widget;
        @Nullable UiComponent<?> component;
        @Nullable String lastEffectiveKey;
        final List<MountNode> childMounts = new ArrayList<>();
    }
}
