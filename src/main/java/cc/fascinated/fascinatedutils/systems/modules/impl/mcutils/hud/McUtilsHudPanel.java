package cc.fascinated.fascinatedutils.systems.modules.impl.mcutils.hud;

import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.MiniMessageHudPanel;
import cc.fascinated.fascinatedutils.systems.hud.anchor.HudAnchorContentAlignment;
import cc.fascinated.fascinatedutils.systems.modules.impl.mcutils.McUtilsModule;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class McUtilsHudPanel extends MiniMessageHudPanel {

    private static final long UPDATE_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(1L);
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.US);

    private final McUtilsModule module;

    public McUtilsHudPanel(McUtilsModule module) {
        super(module, "mcutils", HudHostModule.UTILITY_WIDGET_MIN_WIDTH);
        this.module = module;
    }

    @Override
    protected HudAnchorContentAlignment.Horizontal textLineHorizontalAlignmentOverrideForPanel() {
        return HudAnchorContentAlignment.Horizontal.LEFT;
    }

    @Override
    protected boolean defaultVisible() {
        return false;
    }

    @Override
    protected long miniMessageLineUpdateIntervalNanos() {
        return UPDATE_INTERVAL_NANOS;
    }

    @Override
    protected List<String> computeMiniMessageLines(float deltaSeconds, boolean editorMode) {
        return List.of(
                "Submitted <white>" + NUMBER_FORMAT.format(module.getSubmittedCount()) + "</white>",
                "Session <green>+" + NUMBER_FORMAT.format(module.getSessionDelta()) + "</green>"
        );
    }
}
