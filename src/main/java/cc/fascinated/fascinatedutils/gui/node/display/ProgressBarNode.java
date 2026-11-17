package cc.fascinated.fascinatedutils.gui.node.display;

import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;

import java.util.function.DoubleSupplier;

/**
 * A horizontal progress bar: a track filled from the left up to a fraction of the node's width. Give it a size with
 * {@code width} and {@code height}.
 */
public class ProgressBarNode extends PositionedNode<ProgressBarNode> {
    private static final int DEFAULT_TRACK_ARGB = 0x80000000;
    private static final int DEFAULT_FILL_ARGB = 0xFFFFFFFF;

    private final DoubleSupplier progress;
    private int trackArgb = DEFAULT_TRACK_ARGB;
    private int fillArgb = DEFAULT_FILL_ARGB;
    private float cornerRadius;

    /**
     * Creates a progress bar.
     *
     * @param progress supplies the filled fraction, clamped to 0 through 1 when drawn
     */
    public ProgressBarNode(DoubleSupplier progress) {
        this.progress = progress;
    }

    public ProgressBarNode setTrackArgb(int trackArgb) {
        this.trackArgb = trackArgb;
        return this;
    }

    public ProgressBarNode setFillArgb(int fillArgb) {
        this.fillArgb = fillArgb;
        return this;
    }

    public ProgressBarNode setCornerRadius(float cornerRadius) {
        this.cornerRadius = Math.max(0f, cornerRadius);
        return this;
    }

    @Override
    protected void renderSelf(AlumiteGuiRenderer renderFrame, float deltaSeconds) {
        float barX = bounds().positionX();
        float barY = bounds().positionY();
        float barWidth = bounds().width();
        float barHeight = bounds().height();
        float fillWidth = barWidth * (float) Math.max(0d, Math.min(1d, progress.getAsDouble()));
        drawBar(renderFrame, barX, barY, barWidth, barHeight, trackArgb);
        if (fillWidth > 0f) {
            drawBar(renderFrame, barX, barY, fillWidth, barHeight, fillArgb);
        }
    }

    private void drawBar(AlumiteGuiRenderer renderFrame, float barX, float barY, float barWidth, float barHeight, int argb) {
        if (cornerRadius > 0f) {
            renderFrame.drawRoundedRect(barX, barY, barWidth, barHeight, Math.min(cornerRadius, barWidth * 0.5f), argb);
        }
        else {
            renderFrame.drawRect(barX, barY, barWidth, barHeight, argb);
        }
    }
}
