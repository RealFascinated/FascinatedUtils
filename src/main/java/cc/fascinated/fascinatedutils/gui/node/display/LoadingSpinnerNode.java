package cc.fascinated.fascinatedutils.gui.node.display;

import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;

public class LoadingSpinnerNode extends UiNode {

    private static final int DOT_COUNT = 8;
    private static final int DOT_SIZE = 6;
    private static final int DEFAULT_RADIUS = 10;
    private static final long ROTATION_PERIOD_NANOS = 1_000_000_000L;

    @Override
    protected void renderSelf(AlumiteGuiRenderer renderFrame, float deltaSeconds) {
        int centerX = bounds().positionX() + bounds().width() / 2;
        int centerY = bounds().positionY() + bounds().height() / 2;
        // The ring fills the smaller side of the bounds, up to the default size, so it never spills out of a small slot.
        int radius = Math.max(1, Math.min(DEFAULT_RADIUS, (Math.min(bounds().width(), bounds().height()) - DOT_SIZE) / 2));
        float progress = (System.nanoTime() % ROTATION_PERIOD_NANOS) / (float) ROTATION_PERIOD_NANOS;
        for (int dot = 0; dot < DOT_COUNT; dot++) {
            double angle = 2 * Math.PI * dot / DOT_COUNT - Math.PI / 2;
            float distance = Math.abs(progress - dot / (float) DOT_COUNT);
            if (distance > 0.5f) {
                distance = 1f - distance;
            }
            float brightness = Math.max(0.15f, 1f - distance * 2f);
            int dotX = (int) Math.round(centerX + radius * Math.cos(angle)) - DOT_SIZE / 2;
            int dotY = (int) Math.round(centerY + radius * Math.sin(angle)) - DOT_SIZE / 2;
            renderFrame.drawRoundedRect(dotX, dotY, DOT_SIZE, DOT_SIZE, DOT_SIZE / 2f, renderFrame.theme().spinnerDot(brightness));
        }
    }
}
