package cc.fascinated.fascinatedutils.gui.widgets;

import cc.fascinated.fascinatedutils.gui.core.UiFrameContext;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;

/**
 * A circular badge widget with a solid color fill and an optional centered text label.
 *
 * <p>Layout always produces a {@code radius * 2} square regardless of the supplied bounds.
 * Not interactive — {@link cc.fascinated.fascinatedutils.gui.core.PointerHitKind#NONE}.
 */
public class FBadgeWidget extends FWidget {

    private final float radius;
    private final int color;
    private String text = null;

    /**
     * Creates a badge with the given circle radius and fill color.
     *
     * @param radius half the width/height of the badge in logical pixels
     * @param color  packed ARGB fill color
     */
    public FBadgeWidget(float radius, int color) {
        this.radius = radius;
        this.color = color;
    }

    /**
     * Sets the text label drawn centered inside the badge. Pass {@code null} to show a plain dot.
     *
     * @param text label text, or {@code null} for no label
     */
    public void setText(String text) {
        this.text = text;
    }

    @Override
    public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
        setBounds(lx, ly, radius * 2f, radius * 2f);
    }

    @Override
    protected void renderSelf(GuiRenderer graphics, UiFrameContext frame, float deltaSeconds) {
        graphics.fillRoundedRect(x(), y(), w(), h(), radius, color, RectCornerRoundMask.ALL);
        if (text != null && !text.isEmpty()) {
            graphics.drawCenteredText(text, x() + radius, y() + (h() - graphics.getFontCapHeight()) / 2f, 0xFFFFFFFF, false, true);
        }
    }
}
