package cc.fascinated.fascinatedutils.gui.node.popup;

import cc.fascinated.fascinatedutils.gui.node.display.ParagraphNode;

import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UiTheme;

/**
 * Asks the user to confirm or cancel an action, with a title and an optional wrapped description.
 */
public class ConfirmPopupNode extends DialogPopupNode<ConfirmPopupNode> {
    private String description;

    private final ParagraphNode descriptionNode;

    public ConfirmPopupNode() {
        descriptionNode = new ParagraphNode(() -> description);
        descriptionNode.setColorResolver(UiTheme::textMuted).alignX(0.5f);
        descriptionNode.setVisible(false);
        addContent(descriptionNode);
    }

    /**
     * Sets the explanation shown under the title.
     *
     * @param description the text, which wraps to the popup width; null or blank hides it
     * @return this popup
     */
    public ConfirmPopupNode setDescription(String description) {
        this.description = description;
        descriptionNode.setVisible(description != null && !description.isBlank());
        return this;
    }

    @Override
    protected int preferredContentWidth(AlumiteGuiRenderer renderFrame) {
        int titleWidth = super.preferredContentWidth(renderFrame);
        return description == null ? titleWidth : Math.max(titleWidth, renderFrame.measureTextWidth(description, false));
    }
}
