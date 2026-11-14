package cc.fascinated.fascinatedutils.gui.node;

import cc.fascinated.fascinatedutils.gui.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui.theme.UiMetrics;
import cc.fascinated.fascinatedutils.gui.node.input.TextInputNode;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public class TextPromptPopupNode extends PopupNode<TextPromptPopupNode> {

    private static final int POPUP_WIDTH = 240;
    private static final int POPUP_HEIGHT = 96;
    private static final int PAD_SIDE = 16;
    private static final int PAD_TOP = 14;
    private static final int PAD_BOTTOM = 12;
    private static final int INPUT_TOP = 34;
    private static final int BUTTON_HEIGHT = UiMetrics.CONTROL_HEIGHT;
    private static final int BUTTON_GAP = 8;
    private static final int BUTTON_WIDTH = 76;

    private final TextInputNode<String> input;
    private Runnable onCancel = () -> {};
    private Runnable onConfirm = () -> {};

    /**
     * Creates a popup asking for a single line of text.
     *
     * @param title           supplies the heading text
     * @param input           shared input node whose value the confirm callback should read
     * @param confirmLabel    supplies the confirm button label
     * @param confirmDisabled supplies whether the confirm button is currently disabled
     */
    public TextPromptPopupNode(Supplier<String> title, TextInputNode<String> input, Supplier<String> confirmLabel, Supplier<Boolean> confirmDisabled) {
        this.input = input;
        setPopupWidth(POPUP_WIDTH);
        setPopupHeight(POPUP_HEIGHT);
        setOnDismiss(() -> onCancel.run());

        addPopupChild(new TextNode(title).setBold(true).setColorResolver(theme -> theme.textPrimary()).alignX(0.5f).top(PAD_TOP));

        PositionedNode<?> inputHost = new PositionedNode<>().left(PAD_SIDE).right(PAD_SIDE).top(INPUT_TOP).height(UiMetrics.CONTROL_HEIGHT);
        inputHost.addChild(input);
        addPopupChild(inputHost);

        ButtonNode cancelButton = new ButtonNode(() -> Component.translatable("alumite.common.cancel").getString())
                .setOnPress(() -> onCancel.run())
                .setRounded(true);
        cancelButton.left(0).size(BUTTON_WIDTH, BUTTON_HEIGHT);
        ButtonNode confirmButton = new ButtonNode(confirmLabel)
                .setOnPress(() -> onConfirm.run())
                .setDisabled(confirmDisabled)
                .setRounded(true);
        confirmButton.left(BUTTON_WIDTH + BUTTON_GAP).size(BUTTON_WIDTH, BUTTON_HEIGHT);

        PositionedNode<?> buttonRow = new PositionedNode<>();
        buttonRow.size(BUTTON_WIDTH * 2 + BUTTON_GAP, BUTTON_HEIGHT).alignX(0.5f).bottom(PAD_BOTTOM);
        buttonRow.addChild(cancelButton);
        buttonRow.addChild(confirmButton);
        addPopupChild(buttonRow);
    }

    public TextPromptPopupNode setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel == null ? () -> {} : onCancel;
        return this;
    }

    public TextPromptPopupNode setOnConfirm(Runnable onConfirm) {
        this.onConfirm = onConfirm == null ? () -> {} : onConfirm;
        return this;
    }

    public TextInputNode<String> input() {
        return input;
    }
}
