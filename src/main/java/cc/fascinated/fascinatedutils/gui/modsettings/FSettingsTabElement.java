package cc.fascinated.fascinatedutils.gui.modsettings;

import cc.fascinated.fascinatedutils.gui.core.*;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.widgets.*;

public class FSettingsTabElement extends FWidget {

    private final FNodeRegistry nodes = new FNodeRegistry();
    private final FNodeWidget root;

    private FState<Float> scrollRef;

    public FSettingsTabElement() {
        root = new FNodeWidget(nodes.get("settings", this::buildRootWidget));
        addChild(root);
    }

    @Override
    public boolean fillsVerticalInColumn() {
        return true;
    }

    @Override
    public boolean fillsHorizontalInRow() {
        return true;
    }

    @Override
    public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutWidth, float layoutHeight) {
        setBounds(layoutX, layoutY, layoutWidth, layoutHeight);
        root.layout(measure, layoutX, layoutY, layoutWidth, layoutHeight);
        nodes.gc();
    }

    public void reset() {
        if (scrollRef != null) scrollRef.set(0f);
    }

    public void disposeDeclarativeSubtree() {
        nodes.dispose();
    }

    private FWidget buildRootWidget(FWidgetNode.RenderContext ctx) {
        scrollRef = ctx.useState(0f);
        return new FWidget() {
            private float lastWidth = Float.NaN;
            private float lastHeight = Float.NaN;

            @Override
            public boolean fillsHorizontalInRow() {
                return true;
            }

            @Override
            public boolean fillsVerticalInColumn() {
                return true;
            }

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
                boolean dimChanged = Math.abs(lw - lastWidth) > 0.5f || Math.abs(lh - lastHeight) > 0.5f;
                if (dimChanged || childrenView().isEmpty()) {
                    lastWidth = lw;
                    lastHeight = lh;
                    clearChildren();
                    addChild(ModSettingsRegistrySettingsTabBuilder.buildSettingsTab(lw, lh, scrollRef));
                }
                for (FWidget child : childrenView()) {
                    child.layout(measure, lx, ly, lw, lh);
                }
            }
        };
    }
}
