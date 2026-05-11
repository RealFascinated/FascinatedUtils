package cc.fascinated.fascinatedutils.gui2.node.social;

import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.node.ButtonNode;
import cc.fascinated.fascinatedutils.gui2.node.DividerNode;
import net.minecraft.client.Minecraft;

public class SocialPanelHeaderNode<T extends SocialPanelHeaderNode<T>> extends PositionedNode<T> {

    private static final int BUTTON_SIZE = 18;

    private final DividerNode divider;
    private final ButtonNode closeButton;

    public SocialPanelHeaderNode() {
        height(32).fullWidth();

        divider = new DividerNode();
        divider.bottom(0).fullWidth().height(1);
        addChild(divider);

        closeButton = new ButtonNode();
        closeButton.size(BUTTON_SIZE, BUTTON_SIZE).right(6).alignY(0.5f);
        closeButton.setIconCenter(ModUiTextures.CLOSE.getId());
        closeButton.setRounded(true);
        closeButton.setOnPress(() -> Minecraft.getInstance().setScreen(null));
        addChild(closeButton);
    }
}
