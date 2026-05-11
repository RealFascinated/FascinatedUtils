package cc.fascinated.fascinatedutils.gui2.node;

import cc.fascinated.fascinatedutils.gui2.core.UiNode;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;

public class LoadingSpinnerNode extends UiNode {

    private static final int DOT_COUNT = 8;
    private static final int RADIUS = 10;
    private static final int DOT_SIZE = 6;

    private float elapsed = 0f;

    @Override
    protected void renderSelf(RenderFrame renderFrame, float deltaSeconds) {
        elapsed += deltaSeconds;
        int cx = bounds().positionX() + bounds().width() / 2;
        int cy = bounds().positionY() + bounds().height() / 2;
        float progress = elapsed % 1f;
        for (int dot = 0; dot < DOT_COUNT; dot++) {
            double angle = 2 * Math.PI * dot / (double) DOT_COUNT - Math.PI / 2;
            float dist = Math.abs(progress - dot / (float) DOT_COUNT);
            if (dist > 0.5f) dist = 1f - dist;
            float alpha = Math.max(0.15f, 1f - dist * 2f);
            int dotX = (int) Math.round(cx + RADIUS * Math.cos(angle)) - DOT_SIZE / 2;
            int dotY = (int) Math.round(cy + RADIUS * Math.sin(angle)) - DOT_SIZE / 2;
            renderFrame.drawRoundedRect(dotX, dotY, DOT_SIZE, DOT_SIZE, DOT_SIZE / 2,
                    (Math.round(alpha * 255) << 24) | 0xFFFFFF);
        }
    }
}
