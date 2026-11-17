package cc.fascinated.fascinatedutils.systems.modules.impl.mcutils.hud;

import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.panel.MiniMessageHudPanel;
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
        super(module, "mcutils", HudHostModule.UTILITY_WIDGET_MIN_WIDTH, UPDATE_INTERVAL_NANOS);
        this.module = module;
    }

    @Override
    protected HudAnchorContentAlignment.Horizontal textLineHorizontalAlignmentOverrideForPanel() {
        return HudAnchorContentAlignment.Horizontal.LEFT;
    }

    @Override
    protected boolean isPanelVisible() {
        return module.getShowHud().isEnabled();
    }

    @Override
    public void hideFromHud() {
        // The module keeps submitting names while only its panel is hidden.
        module.getShowHud().setValue(false);
    }

    @Override
    protected List<String> computeMiniMessageLines(float deltaSeconds, boolean editorMode) {
        return List.of(
                hudText("submitted") + " <white>" + (module.getSubmittedCount() < 0 ? "…" : NUMBER_FORMAT.format(module.getSubmittedCount())) + "</white>",
                hudText("session") + " <green>+" + NUMBER_FORMAT.format(module.getSessionDelta()) + "</green>"
        );
    }
}
