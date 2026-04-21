package cc.fascinated.fascinatedutils.gui.widgets;

import cc.fascinated.fascinatedutils.gui.core.Callback;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.theme.ModSettingsTheme;
import net.minecraft.network.chat.Component;

public class FShellTabStripWidget extends FWidget {
    public static final String TAB_KEY_MODULES = "modules";
    public static final String TAB_KEY_PROFILES = "profiles";
    public static final String TAB_KEY_SETTINGS = "settings";

    private final FClickableTabSegmentWidget modulesSegment;
    private final FClickableTabSegmentWidget settingsSegment;
    private float trackX;
    private float trackY;
    private float trackWidth;
    private float trackHeight;

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
        float segmentGap = ModSettingsTheme.SHELL_TAB_STRIP_SEGMENT_GAP;
        float segmentPadX = 10f;
        trackHeight = ModSettingsTheme.titleBarSquareControlSizePx();

        // Size each segment to fit its label text
        float modulesTextW = measure.measureTextWidth(modulesSegment.getLabelText(), false);
        float settingsTextW = measure.measureTextWidth(settingsSegment.getLabelText(), false);
        float modulesSegW = modulesTextW + 2f * segmentPadX;
        float settingsSegW = settingsTextW + 2f * segmentPadX;
        trackWidth = modulesSegW + segmentGap + settingsSegW;

        // Center the compact track within the full available width
        trackX = layoutX + (layoutWidth - trackWidth) * 0.5f;
        trackY = layoutY + Math.max(0f, (layoutHeight - trackHeight) * 0.5f);

        float segmentHeight = trackHeight;
        float modulesSegmentX = trackX;
        float settingsSegmentX = modulesSegmentX + modulesSegW + segmentGap;

        float cornerRadius = Math.max(0.5f, Math.min(ModSettingsTheme.SHELL_TAB_STRIP_SEGMENT_CORNER_RADIUS, trackHeight * 0.5f - 0.5f));
        modulesSegment.setShellSegmentFillet(cornerRadius, RectCornerRoundMask.ALL);
        settingsSegment.setShellSegmentFillet(cornerRadius, RectCornerRoundMask.ALL);
        modulesSegment.layout(measure, modulesSegmentX, trackY, modulesSegW, segmentHeight);
        settingsSegment.layout(measure, settingsSegmentX, trackY, settingsSegW, segmentHeight);
    }

    @Override
    protected void renderSelf(GuiRenderer graphics, float mouseX, float mouseY, float deltaSeconds) {
        if (trackWidth <= 0f || trackHeight <= 0f) {
            return;
        }
        float cornerRadius = Math.max(0.5f, Math.min(ModSettingsTheme.SHELL_TAB_STRIP_SEGMENT_CORNER_RADIUS, trackHeight * 0.5f - 0.01f));
        graphics.fillRoundedRect(trackX, trackY, trackWidth, trackHeight, cornerRadius, graphics.theme().surface(), RectCornerRoundMask.ALL);
    }

    @Override
    public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
        return ModSettingsTheme.TOPBAR_HEIGHT;
    }
}
