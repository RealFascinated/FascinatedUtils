package cc.fascinated.fascinatedutils.systems.hud;

import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import org.jspecify.annotations.Nullable;

public interface HudRenderableModule {
    String getId();

    String getName();

    float getMinWidth();

    ModuleHudState getHudState();

    /**
     * Compute content, measure dimensions, update the module's layout state,
     * and return a draw callback. The framework positions the widget between this call
     * and executing the returned callback.
     *
     * @param glRenderer   the renderer (used for text measurement)
     * @param deltaSeconds frame delta in seconds
     * @param editorMode   true when rendering in the HUD editor
     * @return a callback that draws the widget content, or {@code null} to skip this frame
     */
    @Nullable Runnable prepareAndDraw(GuiRenderer glRenderer, float deltaSeconds, boolean editorMode);
}
