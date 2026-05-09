package cc.fascinated.fascinatedutils.oldgui.modsettings;

import cc.fascinated.fascinatedutils.oldgui.theme.ModSettingsTheme;
import cc.fascinated.fascinatedutils.oldgui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.oldgui.widgets.FButtonWidget;
import net.minecraft.network.chat.Component;

public class FHudLayoutEditorChipWidget extends FButtonWidget {
    public static final float HORIZONTAL_TEXT_PAD_DESIGN = 6f;

    public FHudLayoutEditorChipWidget(Runnable onActivate, float layoutWidth) {
        super(onActivate, () -> Component.translatable("alumite.setting.shell.edit_hud_layout").getString(), layoutWidth, 1, 1f, 6f, 1f, HORIZONTAL_TEXT_PAD_DESIGN);
    }

    public static float chipHeightPx() {
        return ModSettingsTheme.titleBarSquareControlSizePx();
    }

    @Override
    protected int resolveButtonFillColorArgb(boolean hovered) {
        return hovered ? FascinatedGuiTheme.INSTANCE.moduleListRowHover() : FascinatedGuiTheme.INSTANCE.surface();
    }
}
