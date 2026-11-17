package cc.fascinated.fascinatedutils.gui.node.display;

import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;

/**
 * A progress bar made of GUI atlas sprites, like the vanilla boss bar: track sprites are stretched over the whole box,
 * then fill sprites are drawn cropped from the left to a whole number of pixels of the node's width. Give it a size
 * with {@code width} and {@code height}; the fill sprites are expected to have the size of the node.
 */
public class SpriteProgressBarNode extends PositionedNode<SpriteProgressBarNode> {
    private static final int OPAQUE_WHITE = 0xFFFFFFFF;

    private final DoubleSupplier progress;
    private final List<Identifier> trackSprites = new ArrayList<>(2);
    private final List<Identifier> fillSprites = new ArrayList<>(2);

    /**
     * Creates a sprite bar.
     *
     * @param progress supplies the filled fraction, clamped to 0 through 1 when drawn
     */
    public SpriteProgressBarNode(DoubleSupplier progress) {
        this.progress = progress;
    }

    /**
     * Adds a sprite stretched over the whole bar, drawn below the fill and in the order added.
     *
     * @param sprite GUI atlas sprite id
     * @return this node
     */
    public SpriteProgressBarNode addTrackSprite(Identifier sprite) {
        trackSprites.add(sprite);
        return this;
    }

    /**
     * Adds a sprite cropped to the filled part of the bar, drawn in the order added.
     *
     * @param sprite GUI atlas sprite id
     * @return this node
     */
    public SpriteProgressBarNode addFillSprite(Identifier sprite) {
        fillSprites.add(sprite);
        return this;
    }

    @Override
    protected void renderSelf(AlumiteGuiRenderer renderFrame, float deltaSeconds) {
        float barX = bounds().positionX();
        float barY = bounds().positionY();
        int barWidth = bounds().width();
        int barHeight = bounds().height();
        for (Identifier sprite : trackSprites) {
            renderFrame.drawSprite(sprite, barX, barY, barWidth, barHeight, OPAQUE_WHITE);
        }
        int fillWidth = Mth.lerpDiscrete((float) Mth.clamp(progress.getAsDouble(), 0d, 1d), 0, barWidth);
        if (fillWidth <= 0) {
            return;
        }
        for (Identifier sprite : fillSprites) {
            renderFrame.drawSprite(sprite, barWidth, barHeight, 0, 0, barX, barY, fillWidth, barHeight);
        }
    }
}
