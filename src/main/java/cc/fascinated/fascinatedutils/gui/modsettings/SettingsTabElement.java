package cc.fascinated.fascinatedutils.gui.modsettings;

import cc.fascinated.fascinatedutils.common.setting.impl.ColorSetting;
import cc.fascinated.fascinatedutils.gui.core.Ref;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.widgets.FAbsoluteStackWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;

import java.util.function.Consumer;

public class SettingsTabElement extends FWidget {
    private final Ref<Float> settingsScrollRef = Ref.of(0f);
    private FWidget inner;
    private boolean dirty = true;
    private float cachedWidth = -1f;
    private float cachedHeight = -1f;

    private boolean showColorPicker;
    private ColorSetting activeColorSetting;

    @Override
    public boolean fillsVerticalInColumn() {
        return true;
    }

    @Override
    public boolean fillsHorizontalInRow() {
        return true;
    }

    @Override
    public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
        setBounds(layoutX, layoutY, layoutWidth, layoutHeight);
        if (dirty || cachedWidth != layoutWidth || cachedHeight != layoutHeight || inner == null) {
            rebuild(layoutWidth, layoutHeight);
            cachedWidth = layoutWidth;
            cachedHeight = layoutHeight;
            dirty = false;
        }
        if (inner != null) {
            inner.layout(measure, layoutX, layoutY, layoutWidth, layoutHeight);
        }
    }

    public void reset() {
        settingsScrollRef.setValue(0f);
        inner = null;
        dirty = true;
        cachedWidth = -1f;
        cachedHeight = -1f;
        showColorPicker = false;
        activeColorSetting = null;
        clearChildren();
    }

    private void rebuild(float width, float height) {
        Consumer<ColorSetting> openColorPicker = this::openColorPicker;
        FWidget settingsContent = ModSettingsRegistrySettingsTabBuilder.buildSettingsTab(width, height, settingsScrollRef, openColorPicker);

        FAbsoluteStackWidget rootStack = new FAbsoluteStackWidget();
        rootStack.addChild(settingsContent);

        if (showColorPicker && activeColorSetting != null) {
            ColorSetting captured = activeColorSetting;
            rootStack.addChild(new ColorPickerPopupWidget(captured.getValue(), newColor -> {
                captured.setValue(newColor);
                cc.fascinated.fascinatedutils.systems.config.ModConfig.saveSettings();
                closeColorPicker();
            }, this::closeColorPicker));
        }

        inner = rootStack;
        clearChildren();
        addChild(inner);
    }

    private void openColorPicker(ColorSetting colorSetting) {
        if (showColorPicker) {
            return;
        }
        activeColorSetting = colorSetting;
        showColorPicker = true;
        dirty = true;
    }

    private void closeColorPicker() {
        if (!showColorPicker) {
            return;
        }
        showColorPicker = false;
        activeColorSetting = null;
        dirty = true;
    }
}
