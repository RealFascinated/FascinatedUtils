package cc.fascinated.fascinatedutils.gui.widgets.settings;

import cc.fascinated.fascinatedutils.common.ColorUtils;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.gui.core.TextLineLayout;
import cc.fascinated.fascinatedutils.gui.hooks.AnimHandle;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.ModSettingsTheme;
import cc.fascinated.fascinatedutils.gui.theme.SettingsUiMetrics;
import cc.fascinated.fascinatedutils.gui.widgets.FAnimatable;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import net.minecraft.util.Mth;

public class FBooleanSettingRowWidget extends FWidget implements FAnimatable {

    // Design px; same fillet scale for track and thumb so outer and inner rounding match (not a pill track + tiny thumb).
    private static final float TOGGLE_CORNER_FILLET_DESIGN = 2.5f;
    private final Runnable onPersist;
    private final BooleanSetting booleanSetting;
    private final float outerWidth;
    private final float outerHeight;
    private final float valueColumnStartX;
    private final AnimHandle toggleProgressAnim = new AnimHandle(0f).speed(26f);
    private boolean hoveredToggle;
    private boolean hoveredReset;

    public FBooleanSettingRowWidget(Module module, BooleanSetting booleanSetting, float outerWidth, float outerHeight, float valueColumnStartX) {
        this(booleanSetting, outerWidth, outerHeight, () -> ModConfig.saveActiveModule(module), valueColumnStartX);
    }

    public FBooleanSettingRowWidget(BooleanSetting booleanSetting, float outerWidth, float outerHeight, Runnable onPersist, float valueColumnStartX) {
        this.booleanSetting = booleanSetting;
        this.onPersist = onPersist;
        this.outerWidth = outerWidth;
        this.outerHeight = outerHeight;
        this.valueColumnStartX = Math.max(0f, valueColumnStartX);
        float initial = booleanSetting.getValue() ? 1f : 0f;
        this.toggleProgressAnim.snap(initial);
    }

    public FBooleanSettingRowWidget(Module module, BooleanSetting booleanSetting, float outerWidth, float outerHeight) {
        this(module, booleanSetting, outerWidth, outerHeight, 0f);
    }

    public FBooleanSettingRowWidget(BooleanSetting booleanSetting, float outerWidth, float outerHeight, Runnable onPersist) {
        this(booleanSetting, outerWidth, outerHeight, onPersist, 0f);
    }

    private static boolean rectContains(float[] rect, float pointerX, float pointerY) {
        float rectLeft = rect[0];
        float rectTop = rect[1];
        float rectW = rect[2];
        float rectH = rect[3];
        return pointerX >= rectLeft && pointerY >= rectTop && pointerX < rectLeft + rectW && pointerY < rectTop + rectH;
    }

    @Override
    public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
        return outerHeight;
    }

    @Override
    public float intrinsicWidthForRow(UIRenderer measure, float heightBudget) {
        return outerWidth;
    }

    @Override
    public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
        setBounds(layoutX, layoutY, layoutWidth, layoutHeight);
    }

    @Override
    public boolean wantsPointer() {
        return true;
    }

    @Override
    public boolean mouseLeave(float pointerX, float pointerY) {
        hoveredToggle = false;
        hoveredReset = false;
        return false;
    }

    @Override
    public boolean mouseMove(float pointerX, float pointerY) {
        hoveredToggle = rectContains(toggleBounds(), pointerX, pointerY);
        float[] resetSquare = inlineResetSquare();
        hoveredReset = SettingRowResetLayout.resetGlyphHitActive(resetSquare[0], resetSquare[1], resetSquare[2], pointerX, pointerY, booleanSetting.isAtDefault());
        return false;
    }

    @Override
    public boolean mouseDown(float pointerX, float pointerY, int button) {
        if (button != 0) {
            return false;
        }
        if (booleanSetting.isLocked()) {
            return hoveredToggle || hoveredReset;
        }
        float[] resetSquare = inlineResetSquare();
        if (SettingRowResetLayout.resetGlyphHitActive(resetSquare[0], resetSquare[1], resetSquare[2], pointerX, pointerY, booleanSetting.isAtDefault())) {
            return true;
        }
        return rectContains(toggleBounds(), pointerX, pointerY);
    }

    @Override
    public boolean click(float pointerX, float pointerY, int button) {
        if (button != 0) {
            return false;
        }
        if (booleanSetting.isLocked()) {
            return hoveredToggle || hoveredReset;
        }
        float[] resetSquare = inlineResetSquare();
        if (SettingRowResetLayout.resetGlyphHitActive(resetSquare[0], resetSquare[1], resetSquare[2], pointerX, pointerY, booleanSetting.isAtDefault())) {
            booleanSetting.resetToDefault();
            toggleProgressAnim.target(booleanSetting.getValue() ? 1f : 0f);
            onPersist.run();
            return true;
        }
        if (rectContains(toggleBounds(), pointerX, pointerY)) {
            booleanSetting.setValue(!booleanSetting.getValue());
            toggleProgressAnim.target(booleanSetting.getValue() ? 1f : 0f);
            onPersist.run();
            return true;
        }
        return false;
    }

    @Override
    public void tickAnims(float deltaSeconds) {
        toggleProgressAnim.target(booleanSetting.getValue() ? 1f : 0f);
        toggleProgressAnim.tick(deltaSeconds);
    }

    @Override
    public void renderOverlayAfterChildren(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
        if (hoveredToggle || hoveredReset) {
            WSettingTooltip.drawTooltipForSetting(graphics, mouseX, mouseY, booleanSetting, hoveredReset);
        }
    }

    @Override
    protected void renderSelf(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
        boolean locked = booleanSetting.isLocked();
        float innerHeight = Math.max(0f, h() - 2f * SettingsUiMetrics.SETTING_ROW_PADDING_Y);
        float padY = SettingsUiMetrics.SETTING_ROW_PADDING_Y;
        float padX = SettingsUiMetrics.SETTING_ROW_PADDING_X;
        float bodyLeft = x() + padX;
        String label = booleanSetting.getTranslatedDisplayName();
        float[] toggle = toggleBounds();
        float toggleW = SettingsUiMetrics.BOOLEAN_TOGGLE_OUTER_W;
        float toggleH = SettingsUiMetrics.BOOLEAN_TOGGLE_OUTER_H;
        float titleRowHeight = Math.max(ModSettingsTheme.shellDesignBodyLineHeight(), toggleH);
        float titleOriginY = titleRowTop(innerHeight, padY, titleRowHeight);
        float labelX = bodyLeft;
        float labelMaxWidth = Math.max(0f, toggle[0] - SettingsUiMetrics.SETTING_VALUE_CONTROL_GAP - labelX);
        label = TextLineLayout.ellipsize(label, labelMaxWidth, segment -> graphics.measureTextWidth(segment, false));
        float labelY = titleOriginY + Math.max(0f, (toggleH - graphics.getFontHeight()) * 0.5f);
        int labelColor = locked ? graphics.theme().textMuted() : graphics.theme().textPrimary();
        graphics.drawMiniMessageText("<color:" + ColorUtils.rgbHex(labelColor) + ">" + label + "</color>", labelX, labelY, false);
        float progress = Mth.clamp(toggleProgressAnim.value(), 0f, 1f);
        int trackFillOff = hoveredToggle && !locked ? graphics.theme().toggleOffFillHover() : graphics.theme().toggleOffFill();
        int trackFillOn = hoveredToggle && !locked ? graphics.theme().toggleOnFillHover() : graphics.theme().toggleOnFill();
        int trackFill = ColorUtils.mixArgb(progress, trackFillOff, trackFillOn);
        int trackBorder = ColorUtils.mixArgb(progress, hoveredToggle && !locked ? graphics.theme().toggleOffBorderHover() : graphics.theme().toggleOffBorder(), graphics.theme().toggleOnBorder());
        if (locked) {
            trackFill = WSettingTooltip.dimColor(trackFill, 0.45f);
            trackBorder = WSettingTooltip.dimColor(trackBorder, 0.6f);
        }
        float borderThickness = 1f;
        float filletRadius = TOGGLE_CORNER_FILLET_DESIGN;
        float maxTrackCornerRadius = Math.max(0.5f, Math.min(toggleH * 0.5f - borderThickness * 0.5f, toggleW * 0.5f - borderThickness * 0.5f));
        float trackCornerRadius = Mth.clamp(filletRadius, 0.5f, maxTrackCornerRadius);
        graphics.fillRoundedRectFrame(toggle[0], titleOriginY, toggleW, toggleH, trackCornerRadius, trackBorder, trackFill, borderThickness, borderThickness, RectCornerRoundMask.ALL);
        float knobSize = toggleH - 5f;
        float knobTravelLeft = toggle[0] + 2f;
        float knobTravelRight = toggle[0] + toggleW - knobSize - 2f;
        float knobX = Mth.lerp(progress, knobTravelLeft, knobTravelRight);
        float knobY = titleOriginY + (toggleH - knobSize) * 0.5f;
        float maxKnobCornerRadius = Math.max(0.5f, knobSize * 0.5f - 0.01f);
        float knobCornerRadius = Mth.clamp(filletRadius, 0.5f, maxKnobCornerRadius);
        int thumbColor = locked ? WSettingTooltip.dimColor(graphics.theme().thumb(), 0.5f) : graphics.theme().thumb();
        graphics.fillRoundedRect(knobX, knobY, knobSize, knobSize, knobCornerRadius, thumbColor, RectCornerRoundMask.ALL);

        float[] resetSquare = inlineResetSquare();
        SettingRowResetLayout.paintGlyph(graphics, resetSquare[0], resetSquare[1], toggleH, hoveredReset && !locked, booleanSetting.isAtDefault());
    }

    private float[] toggleBounds() {
        float innerHeight = Math.max(0f, h() - 2f * SettingsUiMetrics.SETTING_ROW_PADDING_Y);
        float padY = SettingsUiMetrics.SETTING_ROW_PADDING_Y;
        float padX = SettingsUiMetrics.SETTING_ROW_PADDING_X;
        float toggleW = SettingsUiMetrics.BOOLEAN_TOGGLE_OUTER_W;
        float toggleH = SettingsUiMetrics.BOOLEAN_TOGGLE_OUTER_H;
        float titleRowHeight = Math.max(ModSettingsTheme.shellDesignBodyLineHeight(), toggleH);
        float titleOriginY = titleRowTop(innerHeight, padY, titleRowHeight);
        float bodyLeft = x() + padX;
        float resetLeft = SettingRowResetLayout.trailingResetLeftX(x() + w());
        float maxToggleLeft = Math.max(bodyLeft, resetLeft - SettingsUiMetrics.SETTING_VALUE_CONTROL_GAP - toggleW);
        float toggleLeft = Mth.clamp(bodyLeft + valueColumnStartX, bodyLeft, maxToggleLeft);
        return new float[]{toggleLeft, titleOriginY, toggleW, toggleH};
    }

    private float[] inlineResetSquare() {
        float[] toggle = toggleBounds();
        float toggleH = toggle[3];
        float resetLeft = SettingRowResetLayout.trailingResetLeftX(x() + w());
        float resetTop = SettingRowResetLayout.verticallyCenteredTop(toggle[1], toggleH);
        float box = SettingRowResetLayout.glyphBoxPx();
        return new float[]{resetLeft, resetTop, box};
    }

    private float titleRowTop(float innerHeight, float padY, float titleRowHeight) {
        float bodyTop = y() + padY;
        return bodyTop + (innerHeight - titleRowHeight) * 0.5f;
    }
}
