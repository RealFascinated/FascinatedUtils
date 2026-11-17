package cc.fascinated.fascinatedutils.systems.hud.node;

import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
import cc.fascinated.fascinatedutils.systems.hud.panel.HudPanel;
import cc.fascinated.fascinatedutils.systems.hud.layout.HudPanelMerger;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds every panel of one HUD frame, in draw order. After all panels have been laid out and anchored it runs the
 * {@link HudPanelMerger}, so touching panels square their shared corners before anything draws. It finds the
 * {@link HudPanelNode}s among its children and their descendants, so the HUD editor can wrap each one in its own node.
 *
 * <p>The layer fills its slot and lays its children out itself every frame, because the merge depends on every
 * panel's final position.
 */
public class HudLayerNode extends PositionedNode<HudLayerNode> {
    private final HudPanelMerger merger;
    private final List<HudPanel> panels = new ArrayList<>();

    /**
     * Creates a layer.
     *
     * @param merger merges the corners of touching panels
     */
    public HudLayerNode(HudPanelMerger merger) {
        this.merger = merger;
    }

    @Override
    public void layout(AlumiteGuiRenderer renderFrame, int slotX, int slotY, int slotWidth, int slotHeight) {
        if (!visible()) {
            return;
        }
        bounds().set(slotX, slotY, slotWidth, slotHeight);
        panels.clear();
        for (UiNode childNode : childrenView()) {
            if (childNode.visible()) {
                childNode.layout(renderFrame, slotX, slotY, slotWidth, slotHeight);
                collectPanels(childNode);
            }
        }
        merger.update(panels, true);
    }

    private void collectPanels(UiNode node) {
        if (node instanceof HudPanelNode panelNode) {
            panels.add(panelNode.getPanel());
            return;
        }
        for (UiNode childNode : node.childrenView()) {
            collectPanels(childNode);
        }
    }
}
