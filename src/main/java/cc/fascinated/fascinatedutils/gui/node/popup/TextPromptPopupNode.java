package cc.fascinated.fascinatedutils.gui.node.popup;

import cc.fascinated.fascinatedutils.gui.node.display.ParagraphNode;

import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.node.input.TextInputNode;
import cc.fascinated.fascinatedutils.gui.render.AlumiteGuiRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UiMetrics;
import cc.fascinated.fascinatedutils.gui.theme.UiTheme;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Asks the user for a single line of text, with an optional validation message under the input.
 */
@Accessors(fluent = true)
public class TextPromptPopupNode extends DialogPopupNode<TextPromptPopupNode> {

    private static final int POPUP_WIDTH = 240;

    @Getter
    private final TextInputNode<String> input;
    private Supplier<String> errorSupplier = () -> null;

    /**
     * Creates a popup asking for a single line of text.
     *
     * @param title           supplies the heading text
     * @param input           shared input node whose value the confirm callback should read
     * @param confirmLabel    supplies the confirm button label
     * @param confirmDisabled supplies whether the confirm button is currently disabled
     */
    public TextPromptPopupNode(Supplier<String> title, TextInputNode<String> input, Supplier<String> confirmLabel, BooleanSupplier confirmDisabled) {
        this.input = input;
        setTitle(title);
        setConfirmLabel(confirmLabel);
        setConfirmDisabled(confirmDisabled);

        PositionedNode<?> inputHost = new PositionedNode<>().fullWidth().height(UiMetrics.CONTROL_HEIGHT);
        inputHost.addChild(input);
        addContent(inputHost);

        ParagraphNode errorNode = new ParagraphNode(() -> errorSupplier.get());
        errorNode.setColorResolver(UiTheme::danger).alignX(0.5f);
        addContent(errorNode);
    }

    /**
     * Sets the validation message shown under the input.
     *
     * @param errorSupplier supplies the message, or null or blank when the input is valid
     * @return this popup
     */
    public TextPromptPopupNode setErrorSupplier(Supplier<String> errorSupplier) {
        this.errorSupplier = errorSupplier == null ? () -> null : errorSupplier;
        return this;
    }

    @Override
    protected int preferredContentWidth(AlumiteGuiRenderer renderFrame) {
        return POPUP_WIDTH - PAD_SIDE * 2;
    }
}
