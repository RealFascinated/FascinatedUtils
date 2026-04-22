package cc.fascinated.fascinatedutils.gui.modsettings;

import cc.fascinated.fascinatedutils.common.Colors;
import cc.fascinated.fascinatedutils.gui.core.Callback;
import cc.fascinated.fascinatedutils.gui.core.TextLayoutMetrics;
import cc.fascinated.fascinatedutils.gui.core.TextLineLayout;
import cc.fascinated.fascinatedutils.gui.core.UiPointerCursor;
import cc.fascinated.fascinatedutils.gui.hooks.AnimHandle;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.ModSettingsTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FAnimatable;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.Supplier;

public class FVisibilityCardWidget<T> extends FWidget implements FAnimatable {
    private static final float TITLE_PAD_TOP_DESIGN = 16f;
    private static final float TITLE_PAD_BOTTOM_DESIGN = 16f;
    private static final float ACTION_STRIP_HEIGHT_DESIGN = 20f;
    private static final int TITLE_MAX_LINES = 2;
    private static final int ACTION_STRIP_COUNT = 2;
    public static float stackedCellOuterHeightPx() {
        float titleTopPad = TITLE_PAD_TOP_DESIGN;
        float titleBottomPad = TITLE_PAD_BOTTOM_DESIGN;
        float buttonBand = ACTION_STRIP_HEIGHT_DESIGN;
        float lineHeight = Math.max(1f, ModSettingsTheme.shellDesignBodyLineHeight());
        float titleBlock = TITLE_MAX_LINES * lineHeight;
        return titleTopPad + titleBlock + titleBottomPad + ACTION_STRIP_COUNT * buttonBand;
    }
    private static boolean pointerInStrip(float[] strip, float pointerX, float pointerY) {
        return pointerX >= strip[0] && pointerY >= strip[1] && pointerX < strip[0] + strip[2] && pointerY < strip[1] + strip[3];
    }
    private final float cardWidthLogical;
    private final float cardHeightLogical;
    private final Supplier<String> titleSupplier;
    private final Supplier<Boolean> settingsAvailableSupplier;
    private final Runnable onOpenSettings;
    private final Supplier<Boolean> enabledSupplier;
    private final Callback<Boolean> onEnabledChange;
    private final AnimHandle toggleStripProgressAnim = new AnimHandle(0f).speed(26f);

    private boolean hoverSettingsStrip;

    private boolean hoverToggleStrip;

    public FVisibilityCardWidget(float layoutWidth, float layoutHeight, Supplier<String> titleSupplier, Supplier<Boolean> settingsAvailableSupplier, Runnable onOpenSettings, Supplier<Boolean> enabledSupplier, Callback<Boolean> onEnabledChange) {
        this.cardWidthLogical = Math.max(0f, layoutWidth);
        this.cardHeightLogical = Math.max(0f, layoutHeight);
        this.titleSupplier = titleSupplier;
        this.settingsAvailableSupplier = settingsAvailableSupplier;
        this.onOpenSettings = onOpenSettings;
        this.enabledSupplier = enabledSupplier;
        this.onEnabledChange = onEnabledChange;
        float initialProgress = enabledSupplier.get() ? 1f : 0f;
        this.toggleStripProgressAnim.snap(initialProgress);
    }

    @Override
    public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
        setBounds(layoutX, layoutY, layoutWidth, layoutHeight);
    }

    @Override
    public float intrinsicWidthForRow(UIRenderer measure, float heightBudget) {
        return cardWidthLogical > 0f ? cardWidthLogical : w();
    }

    @Override
    public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
        return cardHeightLogical > 0f ? cardHeightLogical : stackedCellOuterHeightPx();
    }

    @Override
    public boolean wantsPointer() {
        return true;
    }

    @Override
    public UiPointerCursor pointerCursor(float pointerX, float pointerY) {
        if (hitToggleStrip(pointerX, pointerY)) {
            return UiPointerCursor.HAND;
        }
        if (hitSettingsStrip(pointerX, pointerY)) {
            if (!hasSettings()) {
                return UiPointerCursor.NOT_ALLOWED;
            }
            return UiPointerCursor.HAND;
        }
        return UiPointerCursor.DEFAULT;
    }

    @Override
    public void tickAnims(float deltaSeconds) {
        toggleStripProgressAnim.target(enabledSupplier.get() ? 1f : 0f);
        toggleStripProgressAnim.tick(deltaSeconds);
    }

    @Override
    public boolean mouseMove(float pointerX, float pointerY) {
        boolean hasSettings = hasSettings();
        hoverSettingsStrip = hasSettings && hitSettingsStrip(pointerX, pointerY);
        hoverToggleStrip = hitToggleStrip(pointerX, pointerY);
        return false;
    }

    @Override
    public boolean mouseLeave(float pointerX, float pointerY) {
        hoverSettingsStrip = false;
        hoverToggleStrip = false;
        return false;
    }

    @Override
    public boolean click(float pointerX, float pointerY, int button) {
        if (button != 0) {
            return false;
        }
        boolean hasSettings = hasSettings();
        if (hasSettings && hitSettingsStrip(pointerX, pointerY)) {
            onOpenSettings.run();
            return true;
        }
        if (hitToggleStrip(pointerX, pointerY)) {
            onEnabledChange.invoke(!enabledSupplier.get());
            return true;
        }
        return false;
    }

    @Override
    protected void renderSelf(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
        float corner = graphics.theme().cardCornerRadius();
        graphics.fillThemedSurfaceCardFrame(x(), y(), w(), h(), corner, RectCornerRoundMask.ALL);
        float titleTopPad = TITLE_PAD_TOP_DESIGN;
        float titleBottomPad = TITLE_PAD_BOTTOM_DESIGN;
        float[] settingsStrip = settingsStripBounds();
        float[] toggleStrip = toggleStripBounds();
        float titleRegionBottom = settingsStrip[1] - titleBottomPad;
        float titleTop = y() + titleTopPad;
        float titleRegionHeight = Math.max(0f, titleRegionBottom - titleTop);
        String title = titleSupplier.get();
        float innerWidth = Math.max(0f, w() - 2f * titleTopPad);
        float wrapBudget = innerWidth;
        java.util.List<String> lines = TextLineLayout.wrapLines(title, wrapBudget, segment -> graphics.measureTextWidth(segment, false));
        float lineHeight = TextLayoutMetrics.layoutLineHeightPx(graphics);
        int lineCount = Math.min(TITLE_MAX_LINES, lines.size());
        float textBlockHeight = lineCount * lineHeight;
        float innerLeft = x() + titleTopPad;
        float cursorY = titleTop + Math.max(0f, (titleRegionHeight - textBlockHeight) * 0.5f);
        for (int lineIndex = 0; lineIndex < lineCount; lineIndex++) {
            String line = lines.get(lineIndex);
            int lineTextWidth = graphics.measureTextWidth(line, false);
            float lineX = innerLeft + (innerWidth - lineTextWidth) * 0.5f;
            graphics.drawMiniMessageText("<color:" + Colors.rgbHex(graphics.theme().textPrimary()) + ">" + line + "</color>", lineX, cursorY, false);
            cursorY += lineHeight;
        }
        float stripBorderThickness = 1f;

        float settingsStripX = settingsStrip[0];
        float settingsStripY = settingsStrip[1];
        float settingsStripW = settingsStrip[2];
        float settingsStripH = settingsStrip[3];
        boolean hasSettings = hasSettings();
        int settingsFill = hasSettings ? hoverSettingsStrip ? graphics.theme().moduleListRowHover() : graphics.theme().borderMuted() : hoverSettingsStrip ? graphics.theme().widgetStateInactiveFillHover() : graphics.theme().widgetStateInactiveFill();
        float settingsCornerMax = Math.max(0.5f, Math.min(settingsStripW, settingsStripH) * 0.5f - stripBorderThickness * 0.5f - 0.01f);
        float settingsCorner = Mth.clamp(corner, 0.5f, settingsCornerMax);
        graphics.fillRoundedRectFrame(settingsStripX, settingsStripY, settingsStripW, settingsStripH, settingsCorner, graphics.theme().border(), settingsFill, stripBorderThickness, stripBorderThickness, RectCornerRoundMask.NONE);
        String settingsLabel = hasSettings ? Component.translatable("fascinatedutils.setting.shell.widget_settings.open").getString() : Component.translatable("fascinatedutils.setting.shell.widget_settings.unavailable").getString();
        int settingsLabelColor = hasSettings ? graphics.theme().widgetStateLabel() : graphics.theme().widgetStateLabelMuted();
        float settingsTextY = settingsStripY + (settingsStripH - graphics.getFontCapHeight()) * 0.5f;
        int settingsLabelWidth = graphics.measureTextWidth(settingsLabel, false);
        graphics.drawMiniMessageText("<color:" + Colors.rgbHex(settingsLabelColor) + ">" + settingsLabel + "</color>", settingsStripX + (settingsStripW - settingsLabelWidth) * 0.5f, settingsTextY, false);

        float stripX = toggleStrip[0];
        float stripY = toggleStrip[1];
        float stripW = toggleStrip[2];
        float stripH = toggleStrip[3];
        float stripProgress = Mth.clamp(toggleStripProgressAnim.value(), 0f, 1f);
        int fillDisabled = hoverToggleStrip ? graphics.theme().widgetStateDisabledFillHover() : graphics.theme().widgetStateDisabledFill();
        int fillEnabled = hoverToggleStrip ? graphics.theme().widgetStateEnabledFillHover() : graphics.theme().widgetStateEnabledFill();
        int fillArgb = Colors.mixArgb(stripProgress, fillDisabled, fillEnabled);
        int borderArgb = Colors.mixArgb(stripProgress, graphics.theme().widgetStateDisabledBorder(), graphics.theme().widgetStateEnabledBorder());
        String actionLabel = enabledSupplier.get() ? Component.translatable("fascinatedutils.setting.shell.widget_state.enabled").getString() : Component.translatable("fascinatedutils.setting.shell.widget_state.disabled").getString();
        float stripCornerMax = Math.max(0.5f, Math.min(stripW, stripH) * 0.5f - stripBorderThickness * 0.5f - 0.01f);
        float stripCorner = Mth.clamp(corner, 0.5f, stripCornerMax);
        graphics.fillRoundedRectFrame(stripX, stripY, stripW, stripH, stripCorner, borderArgb, fillArgb, stripBorderThickness, stripBorderThickness, RectCornerRoundMask.BOTTOM);
        float textY = stripY + (stripH - graphics.getFontCapHeight()) * 0.5f;
        int labelWidth = graphics.measureTextWidth(actionLabel, false);
        graphics.drawMiniMessageText("<color:" + Colors.rgbHex(graphics.theme().widgetStateLabel()) + ">" + actionLabel + "</color>", stripX + (stripW - labelWidth) * 0.5f, textY, false);
    }

    private boolean hasSettings() {
        return settingsAvailableSupplier.get();
    }

    private boolean hitSettingsStrip(float pointerX, float pointerY) {
        float[] strip = settingsStripBounds();
        return pointerInStrip(strip, pointerX, pointerY);
    }

    private boolean hitToggleStrip(float pointerX, float pointerY) {
        float[] strip = toggleStripBounds();
        return pointerInStrip(strip, pointerX, pointerY);
    }

    private float actionHeightPx() {
        return ACTION_STRIP_HEIGHT_DESIGN;
    }

    private float[] settingsStripBounds() {
        float actionHeight = actionHeightPx();
        float stripY = y() + h() - ACTION_STRIP_COUNT * actionHeight;
        return new float[]{x(), stripY, w(), actionHeight};
    }

    private float[] toggleStripBounds() {
        float actionHeight = actionHeightPx();
        float stripY = y() + h() - actionHeight;
        return new float[]{x(), stripY, w(), actionHeight};
    }
}
