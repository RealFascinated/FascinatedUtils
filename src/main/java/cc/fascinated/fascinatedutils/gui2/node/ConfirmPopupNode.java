package cc.fascinated.fascinatedutils.gui2.node;

import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;
import cc.fascinated.fascinatedutils.gui2.theme.UiTheme;
import cc.fascinated.fascinatedutils.gui2.theme.UiThemeRepository;

import java.util.function.Function;
import net.minecraft.network.chat.Component;

public class ConfirmPopupNode extends PopupNode<ConfirmPopupNode> {

    private static final int POPUP_HEIGHT = 90;
    private static final int PAD_TOP = 16;
    private static final int PAD_BOTTOM = 12;
    private static final int PAD_SIDE = 16;
    private static final int MESSAGE_TITLE_GAP = 6;
    private static final int BUTTON_HEIGHT = 22;
    private static final int BUTTON_GAP = 8;
    private static final int BUTTON_WIDTH = 76;
    private static final int BUTTON_ROW_WIDTH = BUTTON_WIDTH * 2 + BUTTON_GAP;
    private static final int MIN_POPUP_WIDTH = BUTTON_ROW_WIDTH + PAD_SIDE * 2;

    private String title = "";
    private String description = null;
    private String cancelLabel = Component.translatable("alumite.common.cancel").getString();
    private String confirmLabel = Component.translatable("alumite.common.confirm").getString();
    private Runnable onCancel = () -> {};
    private Runnable onConfirm = () -> {};

    private final ButtonNode cancelButton;
    private final ButtonNode confirmButton;
    private final TextNode titleTextNode;
    private final TextNode descriptionTextNode;

    public ConfirmPopupNode() {
        setPopupHeight(POPUP_HEIGHT);
        setOnDismiss(() -> onCancel.run());

        titleTextNode = new TextNode(() -> title);
        titleTextNode.setBold(true).setColorResolver(theme -> theme.textPrimary()).alignX(0.5f).top(PAD_TOP);
        addPopupChild(titleTextNode);

        descriptionTextNode = new TextNode(() -> description != null ? description : "");
        descriptionTextNode.setColorResolver(theme -> theme.textMuted()).alignX(0.5f).top(PAD_TOP);
        descriptionTextNode.setVisible(false);
        addPopupChild(descriptionTextNode);

        cancelButton = new ButtonNode(() -> cancelLabel).setOnPress(() -> onCancel.run());
        cancelButton.left(0).size(BUTTON_WIDTH, BUTTON_HEIGHT);
        cancelButton.setRounded(true);

        confirmButton = new ButtonNode(() -> confirmLabel).setOnPress(() -> onConfirm.run());
        confirmButton.left(BUTTON_WIDTH + BUTTON_GAP).size(BUTTON_WIDTH, BUTTON_HEIGHT);
        confirmButton.setRounded(true);

        PositionedNode buttonRow = new PositionedNode();
        buttonRow.size(BUTTON_ROW_WIDTH, BUTTON_HEIGHT).alignX(0.5f).bottom(PAD_BOTTOM);
        buttonRow.addChild(cancelButton);
        buttonRow.addChild(confirmButton);
        addPopupChild(buttonRow);
    }

    public ConfirmPopupNode setTitle(String title) {
        this.title = title == null ? "" : title;
        return this;
    }

    public ConfirmPopupNode setDescription(String description) {
        this.description = description;
        descriptionTextNode.setVisible(description != null && !description.isBlank());
        return this;
    }

    public ConfirmPopupNode setCancelLabel(String cancelLabel) {
        this.cancelLabel = cancelLabel == null ? "Cancel" : cancelLabel;
        cancelButton.setLabel(this.cancelLabel);
        return this;
    }

    public ConfirmPopupNode setConfirmLabel(String confirmLabel) {
        this.confirmLabel = confirmLabel == null ? Component.translatable("alumite.common.confirm").getString() : confirmLabel;
        confirmButton.setLabel(this.confirmLabel);
        return this;
    }

    public ConfirmPopupNode setConfirmLabelColorArgb(Integer confirmLabelColorArgb) {
        confirmButton.setLabelColorArgb(confirmLabelColorArgb);
        return this;
    }

    public ConfirmPopupNode setConfirmLabelColorResolver(Function<UiTheme, Integer> colorResolver) {
        confirmButton.setLabelColorArgb(colorResolver != null ? colorResolver.apply(UiThemeRepository.get()) : null);
        return this;
    }

    public ConfirmPopupNode setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel == null ? () -> {} : onCancel;
        setOnDismiss(this.onCancel);
        cancelButton.setOnPress(this.onCancel);
        return this;
    }

    public ConfirmPopupNode setOnConfirm(Runnable onConfirm) {
        this.onConfirm = onConfirm == null ? () -> {} : onConfirm;
        confirmButton.setOnPress(this.onConfirm);
        return this;
    }

    @Override
    protected void configurePopup(RenderFrame renderFrame) {
        boolean hasDescription = description != null && !description.isBlank();
        setPopupHeight(hasDescription ? POPUP_HEIGHT + renderFrame.fontHeight() + MESSAGE_TITLE_GAP : POPUP_HEIGHT);
        if (hasDescription) {
            descriptionTextNode.top(PAD_TOP + renderFrame.fontHeight() + MESSAGE_TITLE_GAP);
        }
        int titleWidth = renderFrame.measureTextWidth(title, true);
        int descriptionWidth = hasDescription ? renderFrame.measureTextWidth(description, false) : 0;
        setPopupWidth(Math.max(MIN_POPUP_WIDTH, Math.max(titleWidth, descriptionWidth) + PAD_SIDE * 2));
    }
}
