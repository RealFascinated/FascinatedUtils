package cc.fascinated.fascinatedutils.gui2.node.social;

import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.node.ButtonNode;
import cc.fascinated.fascinatedutils.gui2.node.DividerNode;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;
import net.minecraft.client.Minecraft;

public class SocialPanelHeaderNode extends PositionedNode {

    private final DividerNode divider;
    private final ButtonNode closeButton;

    public SocialPanelHeaderNode() {
        height(40).fullWidth();

        divider = new DividerNode();
        divider.bottom(0).fullWidth();
        addChild(divider);

        closeButton = new ButtonNode("");
        closeButton.size(20, 20).right(6);
        closeButton.setLeftIcon(ModUiTextures.CLOSE.getId());
        closeButton.setRounded(true);
        closeButton.setOnPress(() -> Minecraft.getInstance().setScreen(null));
        addChild(closeButton);
    }

    @Override
    public void layout(RenderFrame renderFrame, int parentX, int parentY, int parentWidth, int parentHeight) {
        int separatorHeight = renderFrame.theme().separatorThickness();
        divider.height(separatorHeight);
        super.layout(renderFrame, parentX, parentY, parentWidth, parentHeight);
        int height = bounds().height() - separatorHeight;
        closeButton.layout(renderFrame,
                bounds().positionX() + bounds().width() - 20 - 6,
                bounds().positionY() + (height - 20) / 2,
                20, 20);
    }
}
