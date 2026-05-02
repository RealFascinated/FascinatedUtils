package cc.fascinated.fascinatedutils.systems.hud;

import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import org.jspecify.annotations.Nullable;

public interface HudRenderablePanel {
    String getId();

    String getName();

    float getMinWidth();

    ModuleHudState getHudState();

    /**
     * The HUD-hosting module owning settings/profile state for this panel.
     *
     * @return the host module
     */
    HudHostModule hudHostModule();

    /**
     * Navigation target opened from the HUD editor ({@link cc.fascinated.fascinatedutils.gui.screens.ModSettingsScreen}).
     *
     * @return the concrete {@link Module} row to drill into — usually {@link #hudHostModule()}
     */
    default Module hudSettingsNavigationTarget() {
        return hudHostModule();
    }

    /**
     * Host enabled and optional per-panel visibility when the host registers multiple panels.
     *
     * @return whether this panel should be drawn in-world and edited
     */
    boolean shouldRenderHudPanel();

    /**
     * Computes content, writes layout dimensions to {@link #getHudState()}, and returns a draw callback executed after positioning.
     *
     * @param glRenderer   the renderer used for measurements
     * @param deltaSeconds frame delta in seconds
     * @param editorMode   true inside the HUD layout editor preview
     * @return deferred draw runnable, or {@code null} to skip this frame
     */
    @Nullable Runnable prepareAndDraw(GuiRenderer glRenderer, float deltaSeconds, boolean editorMode);
}
