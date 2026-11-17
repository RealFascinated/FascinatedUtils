package cc.fascinated.fascinatedutils.gui.node.display;

import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
import net.minecraft.network.chat.Component;

/**
 * One line of rich text: either MiniMessage markup or a vanilla {@link Component}. It is sized to the text and drawn
 * from its top-left corner, so a parent aligns it with the box constraints ({@code alignX}, {@code left}, ...).
 * Measurement and drawing go through the renderer's text caches, so rebuilding the node every frame stays cheap.
 */
public class RichTextNode extends PositionedNode<RichTextNode> {
    private static final int DEFAULT_COLOR_ARGB = 0xFFFFFFFF;

    private final String miniMessage;
    private final Component component;
    private final boolean shadow;
    private int colorArgb = DEFAULT_COLOR_ARGB;

    private RichTextNode(String miniMessage, Component component, boolean shadow) {
        this.miniMessage = miniMessage;
        this.component = component;
        this.shadow = shadow;
    }

    /**
     * Creates a line of MiniMessage text.
     *
     * @param miniMessage MiniMessage markup to draw
     * @param shadow      whether the text has a drop shadow
     * @return the text node
     */
    public static RichTextNode miniMessage(String miniMessage, boolean shadow) {
        return new RichTextNode(miniMessage == null ? "" : miniMessage, null, shadow);
    }

    /**
     * Creates a line of vanilla component text.
     *
     * @param component the text to draw
     * @param shadow    whether the text has a drop shadow
     * @return the text node
     */
    public static RichTextNode component(Component component, boolean shadow) {
        return new RichTextNode(null, component, shadow);
    }

    /**
     * Sets the base color of component text; MiniMessage text carries its own colors.
     *
     * @param colorArgb packed ARGB color
     * @return this node
     */
    public RichTextNode setColorArgb(int colorArgb) {
        this.colorArgb = colorArgb;
        return this;
    }

    @Override
    protected int intrinsicWidth(AlumiteGuiRenderer renderFrame, int parentWidth) {
        return miniMessage != null ? renderFrame.measureMiniMessageTextWidth(miniMessage) : renderFrame.measureTextWidth(component);
    }

    @Override
    protected int intrinsicHeight(AlumiteGuiRenderer renderFrame, int parentHeight, int resolvedWidth) {
        return renderFrame.fontHeight();
    }

    @Override
    protected void renderSelf(AlumiteGuiRenderer renderFrame, float deltaSeconds) {
        if (miniMessage != null) {
            renderFrame.drawMiniMessageText(miniMessage, bounds().positionX(), bounds().positionY(), shadow);
        }
        else {
            renderFrame.drawComponentText(component, bounds().positionX(), bounds().positionY(), colorArgb, shadow);
        }
    }
}
