package cc.fascinated.fascinatedutils.gui.widgets.settings;

import cc.fascinated.fascinatedutils.common.ColorUtils;
import cc.fascinated.fascinatedutils.common.setting.impl.KeybindSetting;
import cc.fascinated.fascinatedutils.gui.core.UiPointerCursor;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.theme.ModSettingsTheme;
import cc.fascinated.fascinatedutils.gui.theme.SettingsUiMetrics;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

public class FKeybindSettingWidget extends FSettingRowWidget {
    private static final float KEY_CHIP_WIDTH_DESIGN = 84f;
    private static final float KEY_CHIP_HEIGHT_DESIGN = 13f;
    private final KeybindSetting keybindSetting;
    private boolean hovered;
    private boolean hoveredReset;
    private boolean listening;

    public FKeybindSettingWidget(KeybindSetting keybindSetting, float outerWidth, float outerHeight, Runnable onPersist) {
        super(outerWidth, outerHeight, onPersist == null ? () -> {} : onPersist);
        this.keybindSetting = keybindSetting;
    }

    @Override
    public int focusId() {
        return keybindSetting.getSettingKey().hashCode();
    }

    @Override
    public UiPointerCursor pointerCursor(float pointerX, float pointerY) {
        boolean locked = keybindSetting.isLocked();
        float[] resetSquare = inlineResetSquare();
        if (SettingRowResetLayout.resetGlyphHitActive(resetSquare[0], resetSquare[1], resetSquare[2], pointerX, pointerY, keybindSetting.isAtDefault())) {
            return locked ? UiPointerCursor.DEFAULT : UiPointerCursor.HAND;
        }
        return rectContains(chipBounds(), pointerX, pointerY) && !locked ? UiPointerCursor.HAND : UiPointerCursor.DEFAULT;
    }

    @Override
    public boolean mouseLeave(float pointerX, float pointerY) {
        hovered = false;
        hoveredReset = false;
        return false;
    }

    @Override
    public boolean mouseMove(float pointerX, float pointerY) {
        hovered = rectContains(chipBounds(), pointerX, pointerY);
        float[] resetSquare = inlineResetSquare();
        hoveredReset = SettingRowResetLayout.resetGlyphHitActive(resetSquare[0], resetSquare[1], resetSquare[2], pointerX, pointerY, keybindSetting.isAtDefault());
        return false;
    }

    @Override
    public boolean mouseDown(float pointerX, float pointerY, int button) {
        if (button != 0) {
            return false;
        }
        if (keybindSetting.isLocked()) {
            return hovered || hoveredReset;
        }
        float[] resetSquare = inlineResetSquare();
        if (SettingRowResetLayout.resetGlyphHitActive(resetSquare[0], resetSquare[1], resetSquare[2], pointerX, pointerY, keybindSetting.isAtDefault())) {
            return true;
        }
        return rectContains(chipBounds(), pointerX, pointerY);
    }

    @Override
    public boolean click(float pointerX, float pointerY, int button) {
        if (button != 0) {
            return false;
        }
        if (keybindSetting.isLocked()) {
            listening = false;
            return hovered || hoveredReset;
        }
        float[] resetSquare = inlineResetSquare();
        if (SettingRowResetLayout.resetGlyphHitActive(resetSquare[0], resetSquare[1], resetSquare[2], pointerX, pointerY, keybindSetting.isAtDefault())) {
            keybindSetting.resetToDefault();
            onPersist.run();
            listening = false;
            return true;
        }
        if (rectContains(chipBounds(), pointerX, pointerY)) {
            listening = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean keyDown(int keyCode, int modifiers) {
        if (!listening || keybindSetting.isLocked()) {
            return false;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            listening = false;
            return true;
        }
        InputConstants.Key nextKey = InputConstants.getKey(new KeyEvent(keyCode, 0, modifiers));
        keybindSetting.applyBinding(nextKey);
        onPersist.run();
        listening = false;
        return true;
    }

    @Override
    public void renderOverlayAfterChildren(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
        if (hovered || hoveredReset) {
            WSettingTooltip.drawTooltipForSetting(graphics, mouseX, mouseY, keybindSetting, hoveredReset);
        }
    }

    @Override
    protected void renderSelf(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
        boolean locked = keybindSetting.isLocked();
        float bodyPadX = SettingsUiMetrics.SETTING_ROW_PADDING_X;
        float bodyLeft = x() + bodyPadX;
        float titleOriginY = titleRowOriginY();
        String label = keybindSetting.getTranslatedDisplayName();
        float titleRowHeight = titleRowHeightPx();
        float labelY = titleOriginY + Math.max(0f, (titleRowHeight - graphics.getFontHeight()) * 0.5f);
        graphics.drawMiniMessageText("<color:" + ColorUtils.rgbHex(locked ? graphics.theme().textMuted() : graphics.theme().textPrimary()) + ">" + label + "</color>", bodyLeft, labelY, false);

        float[] chip = chipBounds();
        float chipLeft = chip[0];
        float chipTop = chip[1];
        float chipWidth = chip[2];
        float chipHeight = chip[3];
        float borderThickness = 1f;
        float corner = Mth.clamp(3f, 0.5f, Math.min(chipWidth, chipHeight) * 0.5f - borderThickness * 0.5f - 0.01f);
        int fillColor = listening && !locked ? graphics.theme().surfaceElevated() : hovered && !locked ? graphics.theme().moduleListRowHover() : graphics.theme().surface();
        int borderColor = listening && !locked ? graphics.theme().accentBright() : graphics.theme().border();
        if (locked) {
            fillColor = WSettingTooltip.dimColor(fillColor, 0.5f);
            borderColor = WSettingTooltip.dimColor(borderColor, 0.6f);
        }
        graphics.fillRoundedRectFrame(chipLeft, chipTop, chipWidth, chipHeight, corner, borderColor, fillColor, borderThickness, borderThickness, RectCornerRoundMask.ALL);
        String bindingLabel = listening && !locked ? "..." : keybindSetting.currentBindingLabel();
        bindingLabel = cc.fascinated.fascinatedutils.gui.core.TextLineLayout.ellipsize(bindingLabel, chipWidth - 4f, segment -> graphics.measureTextWidth(segment, false));
        int textWidth = graphics.measureTextWidth(bindingLabel, false);
        float textX = chipLeft + Math.max(0f, (chipWidth - textWidth) * 0.5f);
        float textY = chipTop + Math.max(0f, (chipHeight - graphics.getFontHeight()) * 0.5f);
        graphics.drawMiniMessageText("<color:" + ColorUtils.rgbHex(locked ? graphics.theme().textMuted() : graphics.theme().textPrimary()) + ">" + bindingLabel + "</color>", textX, textY, false);
        float[] resetSquare = inlineResetSquare();
        SettingRowResetLayout.paintGlyph(graphics, resetSquare[0], resetSquare[1], titleRowHeight, hoveredReset && !locked, keybindSetting.isAtDefault());
    }

    private float[] chipBounds() {
        float chipWidth = KEY_CHIP_WIDTH_DESIGN;
        float chipHeight = KEY_CHIP_HEIGHT_DESIGN;
        float chipTop = titleRowOriginY();
        float chipLeft = x() + outerWidth - chipWidth - SettingRowResetLayout.trailingResetReservePx();
        return new float[]{chipLeft, chipTop, chipWidth, chipHeight};
    }

    private float innerBodyHeightPx() {
        return Math.max(0f, h() - 2f * SettingsUiMetrics.SETTING_ROW_PADDING_Y);
    }

    private float bodyPadYPx() {
        return SettingsUiMetrics.SETTING_ROW_PADDING_Y;
    }

    private float titleRowHeightPx() {
        float chipH = KEY_CHIP_HEIGHT_DESIGN;
        return Math.max(ModSettingsTheme.shellDesignBodyLineHeight(), chipH);
    }

    private float titleRowOriginY() {
        float innerHeight = innerBodyHeightPx();
        float padY = bodyPadYPx();
        float titleRowHeight = titleRowHeightPx();
        float bodyTop = y() + padY;
        return bodyTop + Math.max(0f, (innerHeight - titleRowHeight) * 0.5f);
    }

    private float[] inlineResetSquare() {
        float contentRight = x() + outerWidth;
        float rowTop = titleRowOriginY();
        float rowHeight = titleRowHeightPx();
        float box = SettingRowResetLayout.glyphBoxPx();
        float resetLeft = SettingRowResetLayout.trailingResetLeftX(contentRight);
        float resetTop = SettingRowResetLayout.verticallyCenteredTop(rowTop, rowHeight);
        return new float[]{resetLeft, resetTop, box};
    }
}
