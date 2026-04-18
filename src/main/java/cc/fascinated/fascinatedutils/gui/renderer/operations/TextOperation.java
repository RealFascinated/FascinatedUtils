package cc.fascinated.fascinatedutils.gui.renderer.operations;

import cc.fascinated.fascinatedutils.renderer.Renderer2D;

/**
 * Deferred plain-string GUI text draw (Meteor-style {@code TextOperation}).
 */
public class TextOperation extends GuiRenderOperation<TextOperation> {
    private String text;
    private boolean shadow;
    private boolean bold;

    /**
     * Configure this operation for a literal string draw.
     *
     * @param text   string content to draw
     * @param shadow whether vanilla text shadow is drawn
     * @param bold   whether bold styling is applied
     * @return this operation for chaining
     */
    public TextOperation set(String text, boolean shadow, boolean bold) {
        this.text = text;
        this.shadow = shadow;
        this.bold = bold;
        return this;
    }

    @Override
    public void execute(Renderer2D renderer2D) {
        renderer2D.drawTextImmediate(text, x, y, colorArgb, shadow, bold);
    }
}
