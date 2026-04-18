package cc.fascinated.fascinatedutils.gui.renderer.operations;

import cc.fascinated.fascinatedutils.renderer.Renderer2D;
import net.minecraft.network.chat.Component;

/**
 * Deferred MiniMessage-backed text draw.
 */
public class MiniMessageTextOperation extends GuiRenderOperation<MiniMessageTextOperation> {
    private Component parsed;
    private boolean shadow;

    /**
     * Configure this draw from already-parsed {@link Component}.
     *
     * @param parsed parsed rich text to draw
     * @param shadow whether vanilla text shadow is drawn
     * @return this operation for chaining
     */
    public MiniMessageTextOperation set(Component parsed, boolean shadow) {
        this.parsed = parsed;
        this.shadow = shadow;
        return this;
    }

    @Override
    public void execute(Renderer2D renderer2D) {
        renderer2D.drawTextImmediate(parsed, x, y, colorArgb, shadow);
    }
}
