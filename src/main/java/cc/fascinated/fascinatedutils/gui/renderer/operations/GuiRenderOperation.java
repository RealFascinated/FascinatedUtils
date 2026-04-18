package cc.fascinated.fascinatedutils.gui.renderer.operations;

import cc.fascinated.fascinatedutils.renderer.Renderer2D;

/**
 * Base type for deferred GUI draw steps (Meteor-style {@code GuiRenderOperation}).
 *
 * @param <T> concrete operation type for fluent return types
 */
public abstract class GuiRenderOperation<T extends GuiRenderOperation<T>> {
    protected float x;
    protected float y;
    protected int colorArgb;

    /**
     * Set the draw origin and packed ARGB color for this operation.
     *
     * @param positionX left origin in logical pixels
     * @param positionY top origin in logical pixels
     * @param colorArgb fill or text color in packed ARGB
     * @return this operation for chaining
     */
    @SuppressWarnings("unchecked")
    public T set(float positionX, float positionY, int colorArgb) {
        this.x = positionX;
        this.y = positionY;
        this.colorArgb = colorArgb;
        return (T) this;
    }

    /**
     * Execute this operation using the low-level immediate renderer (after batched geometry has been flushed).
     *
     * @param renderer2D backend that performs vanilla {@link net.minecraft.client.gui.GuiGraphicsExtractor} text draws
     */
    public abstract void execute(Renderer2D renderer2D);
}
