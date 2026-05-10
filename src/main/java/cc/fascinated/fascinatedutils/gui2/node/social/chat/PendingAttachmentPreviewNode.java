package cc.fascinated.fascinatedutils.gui2.node.social.chat;

import cc.fascinated.fascinatedutils.caches.UrlTextureCache;
import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.core.UiState;
import cc.fascinated.fascinatedutils.gui2.node.ButtonNode;
import cc.fascinated.fascinatedutils.gui2.node.CardNode;
import cc.fascinated.fascinatedutils.gui2.node.ImageNode;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;

import java.nio.file.Path;

class PendingAttachmentPreviewNode extends PositionedNode {

    private static final int THUMB_SIZE = 100;
    private static final int PAD = 6;
    private static final int CORNER_RADIUS = 4;
    private static final int CLOSE_BTN_SIZE = 16;
    static final int HEIGHT = THUMB_SIZE + PAD * 2;

    private final UiState<Path> pendingAttachment;

    PendingAttachmentPreviewNode(UiState<Path> pendingAttachment) {
        this.pendingAttachment = pendingAttachment;
        fullWidth().height(HEIGHT);

        addChild(new CardNode()
                .setCornerRadius(CORNER_RADIUS)
                .setNoBorder()
                .setFillResolver(theme -> theme.attachmentPlaceholderFill())
                .left(PAD).top(PAD).size(THUMB_SIZE, THUMB_SIZE));

        addChild(new ImageNode()
                .setTextureSupplier(() -> {
                    Path path = pendingAttachment.get();
                    return path != null ? UrlTextureCache.INSTANCE.getLocal(path, null) : null;
                })
                .setNaturalSizeSupplier(() -> {
                    Path path = pendingAttachment.get();
                    return path != null ? UrlTextureCache.INSTANCE.getLocalSizePixels(path) : null;
                })
                .setCornerRadius(CORNER_RADIUS)
                .left(PAD).top(PAD).size(THUMB_SIZE, THUMB_SIZE));

        addChild(new ButtonNode("\u00D7")
                .setVariant(ButtonNode.ButtonVariant.GHOST)
                .setLabelColorResolver(theme -> theme.textMuted())
                .setOnPress(() -> pendingAttachment.set(null))
                .left(PAD + THUMB_SIZE - CLOSE_BTN_SIZE).top(PAD).size(CLOSE_BTN_SIZE, CLOSE_BTN_SIZE));
    }

    @Override
    public boolean blocksHitWhenEmpty() {
        return true;
    }

    @Override
    public void render(RenderFrame frame, float deltaSeconds) {
        if (pendingAttachment.get() == null) {
            return;
        }
        super.render(frame, deltaSeconds);
    }
}