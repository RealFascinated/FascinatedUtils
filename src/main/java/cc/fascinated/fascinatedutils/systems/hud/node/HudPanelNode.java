package cc.fascinated.fascinatedutils.systems.hud.node;

import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.gui.layout.AxisConstraints;
import cc.fascinated.fascinatedutils.gui.layout.Axis;
import cc.fascinated.fascinatedutils.gui.layout.MeasuredSize;
import cc.fascinated.fascinatedutils.gui.node.display.AlphaNode;
import cc.fascinated.fascinatedutils.gui.node.display.PanelBoxNode;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.layout.HudLayoutCanvas;
import cc.fascinated.fascinatedutils.systems.hud.panel.HudPanel;
import cc.fascinated.fascinatedutils.systems.hud.ModuleHudState;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.util.function.DoubleSupplier;

/**
 * The chrome around one HUD panel's content, shared by the in-game HUD and the HUD editor. It owns everything that is
 * not the panel's business: the background, border and rounded or merged corners (a {@link PanelBoxNode}), the host's
 * padding, the minimum width, the fade, the scale and the anchored position.
 *
 * <p>Layout measures the content once with unbounded space, raises the size to the panel minimum, commits it to the
 * panel's {@link ModuleHudState} (which the merger and the editor read), resolves the anchored position and then
 * arranges box and content at that position in unscaled canvas pixels. Drawing scales the whole subtree about the
 * panel's top-left corner. The node's own {@link #bounds()} are the scaled rectangle on the canvas.
 *
 * <p>The node lays itself out every frame instead of using measure and arrange change tracking, because its size
 * feeds back into the panel state that other panels' placement depends on.
 */
public class HudPanelNode extends AlphaNode {
    private static final int UNBOUNDED = 8192;

    /**
     * The panel this node draws.
     */
    @Getter
    private final HudPanel panel;
    private final PanelBoxNode box;
    private final @Nullable UiNode content;
    private final int padding;
    private final boolean followAnchor;
    private int originX;
    private int originY;
    private float scale = 1f;

    /**
     * Creates the chrome for a panel.
     *
     * @param panel        panel being drawn
     * @param content      the composed content, or {@code null} for an empty panel that only shows chrome
     * @param padding      space between the panel edge and the content, in pixels
     * @param opacity      supplies the panel opacity from 0 to 1, read when drawing
     * @param editorMode   whether an invisible panel outlines itself so it can be found in the HUD editor
     * @param followAnchor whether layout recomputes the position from the panel's anchor; false while the editor moves it
     */
    public HudPanelNode(HudPanel panel, @Nullable UiNode content, int padding, DoubleSupplier opacity, boolean editorMode, boolean followAnchor) {
        super(opacity);
        this.panel = panel;
        this.content = content;
        this.padding = padding;
        this.followAnchor = followAnchor;
        HudHostModule host = panel.hudHostModule();
        ModuleHudState state = panel.getHudState();
        box = new PanelBoxNode()
                .setFillArgb(host.backgroundArgb())
                .setBorderArgb(host.borderArgb())
                .setBorderThickness(host.getBorderThicknessValue())
                .setCornerRadius(host.getCornerRadius())
                .setDottedOutline(editorMode)
                .setSquaredCorners(state::getSquaredCornerMask);
        addChild(box);
        addChild(content);
    }

    @Override
    public void layout(AlumiteGuiRenderer renderFrame, int slotX, int slotY, int slotWidth, int slotHeight) {
        if (!visible()) {
            return;
        }
        MeasuredSize contentSize = content == null ? null : content.measure(renderFrame, UNBOUNDED, UNBOUNDED);
        int contentWidth = contentSize == null ? 0 : contentSize.width();
        int contentHeight = contentSize == null ? 0 : contentSize.height();
        float layoutWidth = Math.max(1f, Math.max(panel.getMinWidth(), contentWidth + 2f * padding));
        float layoutHeight = Math.max(1f, contentHeight + 2f * padding);
        panel.commitLayout(layoutWidth, layoutHeight);
        if (followAnchor) {
            panel.applyHudAnchorToPosition(HudLayoutCanvas.width(), HudLayoutCanvas.height());
        }

        ModuleHudState state = panel.getHudState();
        scale = state.getScale();
        originX = Math.round(state.getPositionX());
        originY = Math.round(state.getPositionY());
        int boxWidth = Math.round(layoutWidth);
        int boxHeight = Math.round(layoutHeight);
        bounds().set(originX, originY, Math.round(layoutWidth * scale), Math.round(layoutHeight * scale));

        box.width(boxWidth).height(boxHeight);
        box.layout(renderFrame, originX, originY, boxWidth, boxHeight);
        if (content != null) {
            int bandWidth = Math.max(0, boxWidth - 2 * padding);
            int bandHeight = Math.max(0, boxHeight - 2 * padding);
            long horizontalBox = content.resolveBox(Axis.HORIZONTAL, originX + padding, bandWidth, contentWidth);
            long verticalBox = content.resolveBox(Axis.VERTICAL, originY + padding, bandHeight, contentHeight);
            content.arrange(renderFrame, AxisConstraints.boxStart(horizontalBox), AxisConstraints.boxStart(verticalBox), AxisConstraints.boxSize(horizontalBox), AxisConstraints.boxSize(verticalBox));
        }
    }

    @Override
    public void render(AlumiteGuiRenderer renderFrame, float deltaSeconds) {
        if (!visible()) {
            return;
        }
        boolean scaled = scale != 1f;
        if (scaled) {
            // Scale about the panel's top-left corner while children keep their canvas coordinates.
            renderFrame.pushTransform(originX, originY, scale, scale);
            renderFrame.pushTranslate(-originX, -originY);
        }
        try {
            super.render(renderFrame, deltaSeconds);
        }
        finally {
            if (scaled) {
                renderFrame.popTranslate();
                renderFrame.popTransform();
            }
        }
    }
}
