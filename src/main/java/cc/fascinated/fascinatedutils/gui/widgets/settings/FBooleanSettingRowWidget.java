package cc.fascinated.fascinatedutils.gui.widgets.settings;

import cc.fascinated.fascinatedutils.common.ColorUtils;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.gui.core.TextLineLayout;
import cc.fascinated.fascinatedutils.gui.hooks.AnimHandle;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.theme.Icons;
import cc.fascinated.fascinatedutils.gui.theme.ModSettingsTheme;
import cc.fascinated.fascinatedutils.gui.theme.SettingsUiMetrics;
import cc.fascinated.fascinatedutils.gui.widgets.FAnimatable;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

import java.util.function.BooleanSupplier;

public class FBooleanSettingRowWidget extends FSettingRowWidget implements FAnimatable {

    // Design px; same fillet scale for track and thumb so outer and inner rounding match (not a pill track + tiny thumb).
    private static final float TOGGLE_CORNER_FILLET_DESIGN = 2.5f;
    private final BooleanSetting booleanSetting;
    private final AnimHandle toggleProgressAnim = new AnimHandle(0f).speed(26f);
    private boolean hoveredToggle;
    private boolean hoveredReset;
    private boolean hoveredChevron;
    private @Nullable BooleanSupplier expandedState;
    private @Nullable Runnable onChevronToggle;

    public FBooleanSettingRowWidget(Module module, BooleanSetting booleanSetting, float outerWidth, float outerHeight, float valueColumnStartX) {
        this(booleanSetting, outerWidth, outerHeight, () -> ModConfig.profiles().saveModule(module), valueColumnStartX);
    }

    public FBooleanSettingRowWidget(BooleanSetting booleanSetting, float outerWidth, float outerHeight, Runnable onPersist, float valueColumnStartX) {
        super(outerWidth, outerHeight, onPersist, valueColumnStartX);
        this.booleanSetting = booleanSetting;
        float initial = booleanSetting.getValue() ? 1f : 0f;
        this.toggleProgressAnim.snap(initial);
    }

    public FBooleanSettingRowWidget(Module module, BooleanSetting booleanSetting, float outerWidth, float outerHeight) {
        this(module, booleanSetting, outerWidth, outerHeight, 0f);
    }

    public FBooleanSettingRowWidget(BooleanSetting booleanSetting, float outerWidth, float outerHeight, Runnable onPersist) {
        this(booleanSetting, outerWidth, outerHeight, onPersist, 0f);
    }

    /**
     * Wires a chevron button that controls an expand/collapse state. When set, a chevron icon is rendered between the
     * toggle and the reset glyph; clicking it calls {@code onToggle}.
     *
     * @param expanded  supplier for whether the sub-panel is currently expanded
     * @param onToggle  called when the user clicks the chevron
     */
    public void setChevronHandlers(BooleanSupplier expanded, Runnable onToggle) {
        this.expandedState = expanded;
        this.onChevronToggle = onToggle;
    }

    @Override
    public boolean mouseLeave(float pointerX, float pointerY) {
        hoveredToggle = false;
        hoveredReset = false;
        hoveredChevron = false;
        return false;
    }

    @Override
    public boolean mouseMove(float pointerX, float pointerY) {
        hoveredToggle = rectContains(toggleBounds(), pointerX, pointerY);
        float[] resetSquare = inlineResetSquare();
        hoveredReset = SettingRowResetLayout.resetGlyphHitActive(resetSquare[0], resetSquare[1], resetSquare[2], pointerX, pointerY, booleanSetting.isAtDefault());
        if (expandedState != null) {
            float[] chevron = chevronBounds();
            hoveredChevron = SettingRowResetLayout.rectContains(chevron[0], chevron[1], chevron[2], pointerX, pointerY);
        }
        return false;
    }

    @Override
    public boolean mouseDown(float pointerX, float pointerY, int button) {
        if (button != 0) {
            return false;
        }
        if (booleanSetting.isLocked()) {
            return hoveredToggle || hoveredReset || hoveredChevron;
        }
        float[] resetSquare = inlineResetSquare();
        if (SettingRowResetLayout.resetGlyphHitActive(resetSquare[0], resetSquare[1], resetSquare[2], pointerX, pointerY, booleanSetting.isAtDefault())) {
            return true;
        }
        if (expandedState != null) {
            float[] chevron = chevronBounds();
            if (SettingRowResetLayout.rectContains(chevron[0], chevron[1], chevron[2], pointerX, pointerY)) {
                return true;
            }
        }
        return rectContains(toggleBounds(), pointerX, pointerY);
    }

    @Override
    public boolean click(float pointerX, float pointerY, int button) {
        if (button != 0) {
            return false;
        }
        if (booleanSetting.isLocked()) {
            return hoveredToggle || hoveredReset || hoveredChevron;
        }
        float[] resetSquare = inlineResetSquare();
        if (SettingRowResetLayout.resetGlyphHitActive(resetSquare[0], resetSquare[1], resetSquare[2], pointerX, pointerY, booleanSetting.isAtDefault())) {
            booleanSetting.resetToDefault();
            toggleProgressAnim.target(booleanSetting.getValue() ? 1f : 0f);
            onPersist.run();
            return true;
        }
        if (expandedState != null && onChevronToggle != null) {
            float[] chevron = chevronBounds();
            if (SettingRowResetLayout.rectContains(chevron[0], chevron[1], chevron[2], pointerX, pointerY)) {
                onChevronToggle.run();
                return true;
            }
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
        if (hoveredToggle || hoveredReset || hoveredChevron) {
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

        if (expandedState != null) {
            float[] chevron = chevronBounds();
            boolean expanded = expandedState.getAsBoolean();
            int chevronColor = hoveredChevron && !locked ? graphics.theme().textPrimary() : graphics.theme().textMuted();
            Icons.paintSubSettingsChevron(graphics, chevron[0], chevron[1], chevron[2], chevron[2], chevronColor, expanded);
        }
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
        float resetLeft = SettingRowResetLayout.trailingResetLeftX(x() + outerWidth);
        // Always reserve chevron space so plain and chevron booleans cap at the same X.
        float chevronReserve = SettingRowResetLayout.glyphBoxPx() + SettingsUiMetrics.SETTING_VALUE_CONTROL_GAP;
        float maxToggleLeft = Math.max(bodyLeft, resetLeft - chevronReserve - SettingsUiMetrics.SETTING_VALUE_CONTROL_GAP - toggleW);
        float toggleLeft = Mth.clamp(bodyLeft + valueColumnStartX, bodyLeft, maxToggleLeft);
        return new float[]{toggleLeft, titleOriginY, toggleW, toggleH};
    }

    private float[] chevronBounds() {
        float[] toggle = toggleBounds();
        float toggleH = toggle[3];
        float resetLeft = SettingRowResetLayout.trailingResetLeftX(x() + outerWidth);
        float box = SettingRowResetLayout.glyphBoxPx();
        float chevronLeft = resetLeft - SettingsUiMetrics.SETTING_VALUE_CONTROL_GAP - box;
        float chevronTop = SettingRowResetLayout.verticallyCenteredTop(toggle[1], toggleH);
        return new float[]{chevronLeft, chevronTop, box};
    }

    private float[] inlineResetSquare() {
        float[] toggle = toggleBounds();
        float toggleH = toggle[3];
        float resetLeft = SettingRowResetLayout.trailingResetLeftX(x() + outerWidth);
        float resetTop = SettingRowResetLayout.verticallyCenteredTop(toggle[1], toggleH);
        float box = SettingRowResetLayout.glyphBoxPx();
        return new float[]{resetLeft, resetTop, box};
    }

    private float titleRowTop(float innerHeight, float padY, float titleRowHeight) {
        float bodyTop = y() + padY;
        return bodyTop + (innerHeight - titleRowHeight) * 0.5f;
    }
}
