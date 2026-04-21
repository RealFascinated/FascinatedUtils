package cc.fascinated.fascinatedutils.gui.modsettings;

import java.util.Locale;
import java.util.function.Consumer;

import org.lwjgl.glfw.GLFW;

import cc.fascinated.fascinatedutils.common.color.RainbowColors;
import cc.fascinated.fascinatedutils.common.color.SettingColor;
import cc.fascinated.fascinatedutils.gui.core.GuiFocusState;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.widgets.FButtonWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FIconCheckboxWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FOutlinedTextInputWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FPopupWidget;

/**
 * HSV color picker popup with saturation/value area, hue bar, rainbow toggle, preview, and apply/cancel buttons.
 * Follows the same FPopupWidget pattern as ProfileCreatePopupWidget.
 */
public class FColorPickerPopupWidget extends FPopupWidget {
    private static final int HEX_INPUT_FOCUS_ID = 5300;
    private static final float SV_SIZE_DESIGN = 98f;
    private static final float HUE_BAR_HEIGHT_DESIGN = 9f;
    private static final float PREVIEW_SIZE_DESIGN = 12f;
    private static final float HEX_HASH_LABEL_WIDTH = 8f;
    private final SettingColor editingColor;
    private final Consumer<SettingColor> onApply;
    private final Runnable onCancel;
    private final FSvPickerAreaWidget svPickerArea;
    private final FHueBarWidget hueBar;
    private final FButtonWidget applyButton;
    private final FButtonWidget cancelButton;
    private final FIconCheckboxWidget rainbowToggleCheckbox;
    private final FOutlinedTextInputWidget hexInput;
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
        hexInput = new FOutlinedTextInputWidget(HEX_INPUT_FOCUS_ID, 6, 13f, () -> "RRGGBB");
        hexInput.setExternalFocusIdSupplier(GuiFocusState::getFocusedId);
        hexInput.setValue(toHexString());
        hexInput.setOnChange(this::onHexChanged);
        addChild(svPickerArea);
        addChild(hueBar);
        addChild(hexInput);
        addChild(applyButton);
        addChild(cancelButton);
        addChild(rainbowToggleCheckbox);
    }

    @Override
    public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
        float horizontalPadding = 7f;
        float verticalPadding = 7f;
        float rowGap = 3f;

        float bodyWidth = SV_SIZE_DESIGN;
        float popupWidth = bodyWidth + 2f * horizontalPadding;
        float svAreaSize = bodyWidth;
        float hueBarHeight = HUE_BAR_HEIGHT_DESIGN;
        float hexRowHeight = hexInput.intrinsicHeightForColumn(measure, bodyWidth);
        float rainbowRowHeight = rainbowToggleCheckbox.intrinsicHeightForColumn(measure, bodyWidth);
        float actionsHeight = applyButton.intrinsicHeightForColumn(measure, bodyWidth);

        float titleHeight = 11f;
        float popupHeight = verticalPadding + titleHeight + rowGap + svAreaSize + rowGap + hueBarHeight + rowGap + hexRowHeight + rowGap + rainbowRowHeight + rowGap + actionsHeight + verticalPadding;

        setDialogBounds(layoutX, layoutY, layoutWidth, layoutHeight, popupWidth, popupHeight);

        float bodyLeft = dialogX() + horizontalPadding;
        float cursorY = dialogY() + verticalPadding + titleHeight + rowGap;

        svPickerArea.layout(measure, bodyLeft, cursorY, svAreaSize, svAreaSize);
        cursorY += svAreaSize + rowGap;

        hueBar.layout(measure, bodyLeft, cursorY, bodyWidth, hueBarHeight);
        cursorY += hueBarHeight + rowGap;

        float hexInputWidth = Math.max(1f, bodyWidth - HEX_HASH_LABEL_WIDTH - 4f - PREVIEW_SIZE_DESIGN);
        hexInput.layout(measure, bodyLeft + HEX_HASH_LABEL_WIDTH, cursorY, hexInputWidth, hexRowHeight);
        cursorY += hexRowHeight + rowGap;

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
        renderHexRow(graphics);
    }

    private void renderHexRow(GuiRenderer graphics) {
        float textY = hexInput.y() + Math.max(0f, hexInput.h() - graphics.getFontCapHeight()) * 0.5f;
        graphics.drawText("#", hexInput.x() - HEX_HASH_LABEL_WIDTH + 1f, textY, graphics.theme().textLabel(), false, false);
        float previewSize = PREVIEW_SIZE_DESIGN;
        float previewX = hexInput.x() + hexInput.w() + 4f;
        float previewY = hexInput.y() + (hexInput.h() - previewSize) * 0.5f;
        int previewArgb = rainbow ? RainbowColors.currentColor().getPackedArgb() | 0xFF000000 : editingColor.getPackedArgb() | 0xFF000000;
        graphics.fillRoundedRectFrame(previewX, previewY, previewSize, previewSize, 3f, graphics.theme().border(), previewArgb, 1f, 1f, RectCornerRoundMask.ALL);
    }

    private void syncEditingColor() {
        SettingColor fromHsv = SettingColor.fromHsv(hue, saturation, value);
        editingColor.setRed(fromHsv.getRed());
        editingColor.setGreen(fromHsv.getGreen());
        editingColor.setBlue(fromHsv.getBlue());
        editingColor.setRainbow(rainbow);
        if (GuiFocusState.getFocusedId() != HEX_INPUT_FOCUS_ID) {
            hexInput.setValue(toHexString());
        }
    }

    private String toHexString() {
        return String.format("%02X%02X%02X", editingColor.getRed(), editingColor.getGreen(), editingColor.getBlue());
    }

    private void onHexChanged(String text) {
        String cleaned = text.toUpperCase(Locale.ROOT).replaceAll("[^0-9A-F]", "");
        if (!cleaned.equals(text)) {
            hexInput.setValue(cleaned);
        }
        if (cleaned.length() == 6) {
            int rgb = Integer.parseInt(cleaned, 16);
            editingColor.setRed((rgb >> 16) & 0xFF);
            editingColor.setGreen((rgb >> 8) & 0xFF);
            editingColor.setBlue(rgb & 0xFF);
            editingColor.setRainbow(false);
            float[] hsv = editingColor.toHsv();
            hue = hsv[0];
            saturation = hsv[1];
            value = hsv[2];
            svPickerArea.setHue(hue);
            svPickerArea.setSaturationValue(saturation, value);
            hueBar.setHue(hue);
            rainbow = false;
        }
    }

    private void applyAndClose() {
        syncEditingColor();
        onApply.accept(editingColor);
        onCancel.run();
    }
}

