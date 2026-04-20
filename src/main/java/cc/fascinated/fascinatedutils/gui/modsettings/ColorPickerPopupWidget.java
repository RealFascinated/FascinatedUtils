package cc.fascinated.fascinatedutils.gui.modsettings;

import cc.fascinated.fascinatedutils.common.color.RainbowColors;
import cc.fascinated.fascinatedutils.common.color.SettingColor;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.widgets.FButtonWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FIconCheckboxWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FPopupWidget;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

/**
 * HSV color picker popup with saturation/value area, hue bar, rainbow toggle, preview, and apply/cancel buttons.
 * Follows the same FPopupWidget pattern as ProfileCreatePopupWidget.
 */
public class ColorPickerPopupWidget extends FPopupWidget {
    private static final float SV_SIZE_DESIGN = 84f;
    private static final float HUE_BAR_HEIGHT_DESIGN = 7f;
    private static final float PREVIEW_SIZE_DESIGN = 10f;
    private final SettingColor editingColor;
    private final Consumer<SettingColor> onApply;
    private final Runnable onCancel;
    private final FButtonWidget applyButton;
    private final FButtonWidget cancelButton;
    private final FIconCheckboxWidget rainbowToggleCheckbox;
    private float hue;
    private float saturation;
    private float value;
    private boolean rainbow;
    private boolean draggingSvArea;
    private boolean draggingHueBar;
    private float svAreaX;
    private float svAreaY;
    private float svAreaSize;
    private float hueBarX;
    private float hueBarY;
    private float hueBarWidth;
    private float hueBarHeight;

    public ColorPickerPopupWidget(SettingColor currentColor, Consumer<SettingColor> onApply, Runnable onCancel) {
        super(onCancel);
        this.editingColor = currentColor.copy();
        this.onApply = onApply;
        this.onCancel = onCancel;
        this.rainbow = currentColor.isRainbow();

        float[] hsv = currentColor.toHsv();
        this.hue = hsv[0];
        this.saturation = hsv[1];
        this.value = hsv[2];

        applyButton = new FButtonWidget(this::applyAndClose, () -> "Apply", 56f, 1, 1f, 4f, 1f, 4f);
        cancelButton = new FButtonWidget(onCancel, () -> "Cancel", 56f, 1, 1f, 4f, 1f, 4f);
        rainbowToggleCheckbox = new FIconCheckboxWidget(rainbow, checked -> {
            rainbow = checked;
            syncEditingColor();
        }, () -> "Rainbow", 84f);
        addChild(applyButton);
        addChild(cancelButton);
        addChild(rainbowToggleCheckbox);
    }

    private static boolean inRect(float pointerX, float pointerY, float rectX, float rectY, float rectWidth, float rectHeight) {
        return pointerX >= rectX && pointerX < rectX + rectWidth && pointerY >= rectY && pointerY < rectY + rectHeight;
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    @Override
    public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
        float horizontalPadding = 7f;
        float verticalPadding = 7f;
        float rowGap = 3f;

        svAreaSize = SV_SIZE_DESIGN;
        hueBarHeight = HUE_BAR_HEIGHT_DESIGN;
        float rainbowRowHeight = rainbowToggleCheckbox.intrinsicHeightForColumn(measure, svAreaSize);
        float actionsHeight = applyButton.intrinsicHeightForColumn(measure, svAreaSize);

        float popupWidth = svAreaSize + 50f * horizontalPadding;
        float titleHeight = 11f;
        float popupHeight = verticalPadding + titleHeight + rowGap + svAreaSize + rowGap + hueBarHeight + rowGap + rainbowRowHeight + rowGap + actionsHeight + verticalPadding;

        setDialogBounds(layoutX, layoutY, layoutWidth, layoutHeight, popupWidth, popupHeight);

        float bodyLeft = dialogX() + horizontalPadding;
        float bodyWidth = Math.max(0f, popupWidth - 2f * horizontalPadding);
        float cursorY = dialogY() + verticalPadding + titleHeight + rowGap;

        svAreaX = bodyLeft;
        svAreaY = cursorY;
        cursorY += svAreaSize + rowGap;

        hueBarX = bodyLeft;
        hueBarY = cursorY;
        hueBarWidth = bodyWidth;
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
    public boolean mouseDown(float pointerX, float pointerY, int button) {
        if (button != 0) {
            return super.mouseDown(pointerX, pointerY, button);
        }
        if (inRect(pointerX, pointerY, svAreaX, svAreaY, svAreaSize, svAreaSize)) {
            draggingSvArea = true;
            updateSvFromPointer(pointerX, pointerY);
            return true;
        }
        if (inRect(pointerX, pointerY, hueBarX, hueBarY, hueBarWidth, hueBarHeight)) {
            draggingHueBar = true;
            updateHueFromPointer(pointerX);
            return true;
        }
        return super.mouseDown(pointerX, pointerY, button);
    }

    @Override
    public boolean click(float pointerX, float pointerY, int button) {
        return false;
    }

    @Override
    public boolean mouseUp(float pointerX, float pointerY, int button) {
        if (button == 0) {
            draggingSvArea = false;
            draggingHueBar = false;
        }
        return super.mouseUp(pointerX, pointerY, button);
    }

    @Override
    public boolean mouseMove(float pointerX, float pointerY) {
        if (draggingSvArea) {
            updateSvFromPointer(pointerX, pointerY);
            return true;
        }
        if (draggingHueBar) {
            updateHueFromPointer(pointerX);
            return true;
        }
        return false;
    }

    @Override
    protected void renderSelf(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
        super.renderSelf(graphics, mouseX, mouseY, deltaSeconds);

        float titleY = dialogY() + 7f;
        graphics.drawCenteredText("Color Picker", dialogX() + dialogWidth() * 0.5f, titleY, graphics.theme().textPrimary(), false, true);

        renderSvPicker(graphics);
        renderHueBar(graphics);
        renderPreview(graphics);
    }

    private void renderSvPicker(GuiRenderer graphics) {
        int columns = 48;
        float columnWidth = svAreaSize / columns;
        for (int column = 0; column < columns; column++) {
            float normalizedSaturation = column / (float) (columns - 1);
            SettingColor topColor = SettingColor.fromHsv(hue, normalizedSaturation, 1f);
            int topArgb = topColor.getPackedArgb() | 0xFF000000;
            int bottomArgb = 0xFF000000;
            graphics.fillGradientVertical(svAreaX + column * columnWidth, svAreaY, columnWidth + 0.5f, svAreaSize, topArgb, bottomArgb);
        }

        float crossX = svAreaX + saturation * svAreaSize;
        float crossY = svAreaY + (1f - value) * svAreaSize;
        float crossSize = 3f;
        graphics.drawRect(crossX - crossSize, crossY - 0.5f, crossSize * 2f, 1f, 0xFFFFFFFF);
        graphics.drawRect(crossX - 0.5f, crossY - crossSize, 1f, crossSize * 2f, 0xFFFFFFFF);

        graphics.drawBorder(svAreaX, svAreaY, svAreaSize, svAreaSize, 1f, graphics.theme().border());
    }

    private void renderHueBar(GuiRenderer graphics) {
        int segments = 48;
        float segmentWidth = hueBarWidth / segments;
        for (int segment = 0; segment < segments; segment++) {
            float segmentHue = (segment / (float) segments) * 360f;
            SettingColor segmentColor = SettingColor.fromHsv(segmentHue, 1f, 1f);
            int segmentArgb = segmentColor.getPackedArgb() | 0xFF000000;
            graphics.drawRect(hueBarX + segment * segmentWidth, hueBarY, segmentWidth + 0.5f, hueBarHeight, segmentArgb);
        }

        float markerX = hueBarX + (hue / 360f) * hueBarWidth;
        float markerW = 2f;
        graphics.drawRect(markerX - markerW * 0.5f, hueBarY - 1f, markerW, hueBarHeight + 2f, 0xFFFFFFFF);
        graphics.drawBorder(hueBarX, hueBarY, hueBarWidth, hueBarHeight, 1f, graphics.theme().border());
    }

    private void renderPreview(GuiRenderer graphics) {
        float previewSize = PREVIEW_SIZE_DESIGN;
        float previewX = rainbowToggleCheckbox.x() + rainbowToggleCheckbox.w() - previewSize;
        float previewY = rainbowToggleCheckbox.y() + (rainbowToggleCheckbox.h() - previewSize) * 0.5f;
        float borderPx = 1f;

        int previewArgb = rainbow ? RainbowColors.currentColor().getPackedArgb() | 0xFF000000 : editingColor.getPackedArgb();
        graphics.fillRoundedRectFrame(previewX, previewY, previewSize, previewSize, 3f, graphics.theme().border(), previewArgb, borderPx, borderPx, RectCornerRoundMask.ALL);
    }

    private void updateSvFromPointer(float pointerX, float pointerY) {
        saturation = clamp01((pointerX - svAreaX) / Math.max(1f, svAreaSize));
        value = 1f - clamp01((pointerY - svAreaY) / Math.max(1f, svAreaSize));
        syncEditingColor();
    }

    private void updateHueFromPointer(float pointerX) {
        hue = clamp01((pointerX - hueBarX) / Math.max(1f, hueBarWidth)) * 360f;
        syncEditingColor();
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
