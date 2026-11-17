package cc.fascinated.fascinatedutils.gui.node.popup;

import cc.fascinated.fascinatedutils.gui.node.control.CheckboxNode;

import cc.fascinated.fascinatedutils.gui.node.control.ButtonNode;

import cc.fascinated.fascinatedutils.gui.node.display.RectNode;

import cc.fascinated.fascinatedutils.gui.node.display.TextNode;

import cc.fascinated.fascinatedutils.common.color.SettingColor;
import cc.fascinated.fascinatedutils.gui.components.UiTranslations;
import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.gui.node.input.TextInputNode;
import cc.fascinated.fascinatedutils.gui.node.input.TextParser;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UiMetrics;
import net.minecraft.util.Mth;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * A popup for editing a {@link SettingColor} with a saturation and value area, a hue bar, a hex field and a rainbow
 * toggle. The editing state lives in this node and is carried to the freshly composed popup by {@link #inheritState},
 * so a screen can compose a new popup on every recompose.
 */
public class ColorPickerPopupNode extends PopupNode<ColorPickerPopupNode> {

    /**
     * Bottom edge of the saturation and value area. It is black by definition of the HSV model, whatever the theme.
     */
    private static final int OPAQUE_BLACK = 0xFF000000;
    private static final int OPAQUE_ALPHA = 0xFF000000;
    private static final int POPUP_WIDTH = 140;
    private static final int PAD_X = 8;
    private static final int PAD_Y = 8;
    private static final int GAP = 4;
    private static final int TITLE_GAP = 6;
    private static final int BODY_WIDTH = POPUP_WIDTH - PAD_X * 2;
    private static final int HUE_HEIGHT = 10;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 4;
    private static final int PREVIEW_SIZE = 20;
    private static final int PREVIEW_RADIUS = 3;
    private static final int HASH_WIDTH = 6;
    private static final int HASH_GAP = 2;
    private static final int MIN_CHECKBOX_HEIGHT = 12;
    private static final int HEX_MAX_LENGTH = 7;
    private static final float DEGREES = 360f;

    private final SettingColor editingColor;
    private final Consumer<SettingColor> onApply;
    private float hue;
    private float saturation;
    private float value;

    private final TextNode titleNode;
    private final SvAreaNode svArea;
    private final HueBarNode hueBar;
    private final TextNode hashNode;
    private final PositionedNode<?> hexWrapper;
    private final TextInputNode<String> hexInput;
    private final RectNode previewRect;
    private final CheckboxNode rainbowCheckbox;
    private final ButtonNode applyButton;
    private final ButtonNode cancelButton;

    /**
     * Creates the popup.
     *
     * @param currentColor the colour to start editing, copied
     * @param onApply      receives a copy of the edited colour when the user applies it
     * @param onCancel     runs when the user cancels or dismisses the popup
     */
    public ColorPickerPopupNode(SettingColor currentColor, Consumer<SettingColor> onApply, Runnable onCancel) {
        setPopupWidth(POPUP_WIDTH);
        setOnDismiss(onCancel);
        this.editingColor = currentColor.copy();
        this.onApply = onApply;

        titleNode = new TextNode(() -> UiTranslations.INSTANCE.get("alumite.color_picker.title"));
        svArea = new SvAreaNode();
        hueBar = new HueBarNode();
        hashNode = new TextNode(() -> "#");
        hexInput = new TextInputNode<>(TextParser.STRING)
                .setMaxLength(HEX_MAX_LENGTH)
                .setPlaceholder("RRGGBB")
                .setOnChange(this::onHexChanged)
                .setOnSubmit(this::onHexSubmitted);
        hexWrapper = new PositionedNode<>();
        hexWrapper.addChild(hexInput);
        previewRect = new RectNode()
                .setCornerRadius(PREVIEW_RADIUS)
                .setFillSupplier(editingColor::getResolvedArgb)
                .setBorderResolver(theme -> theme.inputBorder());
        rainbowCheckbox = new CheckboxNode()
                .setLabel(() -> UiTranslations.INSTANCE.get("alumite.color_picker.rainbow"))
                .setOnChange(editingColor::setRainbow);
        applyButton = new ButtonNode(() -> UiTranslations.INSTANCE.get("alumite.color_picker.apply")).setOnPress(this::applyAndClose);
        cancelButton = new ButtonNode(() -> UiTranslations.INSTANCE.get("alumite.common.cancel")).setOnPress(onCancel);

        for (UiNode childNode : new UiNode[]{titleNode, svArea, hueBar, hashNode, hexWrapper, previewRect, rainbowCheckbox, cancelButton, applyButton}) {
            addChild(childNode);
        }
        svArea.full();
        hueBar.full();
        titleNode.full();
        hashNode.full();
        hexWrapper.full();
        previewRect.full();
        rainbowCheckbox.full();
        cancelButton.full();
        applyButton.full();

        adoptEditingColor();
    }

    @Override
    protected void inheritState(UiNode previous) {
        super.inheritState(previous);
        ColorPickerPopupNode previousPopup = (ColorPickerPopupNode) previous;
        editingColor.set(previousPopup.editingColor);
        hue = previousPopup.hue;
        saturation = previousPopup.saturation;
        value = previousPopup.value;
        rainbowCheckbox.setChecked(editingColor.isRainbow());
        // Text the user is typing in the hex field survives; it is only rewritten when something else changed the colour.
        if (!previousPopup.hexInput.isFocused()) {
            refreshHexInput();
        }
    }

    private int hexInputHeight(int fontHeight) {
        return UiMetrics.INPUT_PAD_Y * 2 + fontHeight;
    }

    private int checkboxHeight(int fontHeight) {
        return Math.max(MIN_CHECKBOX_HEIGHT, fontHeight);
    }

    @Override
    protected void configurePopup(AlumiteGuiRenderer renderFrame, int availableWidth, int availableHeight) {
        int fontHeight = renderFrame.fontHeight();
        int svTop = PAD_Y + fontHeight + TITLE_GAP;
        int hexTop = svTop + BODY_WIDTH + GAP + HUE_HEIGHT + GAP;
        int buttonTop = hexTop + hexInputHeight(fontHeight) + GAP + checkboxHeight(fontHeight) + GAP;
        setPopupHeight(buttonTop + BUTTON_HEIGHT + PAD_Y);
    }

    @Override
    protected void arrangePopupChildren(AlumiteGuiRenderer renderFrame, int panelX, int panelY, int panelWidth, int panelHeight) {
        int fontHeight = renderFrame.fontHeight();
        int hexHeight = hexInputHeight(fontHeight);
        int checkboxHeight = checkboxHeight(fontHeight);
        int svTop = PAD_Y + fontHeight + TITLE_GAP;
        int hueTop = svTop + BODY_WIDTH + GAP;
        int hexTop = hueTop + HUE_HEIGHT + GAP;
        int rainbowTop = hexTop + hexHeight + GAP;
        int buttonTop = rainbowTop + checkboxHeight + GAP;
        int buttonWidth = (BODY_WIDTH - BUTTON_GAP) / 2;
        int hexWidth = BODY_WIDTH - HASH_WIDTH - HASH_GAP - PREVIEW_SIZE - GAP;
        int bodyX = panelX + PAD_X;

        titleNode.layout(renderFrame, bodyX, panelY + PAD_Y, BODY_WIDTH, fontHeight);
        svArea.layout(renderFrame, bodyX, panelY + svTop, BODY_WIDTH, BODY_WIDTH);
        hueBar.layout(renderFrame, bodyX, panelY + hueTop, BODY_WIDTH, HUE_HEIGHT);
        hashNode.layout(renderFrame, bodyX, panelY + hexTop + (hexHeight - fontHeight) / 2, HASH_WIDTH, fontHeight);
        hexWrapper.layout(renderFrame, bodyX + HASH_WIDTH + HASH_GAP, panelY + hexTop, hexWidth, hexHeight);
        previewRect.layout(renderFrame, panelX + panelWidth - PAD_X - PREVIEW_SIZE, panelY + hexTop + (hexHeight - PREVIEW_SIZE) / 2, PREVIEW_SIZE, PREVIEW_SIZE);
        rainbowCheckbox.layout(renderFrame, bodyX, panelY + rainbowTop, BODY_WIDTH, checkboxHeight);
        cancelButton.layout(renderFrame, bodyX, panelY + buttonTop, buttonWidth, BUTTON_HEIGHT);
        applyButton.layout(renderFrame, bodyX + buttonWidth + BUTTON_GAP, panelY + buttonTop, buttonWidth, BUTTON_HEIGHT);
    }

    /**
     * Derives hue, saturation, value, the rainbow toggle and the hex field from the edited colour.
     */
    private void adoptEditingColor() {
        float[] hsv = editingColor.toHsv();
        hue = hsv[0];
        saturation = hsv[1];
        value = hsv[2];
        rainbowCheckbox.setChecked(editingColor.isRainbow());
        refreshHexInput();
    }

    /**
     * Writes the picker's hue, saturation and value back into the edited colour and the hex field.
     */
    private void syncColor() {
        int rgb = Mth.hsvToRgb(hue / DEGREES, saturation, value);
        editingColor.setRed((rgb >> 16) & 0xFF);
        editingColor.setGreen((rgb >> 8) & 0xFF);
        editingColor.setBlue(rgb & 0xFF);
        // Rewriting the field would reset the caret and undo history of text being typed in it.
        if (!hexInput.isFocused()) {
            refreshHexInput();
        }
    }

    private void refreshHexInput() {
        hexInput.setValue(String.format(Locale.ROOT, "%02X%02X%02X", editingColor.getRed(), editingColor.getGreen(), editingColor.getBlue()));
    }

    private void onHexChanged(String text) {
        String digits = hexDigits(text);
        if (digits.length() == 6) {
            applyHex(Integer.parseInt(digits, 16));
        }
    }

    private void onHexSubmitted(String text) {
        String digits = hexDigits(text);
        if (digits.length() == 3) {
            int red = Character.digit(digits.charAt(0), 16);
            int green = Character.digit(digits.charAt(1), 16);
            int blue = Character.digit(digits.charAt(2), 16);
            applyHex((red * 0x11) << 16 | (green * 0x11) << 8 | blue * 0x11);
            refreshHexInput();
        } else if (digits.length() == 6) {
            applyHex(Integer.parseInt(digits, 16));
            refreshHexInput();
        }
    }

    private static String hexDigits(String text) {
        return text.toUpperCase(Locale.ROOT).replaceAll("[^0-9A-F]", "");
    }

    private void applyHex(int rgb) {
        editingColor.setRed((rgb >> 16) & 0xFF);
        editingColor.setGreen((rgb >> 8) & 0xFF);
        editingColor.setBlue(rgb & 0xFF);
        editingColor.setRainbow(false);
        float[] hsv = editingColor.toHsv();
        hue = hsv[0];
        saturation = hsv[1];
        value = hsv[2];
        rainbowCheckbox.setChecked(false);
    }

    private void applyAndClose() {
        // Hand over a copy: the stored colour must not alias the one this popup keeps editing.
        onApply.accept(editingColor.copy());
    }

    private class SvAreaNode extends PositionedNode<SvAreaNode> {

        private static final int CROSSHAIR_RADIUS = 4;

        @Override
        public boolean hitTarget() {
            return true;
        }

        @Override
        public boolean onPointerPress(float pointerX, float pointerY, int button) {
            if (button != 0) {
                return false;
            }
            updateFromPointer(pointerX, pointerY);
            return true;
        }

        @Override
        public boolean onPointerMove(float pointerX, float pointerY) {
            if (!isPressed()) {
                return false;
            }
            updateFromPointer(pointerX, pointerY);
            return true;
        }

        @Override
        protected void renderSelf(AlumiteGuiRenderer renderFrame, float deltaSeconds) {
            int positionX = bounds().positionX();
            int positionY = bounds().positionY();
            int width = bounds().width();
            int height = bounds().height();

            float hueFraction = hue / DEGREES;
            for (int column = 0; column < width; column++) {
                float columnSaturation = width > 1 ? column / (float) (width - 1) : 0f;
                int topArgb = OPAQUE_ALPHA | Mth.hsvToRgb(hueFraction, columnSaturation, 1f);
                renderFrame.drawVerticalGradient(positionX + column, positionY, 1, height, topArgb, OPAQUE_BLACK);
            }

            int crossX = positionX + Math.round(saturation * (width - 1));
            int crossY = positionY + Math.round((1f - value) * (height - 1));
            renderFrame.drawRoundedRect(crossX - CROSSHAIR_RADIUS, crossY - CROSSHAIR_RADIUS,
                    CROSSHAIR_RADIUS * 2, CROSSHAIR_RADIUS * 2, CROSSHAIR_RADIUS, renderFrame.theme().crosshairOuter());
            renderFrame.drawRoundedRect(crossX - CROSSHAIR_RADIUS + 1, crossY - CROSSHAIR_RADIUS + 1,
                    CROSSHAIR_RADIUS * 2 - 2, CROSSHAIR_RADIUS * 2 - 2, CROSSHAIR_RADIUS - 1, renderFrame.theme().crosshairInner());
        }

        private void updateFromPointer(float pointerX, float pointerY) {
            saturation = Mth.clamp((pointerX - bounds().positionX()) / Math.max(1, bounds().width() - 1), 0f, 1f);
            value = 1f - Mth.clamp((pointerY - bounds().positionY()) / Math.max(1, bounds().height() - 1), 0f, 1f);
            syncColor();
        }
    }

    private class HueBarNode extends PositionedNode<HueBarNode> {

        private static final int MARKER_HALF_WIDTH = 2;

        @Override
        public boolean hitTarget() {
            return true;
        }

        @Override
        public boolean onPointerPress(float pointerX, float pointerY, int button) {
            if (button != 0) {
                return false;
            }
            updateFromPointer(pointerX);
            return true;
        }

        @Override
        public boolean onPointerMove(float pointerX, float pointerY) {
            if (!isPressed()) {
                return false;
            }
            updateFromPointer(pointerX);
            return true;
        }

        @Override
        protected void renderSelf(AlumiteGuiRenderer renderFrame, float deltaSeconds) {
            int positionX = bounds().positionX();
            int positionY = bounds().positionY();
            int width = bounds().width();
            int height = bounds().height();

            for (int column = 0; column < width; column++) {
                float columnHue = width > 1 ? column / (float) (width - 1) : 0f;
                renderFrame.drawRect(positionX + column, positionY, 1, height, OPAQUE_ALPHA | Mth.hsvToRgb(columnHue, 1f, 1f));
            }

            int markerX = positionX + Math.round((hue / DEGREES) * (width - 1));
            renderFrame.drawRect(markerX - MARKER_HALF_WIDTH, positionY - 1, MARKER_HALF_WIDTH * 2, height + 2, renderFrame.theme().crosshairOuter());
            renderFrame.drawRect(markerX - MARKER_HALF_WIDTH + 1, positionY, MARKER_HALF_WIDTH * 2 - 2, height, renderFrame.theme().crosshairInner());
        }

        private void updateFromPointer(float pointerX) {
            hue = Mth.clamp((pointerX - bounds().positionX()) / Math.max(1, bounds().width() - 1), 0f, 1f) * DEGREES;
            syncColor();
        }
    }
}
