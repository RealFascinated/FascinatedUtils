package cc.fascinated.fascinatedutils.gui.modsettings;

import cc.fascinated.fascinatedutils.common.Colors;
import cc.fascinated.fascinatedutils.common.setting.impl.BooleanSetting;
import cc.fascinated.fascinatedutils.gui.core.TextLineLayout;
import cc.fascinated.fascinatedutils.gui.core.UiPointerCursor;
import cc.fascinated.fascinatedutils.gui.hooks.AnimHandle;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.ModSettingsTheme;
import cc.fascinated.fascinatedutils.gui.theme.SettingsUiMetrics;
import cc.fascinated.fascinatedutils.gui.widgets.FAnimatable;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import cc.fascinated.fascinatedutils.gui.widgets.settings.SettingRowResetLayout;
import cc.fascinated.fascinatedutils.gui.widgets.settings.WSettingTooltip;
import net.minecraft.util.Mth;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Compact boolean row used inside a two-column settings grid.
 *
 * <p>The toggle sits on the leading edge with the label immediately to its right, matching common mod-settings column
 * layouts. A trailing reset control keeps parity with full-width boolean rows.
 */
public class FBooleanSettingGridCellWidget extends FWidget implements FAnimatable {
    private static final Map<BooleanSetting, AnimHandle> TOGGLE_ANIM_CACHE = new IdentityHashMap<>();

    private final Runnable onPersist;
    private final BooleanSetting booleanSetting;
    private final float outerWidth;
    private final float outerHeight;
    private final AnimHandle toggleProgressAnim;

    public FBooleanSettingGridCellWidget(BooleanSetting booleanSetting, float outerWidth, float outerHeight, Runnable onPersist) {
        this.booleanSetting = booleanSetting;
        this.onPersist = onPersist;
        this.outerWidth = outerWidth;
        this.outerHeight = outerHeight;
        this.toggleProgressAnim = TOGGLE_ANIM_CACHE.computeIfAbsent(booleanSetting, setting -> new AnimHandle(setting.getValue() ? 1f : 0f).speed(26f));
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
    public UiPointerCursor pointerCursor(float pointerX, float pointerY) {
        if (booleanSetting.isLocked()) {
            return UiPointerCursor.DEFAULT;
        }
        float[] toggle = toggleBounds();
        boolean isHoveredToggle = SettingRowResetLayout.rectContains(toggle[0], toggle[1], toggle[2], toggle[3], pointerX, pointerY);
        boolean isHoveredReset = SettingRowResetLayout.resetGlyphHitActive(inlineResetSquare(), pointerX, pointerY, booleanSetting.isAtDefault());
        return isHoveredToggle || isHoveredReset ? UiPointerCursor.HAND : UiPointerCursor.DEFAULT;
    }

    @Override
    public boolean mouseDown(float pointerX, float pointerY, int button) {
        if (button != 0) {
            return false;
        }
        float[] toggle = toggleBounds();
        boolean isHoveredToggle = SettingRowResetLayout.rectContains(toggle[0], toggle[1], toggle[2], toggle[3], pointerX, pointerY);
        boolean isHoveredReset = SettingRowResetLayout.resetGlyphHitActive(inlineResetSquare(), pointerX, pointerY, booleanSetting.isAtDefault());
        if (booleanSetting.isLocked()) {
            return isHoveredToggle || isHoveredReset;
        }
        return isHoveredReset || isHoveredToggle;
    }

    @Override
    public boolean click(float pointerX, float pointerY, int button) {
        if (button != 0) {
            return false;
        }
        float[] toggle = toggleBounds();
        boolean isHoveredToggle = SettingRowResetLayout.rectContains(toggle[0], toggle[1], toggle[2], toggle[3], pointerX, pointerY);
        boolean isHoveredReset = SettingRowResetLayout.resetGlyphHitActive(inlineResetSquare(), pointerX, pointerY, booleanSetting.isAtDefault());
        if (booleanSetting.isLocked()) {
            return isHoveredToggle || isHoveredReset;
        }
        if (isHoveredReset) {
            booleanSetting.resetToDefault();
            toggleProgressAnim.target(booleanSetting.getValue() ? 1f : 0f);
            onPersist.run();
            return true;
        }
        if (isHoveredToggle) {
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
        float[] toggle = toggleBounds();
        boolean isHoveredToggle = SettingRowResetLayout.rectContains(toggle[0], toggle[1], toggle[2], toggle[3], mouseX, mouseY);
        boolean isHoveredReset = SettingRowResetLayout.resetGlyphHitActive(inlineResetSquare(), mouseX, mouseY, booleanSetting.isAtDefault());
        if (isHoveredToggle || isHoveredReset) {
            WSettingTooltip.drawTooltipForSetting(graphics, mouseX, mouseY, booleanSetting, isHoveredReset);
        }
    }

    @Override
    protected void renderSelf(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
        boolean locked = booleanSetting.isLocked();
        float innerHeight = Math.max(0f, h() - 2f * SettingsUiMetrics.SETTING_ROW_PADDING_Y);
        float padY = SettingsUiMetrics.SETTING_ROW_PADDING_Y;
        float[] toggle = toggleBounds();
        float toggleW = toggle[2];
        float toggleH = toggle[3];
        float titleRowHeight = Math.max(ModSettingsTheme.shellDesignBodyLineHeight(), toggleH);
        float titleOriginY = titleRowTop(innerHeight, padY, titleRowHeight);
        float labelGap = SettingsUiMetrics.SETTING_VALUE_CONTROL_GAP;
        float labelX = toggle[0] + toggleW + labelGap;
        float resetLeft = SettingRowResetLayout.trailingResetLeftX(x() + w());
        float labelMaxWidth = Math.max(0f, resetLeft - labelGap - labelX);
        String label = booleanSetting.getName();
        label = TextLineLayout.ellipsize(label, labelMaxWidth, segment -> graphics.measureTextWidth(segment, false));
        float labelY = titleOriginY + Math.max(0f, (toggleH - graphics.getFontCapHeight()) * 0.5f);
        int labelColor = locked ? graphics.theme().textMuted() : graphics.theme().textPrimary();
        graphics.drawMiniMessageText("<color:" + Colors.rgbHex(labelColor) + ">" + label + "</color>", labelX, labelY, false);
        float progress = Mth.clamp(toggleProgressAnim.value(), 0f, 1f);
        boolean isHoveredToggle = SettingRowResetLayout.rectContains(toggle[0], toggle[1], toggle[2], toggle[3], mouseX, mouseY);
        float[] resetSquare = inlineResetSquare();
        boolean isHoveredReset = SettingRowResetLayout.resetGlyphHitActive(resetSquare, mouseX, mouseY, booleanSetting.isAtDefault());
        int trackFillOff = isHoveredToggle && !locked ? graphics.theme().toggleOffFillHover() : graphics.theme().toggleOffFill();
        int trackFillOn = isHoveredToggle && !locked ? graphics.theme().toggleOnFillHover() : graphics.theme().toggleOnFill();
        int trackFill = Colors.mixArgb(progress, trackFillOff, trackFillOn);
        int trackBorder = Colors.mixArgb(progress, isHoveredToggle && !locked ? graphics.theme().toggleOffBorderHover() : graphics.theme().toggleOffBorder(), graphics.theme().toggleOnBorder());
        if (locked) {
            trackFill = WSettingTooltip.dimColor(trackFill, 0.45f);
            trackBorder = WSettingTooltip.dimColor(trackBorder, 0.6f);
        }
        float borderThickness = 1f;
        float maxTrackCornerRadius = Math.max(0.5f, Math.min(toggleH * 0.5f - borderThickness * 0.5f, toggleW * 0.5f - borderThickness * 0.5f));
        graphics.fillRoundedRectFrame(toggle[0], titleOriginY, toggleW, toggleH, maxTrackCornerRadius, trackBorder, trackFill, borderThickness, borderThickness, RectCornerRoundMask.ALL);
        float knobSize = toggleH - 4f;
        float knobTravelLeft = toggle[0] + 2f;
        float knobTravelRight = toggle[0] + toggleW - knobSize - 2f;
        float knobX = Mth.lerp(progress, knobTravelLeft, knobTravelRight);
        float knobY = titleOriginY + (toggleH - knobSize) * 0.5f;
        float knobCornerRadius = Math.max(0.5f, knobSize * 0.5f - 0.01f);
        int thumbColor = locked ? WSettingTooltip.dimColor(graphics.theme().thumb(), 0.5f) : graphics.theme().thumb();
        graphics.fillRoundedRect(knobX, knobY, knobSize, knobSize, knobCornerRadius, thumbColor, RectCornerRoundMask.ALL);

        SettingRowResetLayout.paintGlyph(graphics, resetSquare[0], resetSquare[1], toggleH, isHoveredReset && !locked, booleanSetting.isAtDefault());
    }

    private float[] toggleBounds() {
        float innerHeight = Math.max(0f, h() - 2f * SettingsUiMetrics.SETTING_ROW_PADDING_Y);
        float padY = SettingsUiMetrics.SETTING_ROW_PADDING_Y;
        float padX = 0f;
        float toggleW = SettingsUiMetrics.BOOLEAN_TOGGLE_OUTER_W;
        float toggleH = SettingsUiMetrics.BOOLEAN_TOGGLE_OUTER_H;
        float titleRowHeight = Math.max(ModSettingsTheme.shellDesignBodyLineHeight(), toggleH);
        float titleOriginY = titleRowTop(innerHeight, padY, titleRowHeight);
        float toggleLeft = x() + padX;
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
