package cc.fascinated.fascinatedutils.gui.screens.impl.hudeditor;

import cc.fascinated.fascinatedutils.systems.hud.HudPanel;
import cc.fascinated.fascinatedutils.systems.hud.HudPanelMerger;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects the panel nodes of one HUD editor frame so touching panels merge their corners exactly like the in-game HUD.
 */
public class HudEditorMergeGroup {

    private final HudPanelMerger merger;
    private final List<HudPanelNode> nodes = new ArrayList<>();
    private final List<HudPanel> drawnPanels = new ArrayList<>();

    public HudEditorMergeGroup(HudPanelMerger merger) {
        this.merger = merger;
    }

    /**
     * Registers a panel node; the first registered node runs the merge for the frame.
     *
     * @param node panel node belonging to this group
     */
    public void register(HudPanelNode node) {
        nodes.add(node);
    }

    /**
     * Recomputes merged corners from the panels that produced a preview this frame. Only the first registered node
     * does the work, since every node has finished layout before any node renders.
     *
     * @param node node about to render
     */
    public void updateIfLeader(HudPanelNode node) {
        if (nodes.isEmpty() || nodes.getFirst() != node) {
            return;
        }
        drawnPanels.clear();
        for (HudPanelNode candidate : nodes) {
            if (candidate.hasPreview()) {
                drawnPanels.add(candidate.getPanel());
            }
        }
        merger.update(drawnPanels, true);
    }
}
