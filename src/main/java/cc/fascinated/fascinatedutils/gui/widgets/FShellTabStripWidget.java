package cc.fascinated.fascinatedutils.gui.widgets;

import cc.fascinated.fascinatedutils.gui.core.Callback;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.ModSettingsTheme;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class FShellTabStripWidget extends FWidget {
    public static final String TAB_KEY_MODULES = "modules";
    public static final String TAB_KEY_PROFILES = "profiles";
    public static final String TAB_KEY_SETTINGS = "settings";

    private static final int TAB_COUNT = 2;

    private final FClickableTabSegmentWidget modulesSegment;
    private final FClickableTabSegmentWidget settingsSegment;

    public FShellTabStripWidget(Callback<String> onSelect) {
        modulesSegment = new FClickableTabSegmentWidget(TAB_KEY_MODULES, Component.translatable("fascinatedutils.setting.shell.tab_modules").getString(), onSelect);
        settingsSegment = new FClickableTabSegmentWidget(TAB_KEY_SETTINGS, Component.translatable("fascinatedutils.setting.shell.tab_settings").getString(), onSelect);
        addChild(modulesSegment);
        addChild(settingsSegment);
    }

    public void setSelectedKey(String selectedKey) {
        String key = selectedKey == null ? TAB_KEY_MODULES : selectedKey;
        modulesSegment.setSelectedKey(key);
        settingsSegment.setSelectedKey(key);
    }

    @Override
    public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
        setBounds(layoutX, layoutY, layoutWidth, layoutHeight);
        float marginX = ModSettingsTheme.SHELL_TAB_STRIP_MARGIN_X;
        float maxTrackWidth = ModSettingsTheme.SHELL_TAB_STRIP_MAX_WIDTH;
        float trackWidth = Math.min(maxTrackWidth, Math.max(0f, layoutWidth - 2f * marginX));
        float trackHeight = ModSettingsTheme.titleBarSquareControlSizePx();
        float trackX = layoutX + marginX;
        float trackY = layoutY + Math.max(0f, (layoutHeight - trackHeight) * 0.5f);
        float segmentGap = ModSettingsTheme.SHELL_TAB_STRIP_SEGMENT_GAP;
        float segmentWidth = Math.max(0f, (trackWidth - segmentGap * (TAB_COUNT - 1)) / Math.max(1, TAB_COUNT));
        float segmentHeight = trackHeight;
        float segmentY = trackY;
        float modulesSegmentX = trackX;
        float settingsSegmentX = modulesSegmentX + segmentWidth + segmentGap;
        float trackCornerRadius = Math.max(0.5f, Math.min(ModSettingsTheme.SHELL_TAB_STRIP_SEGMENT_CORNER_RADIUS, trackHeight * 0.5f - 0.5f));
        float segmentCornerMax = Math.max(0.5f, segmentWidth * 0.5f - 0.5f);
        float segmentCornerRadius = Mth.clamp(Math.min(trackCornerRadius - 0.5f, Math.min(segmentHeight * 0.5f - 0.5f, segmentCornerMax)), 0.5f, segmentCornerMax);
        modulesSegment.setShellSegmentFillet(segmentCornerRadius, RectCornerRoundMask.ALL);
        settingsSegment.setShellSegmentFillet(segmentCornerRadius, RectCornerRoundMask.ALL);
        modulesSegment.layout(measure, modulesSegmentX, segmentY, segmentWidth, segmentHeight);
        settingsSegment.layout(measure, settingsSegmentX, segmentY, segmentWidth, segmentHeight);
    }

    @Override
    public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
        return ModSettingsTheme.TOPBAR_HEIGHT;
    }
}
