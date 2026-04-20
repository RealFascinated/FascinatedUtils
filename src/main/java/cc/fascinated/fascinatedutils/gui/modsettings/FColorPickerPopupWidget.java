package cc.fascinated.fascinatedutils.gui.modsettings;

import java.util.function.Consumer;

import org.lwjgl.glfw.GLFW;

import cc.fascinated.fascinatedutils.common.color.RainbowColors;
import cc.fascinated.fascinatedutils.common.color.SettingColor;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.widgets.FButtonWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FIconCheckboxWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FPopupWidget;

/**
 * HSV color picker popup with saturation/value area, hue bar, rainbow toggle, preview, and apply/cancel buttons.
 * Follows the same FPopupWidget pattern as ProfileCreatePopupWidget.
 */
public class FColorPickerPopupWidget extends FPopupWidget {
    private static final float SV_SIZE_DESIGN = 84f;
    private static final float HUE_BAR_HEIGHT_DESIGN = 7f;
    private static final float PREVIEW_SIZE_DESIGN = 10f;
    private final SettingColor editingColor;
    private final Consumer<SettingColor> onApply;
    private final Runnable onCancel;
    private final FSvPickerAreaWidget svPickerArea;
    private final FHueBarWidget hueBar;
    private final FButtonWidget applyButton;
    private final FButtonWidget cancelButton;
    private final FIconCheckboxWidget rainbowToggleCheckbox;
    private float hue;
    private float saturation;
    private float value;
    private boolean rainbow;

    public FColorPickerPopupWidget(SettingColor currentColor, Consumer<SettingColor> onApply, Runnable onCancel) {
        super(onCancel);
        this.editingColor = currentColor.copy();
        this.onApply = onApply;
        this.onCancel = onCancel;
        this.rainbow = currentColor.isRainbow();

        float[] hsv = currentColor.toHsv();
        this.hue = hsv[0];
        this.saturation = hsv[1];
        this.value = hsv[2];

        svPickerArea = new FSvPickerAreaWidget(hue, saturation, value, (newSaturation, newValue) -> {
            saturation = newSaturation;
            value = newValue;
            syncEditingColor();
        });
        hueBar = new FHueBarWidget(hue, newHue -> {
            hue = newHue;
            svPickerArea.setHue(newHue);
            syncEditingColor();
        });
        applyButton = new FButtonWidget(this::applyAndClose, () -> "Apply", 56f, 1, 1f, 4f, 1f, 4f);
        cancelButton = new FButtonWidget(onCancel, () -> "Cancel", 56f, 1, 1f, 4f, 1f, 4f);
        rainbowToggleCheckbox = new FIconCheckboxWidget(rainbow, checked -> {
            rainbow = checked;
            syncEditingColor();
        }, () -> "Rainbow", 84f);
        addChild(svPickerArea);
        addChild(hueBar);
        addChild(applyButton);
        addChild(cancelButton);
        addChild(rainbowToggleCheckbox);
    }

    @Override
    public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
        float horizontalPadding = 7f;
        float verticalPadding = 7f;
        float rowGap = 3f;

        float svAreaSize = SV_SIZE_DESIGN;
        float hueBarHeight = HUE_BAR_HEIGHT_DESIGN;
        float rainbowRowHeight = rainbowToggleCheckbox.intrinsicHeightForColumn(measure, svAreaSize);
        float actionsHeight = applyButton.intrinsicHeightForColumn(measure, svAreaSize);

        float popupWidth = svAreaSize + 50f * horizontalPadding;
        float titleHeight = 11f;
        float popupHeight = verticalPadding + titleHeight + rowGap + svAreaSize + rowGap + hueBarHeight + rowGap + rainbowRowHeight + rowGap + actionsHeight + verticalPadding;

        setDialogBounds(layoutX, layoutY, layoutWidth, layoutHeight, popupWidth, popupHeight);

        float bodyLeft = dialogX() + horizontalPadding;
        float bodyWidth = Math.max(0f, popupWidth - 2f * horizontalPadding);
        float cursorY = dialogY() + verticalPadding + titleHeight + rowGap;

        svPickerArea.layout(measure, bodyLeft, cursorY, svAreaSize, svAreaSize);
        cursorY += svAreaSize + rowGap;

        hueBar.layout(measure, bodyLeft, cursorY, bodyWidth, hueBarHeight);
        cursorY += hueBarHeight + rowGap;

        rainbowToggleCheckbox.setOuterWidth(bodyWidth);
        rainbowToggleCheckbox.setChecked(rainbow);
        rainbowToggleCheckbox.layout(measure, bodyLeft, cursorY, bodyWidth, rainbowRowHeight);
        cursorY += rainbowRowHeight + rowGap;

        float actionGap = 3f;
        float actionWidth = Math.max(0f, (bodyWidth - actionGap) * 0.5f);
        float buttonHeight = applyButton.intrinsicHeightForColumn(measure, actionWidth);
        applyButton.layout(measure, bodyLeft, cursorY, actionWidth, buttonHeight);
        cancelButton.layout(measure, bodyLeft + actionWidth + actionGap, cursorY, actionWidth, buttonHeight);
    }

    @Override
    public boolean keyDownCapture(int keyCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            applyAndClose();
            return true;
        }
        return super.keyDownCapture(keyCode, modifiers);
    }

    @Override
    protected void renderSelf(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
        super.renderSelf(graphics, mouseX, mouseY, deltaSeconds);
        float titleY = dialogY() + 7f;
        graphics.drawCenteredText("Color Picker", dialogX() + dialogWidth() * 0.5f, titleY, graphics.theme().textPrimary(), false, true);
        renderPreview(graphics);
    }

    private void renderPreview(GuiRenderer graphics) {
        float previewSize = PREVIEW_SIZE_DESIGN;
        float previewX = rainbowToggleCheckbox.x() + rainbowToggleCheckbox.w() - previewSize;
        float previewY = rainbowToggleCheckbox.y() + (rainbowToggleCheckbox.h() - previewSize) * 0.5f;
        float borderPx = 1f;

        int previewArgb = rainbow ? RainbowColors.currentColor().getPackedArgb() | 0xFF000000 : editingColor.getPackedArgb();
        graphics.fillRoundedRectFrame(previewX, previewY, previewSize, previewSize, 3f, graphics.theme().border(), previewArgb, borderPx, borderPx, RectCornerRoundMask.ALL);
    }

    private void syncEditingColor() {
        SettingColor fromHsv = SettingColor.fromHsv(hue, saturation, value);
        editingColor.setRed(fromHsv.getRed());
        editingColor.setGreen(fromHsv.getGreen());
        editingColor.setBlue(fromHsv.getBlue());
        editingColor.setRainbow(rainbow);
    }

    private void applyAndClose() {
        syncEditingColor();
        onApply.accept(editingColor);
        onCancel.run();
    }
}

