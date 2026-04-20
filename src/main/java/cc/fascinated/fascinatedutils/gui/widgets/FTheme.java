package cc.fascinated.fascinatedutils.gui.widgets;

import cc.fascinated.fascinatedutils.gui.core.Callback;
import cc.fascinated.fascinatedutils.gui.modsettings.FHudWidgetVisibilityCardWidget;
import cc.fascinated.fascinatedutils.gui.modsettings.FModuleVisibilityCardWidget;
import cc.fascinated.fascinatedutils.gui.theme.ModSettingsTheme;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.systems.hud.HudModule;
import cc.fascinated.fascinatedutils.systems.modules.Module;

public class FTheme {
    public interface Components {
        FModuleVisibilityCardWidget createModuleVisibilityCard(Module module, float layoutWidth, float layoutHeight, Callback<Module> onOpenSettings, Callback<Boolean> onEnabledChange);

        FHudWidgetVisibilityCardWidget createHudWidgetVisibilityCard(HudModule widget, float layoutWidth, float layoutHeight, Callback<Boolean> onVisibilityChange, Callback<HudModule> onOpenSettings);

        FScrollColumnWidget createScrollColumn(FWidget bodyColumn, float rowGap);
    }

    private static class DefaultComponents implements Components {
        @Override
        public FModuleVisibilityCardWidget createModuleVisibilityCard(Module module, float layoutWidth, float layoutHeight, Callback<Module> onOpenSettings, Callback<Boolean> onEnabledChange) {
            return new FModuleVisibilityCardWidget(module, layoutWidth, layoutHeight, onOpenSettings, onEnabledChange);
        }

        @Override
        public FHudWidgetVisibilityCardWidget createHudWidgetVisibilityCard(HudModule widget, float layoutWidth, float layoutHeight, Callback<Boolean> onVisibilityChange, Callback<HudModule> onOpenSettings) {
            return new FHudWidgetVisibilityCardWidget(widget, layoutWidth, layoutHeight, onVisibilityChange, onOpenSettings);
        }

        @Override
        public FScrollColumnWidget createScrollColumn(FWidget bodyColumn, float rowGap) {
            return new FScrollColumnWidget(bodyColumn, rowGap);
        }
    }

    private static Components components = new DefaultComponents();

    public static int textPrimary() {
        return FascinatedGuiTheme.INSTANCE.textPrimary();
    }

    public static int textMuted() {
        return FascinatedGuiTheme.INSTANCE.textMuted();
    }

    public static float gapSm() {
        return UITheme.GAP_SM;
    }

    public static float gapMd() {
        return UITheme.GAP_MD;
    }

    public static float paddingSm() {
        return UITheme.PADDING_SM;
    }

    public static float paddingXs() {
        return UITheme.PADDING_XS;
    }

    public static float scrollWheelScale() {
        return UITheme.SCROLL_WHEEL_SCALE;
    }

    public static float scrollClipHorizontalOutsetLogical() {
        return UITheme.SCROLL_CLIP_HORIZONTAL_OUTSET_LOGICAL;
    }

    public static int scrollbarThumb() {
        return UITheme.COLOR_SCROLLBAR_THUMB;
    }

    public static float vanillaLineHeightDesign() {
        return ModSettingsTheme.shellDesignBodyLineHeight();
    }

    public static Components components() {
        return components;
    }

    public static void setComponents(Components components) {
        FTheme.components = components == null ? new DefaultComponents() : components;
    }

    private FTheme() {
    }
}
