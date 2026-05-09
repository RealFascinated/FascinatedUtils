package cc.fascinated.fascinatedutils.gui2.node.social;

import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.node.TextNode;
import cc.fascinated.fascinatedutils.gui2.theme.UiTheme;

public class SocialSectionLabelNode extends PositionedNode {

    private String label;
    private final TextNode text;

    public SocialSectionLabelNode(String label) {
        this.label = label;
        height(20).fullWidth();
        text = new TextNode(() -> this.label)
                .setColorResolver(UiTheme::textMuted)
                .setTextAlign(0f, 0.5f);
        text.left(4).right(0).fullHeight();
        addChild(text);
    }

    public SocialSectionLabelNode setLabel(String label) {
        this.label = label;
        return this;
    }
}
