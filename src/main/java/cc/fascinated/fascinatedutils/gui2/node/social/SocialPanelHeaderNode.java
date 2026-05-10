package cc.fascinated.fascinatedutils.gui2.node.social;

import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.node.ButtonNode;
import cc.fascinated.fascinatedutils.gui2.node.DividerNode;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;
import net.minecraft.client.Minecraft;

public class SocialPanelHeaderNode extends PositionedNode {

    private static final int CLOSE_BUTTON_SIZE = 18;

    private final DividerNode divider;
    private final ButtonNode closeButton;

    public SocialPanelHeaderNode() {
        height(32).fullWidth();

        divider = new DividerNode();
        divider.bottom(0).fullWidth();
        addChild(divider);

        closeButton = new ButtonNode("");
        closeButton.size(CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE).right(6);
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
                bounds().positionX() + bounds().width() - CLOSE_BUTTON_SIZE - 6,
                bounds().positionY() + (height - CLOSE_BUTTON_SIZE) / 2,
                CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE);
    }
}
