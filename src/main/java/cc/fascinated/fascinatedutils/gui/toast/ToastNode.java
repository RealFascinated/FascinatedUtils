package cc.fascinated.fascinatedutils.gui.toast;

import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.gui.layout.PositionedNode;
import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.gui.node.layout.PadNode;
import cc.fascinated.fascinatedutils.gui.node.display.ParagraphNode;
import cc.fascinated.fascinatedutils.gui.node.display.RectNode;
import cc.fascinated.fascinatedutils.gui.node.display.SpriteNode;
import cc.fascinated.fascinatedutils.gui.node.display.TextNode;
import cc.fascinated.fascinatedutils.gui.theme.UiMetrics;
import cc.fascinated.fascinatedutils.gui.theme.UiTheme;
import cc.fascinated.fascinatedutils.gui.theme.UiThemeRepository;

/**
 * The card of one {@link Toast}: a rounded surface with a type icon, a bold title and a wrapped message. Hovering
 * raises the surface, and clicking it dismisses the toast.
 */
public class ToastNode extends RectNode {

    static final int TOAST_WIDTH = 220;
    static final int MIN_HEIGHT = 46;

    private static final int ICON_SLOT_WIDTH = 30;
    private static final int ICON_SIZE = 8;
    private static final int TEXT_PADDING_VERTICAL = 12;
    private static final int TEXT_PADDING_END = 10;
    private static final int TEXT_WIDTH = TOAST_WIDTH - ICON_SLOT_WIDTH - TEXT_PADDING_END;
    private static final int TITLE_GAP = 4;
    private static final int LINE_GAP = 2;
    private static final int MAX_MESSAGE_LINES = 4;

    private final Runnable dismissAction;

    /**
     * Builds the card for a toast.
     *
     * @param toast         the toast to show
     * @param dismissAction runs when the card is clicked
     */
    public ToastNode(Toast toast, Runnable dismissAction) {
        this.dismissAction = dismissAction;
        setCornerRadius(UiMetrics.TOAST_RADIUS);
        setFillSupplier(() -> isHovered() ? UiThemeRepository.get().surfaceActive() : UiThemeRepository.get().surfaceRaised());
        setBorderResolver(UiTheme::borderSubtle);
        width(TOAST_WIDTH).columnGap(0);

        ParagraphNode title = new ParagraphNode(toast::title).setBold(true).setMaxLines(1);
        title.width(TEXT_WIDTH);
        PositionedNode<?> textColumn = new PositionedNode<>().columnGap(TITLE_GAP).width(TEXT_WIDTH);
        textColumn.addChild(title);
        if (!toast.message().isEmpty()) {
            ParagraphNode message = new ParagraphNode(toast::message).setColorResolver(UiTheme::textMuted).setMaxLines(MAX_MESSAGE_LINES).setLineGap(LINE_GAP);
            message.width(TEXT_WIDTH);
            textColumn.addChild(message);
        }

        PositionedNode<?> iconSlot = new PositionedNode<>().size(ICON_SLOT_WIDTH, ICON_SIZE).alignY(0.5f);
        iconSlot.addChild(iconFor(toast.type()));

        PositionedNode<?> row = new PositionedNode<>().rowGap(0).alignY(0.5f);
        row.addChild(iconSlot);
        row.addChild(textColumn);
        addChild(new PadNode().padding(TEXT_PADDING_VERTICAL, TEXT_PADDING_END, TEXT_PADDING_VERTICAL, 0).minSize(0, MIN_HEIGHT).addChild(row));
    }

    private static UiNode iconFor(Toast.Type type) {
        return switch (type) {
            case SUCCESS -> new SpriteNode(ModUiTextures.CHECK.getId()).setTintResolver(UiTheme::success).size(ICON_SIZE).center();
            case ERROR -> new SpriteNode(ModUiTextures.CLOSE.getId()).setTintResolver(UiTheme::danger).size(ICON_SIZE).center();
            case WARNING -> new SpriteNode(ModUiTextures.WARNING.getId()).setTintResolver(UiTheme::warning).size(ICON_SIZE).center();
            case INFO -> new TextNode(() -> "ℹ").setBold(true).setColorResolver(UiTheme::info).size(ICON_SIZE).center();
        };
    }

    @Override
    public boolean hitTarget() {
        return true;
    }

    @Override
    public boolean onClick(float pointerX, float pointerY, int button) {
        dismissAction.run();
        return true;
    }
}
