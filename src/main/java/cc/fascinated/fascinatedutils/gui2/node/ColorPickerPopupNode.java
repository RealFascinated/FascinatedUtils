package cc.fascinated.fascinatedutils.gui2.node;

import cc.fascinated.fascinatedutils.common.color.SettingColor;
import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ColorPickerPopupNode extends PopupNode<ColorPickerPopupNode> {

    private static final int POPUP_W = 140;
    private static final int PAD_H = 8;
    private static final int PAD_V = 8;
    private static final int GAP = 4;
    private static final int TITLE_GAP = 6;
    private static final int BODY_W = POPUP_W - PAD_H * 2;
    private static final int HUE_H = 10;
    private static final int BUTTON_H = 20;
    private static final int BUTTON_GAP = 4;
    private static final int PREVIEW_W = 20;
    private static final int HASH_W = 6;
    private static final int HASH_GAP = 2;

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
    private final cc.fascinated.fascinatedutils.gui2.node.input.TextInputNode<String> hexInput;
    private final RectNode previewRect;
    private final CheckboxNode rainbowCheckbox;
    private final ButtonNode applyButton;
    private final ButtonNode cancelButton;

    public ColorPickerPopupNode(SettingColor currentColor, Consumer<SettingColor> onApply, Runnable onCancel) {
        setPopupWidth(POPUP_W);
        setOnDismiss(onCancel);
        this.editingColor = currentColor.copy();
        this.onApply = onApply;
        float[] hsv = currentColor.toHsv();
        this.hue = hsv[0];
        this.saturation = hsv[1];
        this.value = hsv[2];

        titleNode = new TextNode(() -> Component.translatable("alumite.color_picker.title").getString());

        svArea = new SvAreaNode(hue, saturation, value, (newSaturation, newValue) -> {
            saturation = newSaturation;
            value = newValue;
            syncColor();
        });

        hueBar = new HueBarNode(hue, newHue -> {
            hue = newHue;
            svArea.setHue(newHue);
            syncColor();
        });

        hashNode = new TextNode(() -> "#");

        hexInput = new cc.fascinated.fascinatedutils.gui2.node.input.TextInputNode<>(
                cc.fascinated.fascinatedutils.gui2.node.input.TextParser.STRING)
                .setMaxLength(6)
                .setPlaceholder("RRGGBB")
                .setOnChange(this::onHexChanged);

        hexWrapper = new PositionedNode<>();
        hexWrapper.addChild(hexInput);

        previewRect = new RectNode()
                .setCornerRadius(3)
                .setFillSupplier(editingColor::getResolvedArgb)
                .setBorderResolver(theme -> theme.inputBorder());

        rainbowCheckbox = new CheckboxNode()
                .setLabel(Component.translatable("alumite.color_picker.rainbow").getString())
                .setChecked(currentColor.isRainbow())
                .setOnChange(editingColor::setRainbow);

        applyButton = new ButtonNode()
                .setLabel(Component.translatable("alumite.color_picker.apply").getString())
                .setRounded(true)
                .setOnPress(this::applyAndClose);

        cancelButton = new ButtonNode()
                .setLabel(Component.translatable("alumite.common.cancel").getString())
                .setRounded(true)
                .setOnPress(onCancel);

        addPopupChild(titleNode);
        addPopupChild(svArea);
        addPopupChild(hueBar);
        addPopupChild(hashNode);
        addPopupChild(hexWrapper);
        addPopupChild(previewRect);
        addPopupChild(rainbowCheckbox);
        addPopupChild(cancelButton);
        addPopupChild(applyButton);

        refreshHexInput();
    }

    @Override
    protected void configurePopup(RenderFrame renderFrame) {
        int fh = renderFrame.fontHeight();
        int hexInputH = 10 + fh;
        int svTop = PAD_V + fh + TITLE_GAP;
        int hueTop = svTop + BODY_W + GAP;
        int hexTop = hueTop + HUE_H + GAP;
        int checkboxH = Math.max(12, fh);
        int rainbowTop = hexTop + hexInputH + GAP;
        int buttonTop = rainbowTop + checkboxH + GAP;
        int popupH = buttonTop + BUTTON_H + PAD_V;

        setPopupHeight(popupH);

        int eachButtonW = (BODY_W - BUTTON_GAP) / 2;
        int hexInputW = BODY_W - HASH_W - HASH_GAP - PREVIEW_W - GAP;

        titleNode.top(PAD_V).left(PAD_H).right(PAD_H);
        svArea.top(svTop).left(PAD_H).size(BODY_W, BODY_W);
        hueBar.top(hueTop).left(PAD_H).size(BODY_W, HUE_H);
        hashNode.top(hexTop + (hexInputH - fh) / 2).left(PAD_H).width(HASH_W).height(fh);
        hexWrapper.top(hexTop).left(PAD_H + HASH_W + HASH_GAP).size(hexInputW, hexInputH);
        previewRect.top(hexTop + (hexInputH - PREVIEW_W) / 2).right(PAD_H).size(PREVIEW_W, PREVIEW_W);
        rainbowCheckbox.top(rainbowTop).left(PAD_H).size(BODY_W, checkboxH);
        cancelButton.top(buttonTop).left(PAD_H).size(eachButtonW, BUTTON_H);
        applyButton.top(buttonTop).left(PAD_H + eachButtonW + BUTTON_GAP).size(eachButtonW, BUTTON_H);
    }

    private void syncColor() {
        SettingColor fromHsv = SettingColor.fromHsv(hue, saturation, value);
        editingColor.setRed(fromHsv.getRed());
        editingColor.setGreen(fromHsv.getGreen());
        editingColor.setBlue(fromHsv.getBlue());
        refreshHexInput();
    }

    private void refreshHexInput() {
        hexInput.setValue(String.format("%02X%02X%02X",
                editingColor.getRed(), editingColor.getGreen(), editingColor.getBlue()));
    }

    private void onHexChanged(String text) {
        String cleaned = text.toUpperCase(Locale.ROOT).replaceAll("[^0-9A-F]", "");
        if (cleaned.length() == 6) {
            int rgb = Integer.parseInt(cleaned, 16);
            editingColor.setRed((rgb >> 16) & 0xFF);
            editingColor.setGreen((rgb >> 8) & 0xFF);
            editingColor.setBlue(rgb & 0xFF);
            editingColor.setRainbow(false);
            rainbowCheckbox.setChecked(false);
            float[] hsv = editingColor.toHsv();
            hue = hsv[0];
            saturation = hsv[1];
            value = hsv[2];
            svArea.setHue(hue);
            svArea.setSaturationValue(saturation, value);
            hueBar.setHue(hue);
        }
    }

    public void initFromColor(SettingColor color) {
        float[] hsv = color.toHsv();
        hue = hsv[0];
        saturation = hsv[1];
        value = hsv[2];
        editingColor.set(color);
        rainbowCheckbox.setChecked(color.isRainbow());
        svArea.setHue(hue);
        svArea.setSaturationValue(saturation, value);
        hueBar.setHue(hue);
        refreshHexInput();
    }

    private void applyAndClose() {
        onApply.accept(editingColor);
    }

    private static class SvAreaNode extends PositionedNode<SvAreaNode> {

        private static final int COLUMNS = 48;
        private static final int CROSSHAIR_RADIUS = 4;

        private final BiConsumer<Float, Float> onSvChanged;
        private float hue;
        private float saturation;
        private float value;
        private boolean dragging;

        SvAreaNode(float hue, float saturation, float value, BiConsumer<Float, Float> onSvChanged) {
            this.hue = hue;
            this.saturation = saturation;
            this.value = value;
            this.onSvChanged = onSvChanged;
        }

        void setHue(float hue) {
            this.hue = hue;
        }

        void setSaturationValue(float saturation, float value) {
            this.saturation = saturation;
            this.value = value;
        }

        @Override
        public boolean blocksHitWhenEmpty() {
            return true;
        }

        @Override
        public boolean onPointerPress(float pointerX, float pointerY, int button) {
            if (button != 0) return false;
            dragging = true;
            updateFromPointer(pointerX, pointerY);
            return true;
        }

        @Override
        public boolean onPointerMove(float pointerX, float pointerY) {
            if (!dragging) return false;
            updateFromPointer(pointerX, pointerY);
            return true;
        }

        @Override
        public boolean onPointerRelease(float pointerX, float pointerY, int button) {
            if (button == 0) dragging = false;
            return false;
        }

        @Override
        protected void renderSelf(RenderFrame renderFrame, float deltaSeconds) {
            int posX = bounds().positionX();
            int posY = bounds().positionY();
            int width = bounds().width();
            int height = bounds().height();

            float columnWidth = width / (float) COLUMNS;
            for (int column = 0; column < COLUMNS; column++) {
                float columnSaturation = column / (float) (COLUMNS - 1);
                SettingColor topColor = SettingColor.fromHsv(hue, columnSaturation, 1f);
                int topArgb = topColor.getPackedArgb() | 0xFF000000;
                renderFrame.drawVerticalGradient(
                        posX + Math.round(column * columnWidth), posY,
                        Math.round(columnWidth) + 1, height,
                        topArgb, 0xFF000000);
            }

            int crossX = posX + Math.round(saturation * (width - 1));
            int crossY = posY + Math.round((1f - value) * (height - 1));
            renderFrame.drawRoundedRect(crossX - CROSSHAIR_RADIUS, crossY - CROSSHAIR_RADIUS,
                    CROSSHAIR_RADIUS * 2, CROSSHAIR_RADIUS * 2, CROSSHAIR_RADIUS, 0xFF000000);
            renderFrame.drawRoundedRect(crossX - CROSSHAIR_RADIUS + 1, crossY - CROSSHAIR_RADIUS + 1,
                    CROSSHAIR_RADIUS * 2 - 2, CROSSHAIR_RADIUS * 2 - 2, CROSSHAIR_RADIUS - 1, 0xFFFFFFFF);
        }

        private void updateFromPointer(float pointerX, float pointerY) {
            int width = bounds().width();
            int height = bounds().height();
            saturation = clamp01((pointerX - bounds().positionX()) / Math.max(1, width - 1));
            value = 1f - clamp01((pointerY - bounds().positionY()) / Math.max(1, height - 1));
            onSvChanged.accept(saturation, value);
        }

        private static float clamp01(float val) {
            return Math.max(0f, Math.min(1f, val));
        }
    }

    private static class HueBarNode extends PositionedNode<HueBarNode> {

        private static final int SEGMENTS = 48;
        private static final int MARKER_HALF_W = 2;

        private final Consumer<Float> onHueChanged;
        private float hue;
        private boolean dragging;

        HueBarNode(float hue, Consumer<Float> onHueChanged) {
            this.hue = hue;
            this.onHueChanged = onHueChanged;
        }

        void setHue(float hue) {
            this.hue = hue;
        }

        @Override
        public boolean blocksHitWhenEmpty() {
            return true;
        }

        @Override
        public boolean onPointerPress(float pointerX, float pointerY, int button) {
            if (button != 0) return false;
            dragging = true;
            updateFromPointer(pointerX);
            return true;
        }

        @Override
        public boolean onPointerMove(float pointerX, float pointerY) {
            if (!dragging) return false;
            updateFromPointer(pointerX);
            return true;
        }

        @Override
        public boolean onPointerRelease(float pointerX, float pointerY, int button) {
            if (button == 0) dragging = false;
            return false;
        }

        @Override
        protected void renderSelf(RenderFrame renderFrame, float deltaSeconds) {
            int posX = bounds().positionX();
            int posY = bounds().positionY();
            int width = bounds().width();
            int height = bounds().height();

            float segmentWidth = width / (float) SEGMENTS;
            for (int segment = 0; segment < SEGMENTS; segment++) {
                float segmentHue = (segment / (float) SEGMENTS) * 360f;
                SettingColor segmentColor = SettingColor.fromHsv(segmentHue, 1f, 1f);
                int segmentArgb = segmentColor.getPackedArgb() | 0xFF000000;
                renderFrame.drawRect(posX + Math.round(segment * segmentWidth), posY,
                        Math.round(segmentWidth) + 1, height, segmentArgb);
            }

            int markerX = posX + Math.round((hue / 360f) * (width - 1));
            renderFrame.drawRect(markerX - MARKER_HALF_W, posY - 1, MARKER_HALF_W * 2, height + 2, 0xFF000000);
            renderFrame.drawRect(markerX - MARKER_HALF_W + 1, posY, MARKER_HALF_W * 2 - 2, height, 0xFFFFFFFF);
        }

        private void updateFromPointer(float pointerX) {
            hue = clamp01((pointerX - bounds().positionX()) / Math.max(1, bounds().width() - 1)) * 360f;
            onHueChanged.accept(hue);
        }

        private static float clamp01(float val) {
            return Math.max(0f, Math.min(1f, val));
        }
    }
}
