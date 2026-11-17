package cc.fascinated.fascinatedutils.gui.node.display;

import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UiTheme;
import net.minecraft.resources.Identifier;

import java.util.function.Function;

/**
 * Draws a GUI atlas sprite stretched over the node's box, tinted with a fixed or theme-resolved color.
 */
public class SpriteNode extends PositionedNode<SpriteNode> {
    private final Identifier sprite;
    private Function<UiTheme, Integer> tintResolver = theme -> 0xFFFFFFFF;

    public SpriteNode(Identifier sprite) {
        this.sprite = sprite;
    }

    public SpriteNode setTintResolver(Function<UiTheme, Integer> tintResolver) {
        this.tintResolver = tintResolver;
        return this;
    }

    @Override
    protected void renderSelf(AlumiteGuiRenderer renderFrame, float deltaSeconds) {
        renderFrame.drawSprite(sprite, bounds().positionX(), bounds().positionY(), bounds().width(), bounds().height(), tintResolver.apply(renderFrame.theme()));
    }
}
