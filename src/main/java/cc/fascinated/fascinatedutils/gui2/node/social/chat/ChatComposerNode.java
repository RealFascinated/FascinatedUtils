package cc.fascinated.fascinatedutils.gui2.node.social.chat;

import cc.fascinated.fascinatedutils.Constants;
import cc.fascinated.fascinatedutils.api.channel.Channel;
import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.core.UiState;
import cc.fascinated.fascinatedutils.gui2.core.UiStateStore;
import cc.fascinated.fascinatedutils.gui2.node.input.TextboxInputNode;
import cc.fascinated.fascinatedutils.gui2.node.input.TextParser;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;
import net.minecraft.client.Minecraft;

import java.nio.file.Path;
import java.util.UUID;

public class ChatComposerNode extends PositionedNode<ChatComposerNode> {

    private static final int ATTACH_GAP = 4;
    private static final int ATTACH_INSET_X = 4;
    private static final int PREVIEW_GAP = 4;

    private final UiState<Path> pendingAttachment;
    private final PendingAttachmentPreviewNode attachmentPreview;
    private final AttachButtonNode attachButton;
    private final TextboxInputNode<String> composerInput;

    public ChatComposerNode(Channel channel, UiStateStore stateStore, String placeholder) {
        UiState<Path> pendingAttachment = stateStore.<Path>state("social.composer.attachment." + channel.id(), null);
        this.pendingAttachment = pendingAttachment;
        UiState<String> composerDraft = stateStore.state("social.composer.draft." + channel.id(), "");
        UiState<Integer> composerCaret = stateStore.state("social.composer.caret." + channel.id(), Integer.MAX_VALUE);
        UiState<Integer> composerSelection = stateStore.state("social.composer.selection." + channel.id(), -1);
        UiState<Boolean> composerDrag = stateStore.state("social.composer.drag." + channel.id(), false);

        attachmentPreview = new PendingAttachmentPreviewNode(pendingAttachment);
        addChild(attachmentPreview);

        attachButton = new AttachButtonNode(pendingAttachment);

        composerInput = new TextboxInputNode<>(TextParser.STRING);
        composerInput.setNodeId("social.chat-composer");
        composerInput.setValue(composerDraft.get());
        composerInput.bindCaretState(composerCaret);
        composerInput.bindSelectionState(composerSelection);
        composerInput.bindDragState(composerDrag);
        composerInput.setPlaceholder(placeholder);
        composerInput.setOnChange(composerDraft::set);
        composerInput.setOnSubmit(text -> {
            String trimmedText = text.trim();
            Path attachment = pendingAttachment.get();
            if (trimmedText.isBlank() && attachment == null) {
                return;
            }
            composerDraft.set("");
            String nonce = UUID.randomUUID().toString();
            channel.insertOptimisticMessage(nonce, trimmedText.isBlank() ? null : trimmedText);
            if (attachment != null) {
                pendingAttachment.set(null);
                Path captured = attachment;
                Constants.EXECUTORS.execute(() -> {
                    try {
                        var real = channel.sendMessage(trimmedText, captured);
                        Minecraft.getInstance().execute(() -> channel.confirmOptimisticSend(nonce, real));
                    } catch (Exception ignored) {
                        Minecraft.getInstance().execute(() -> channel.removeOptimisticMessage(nonce));
                    }
                });
            } else {
                Constants.EXECUTORS.execute(() -> {
                    try {
                        var real = channel.sendMessage(trimmedText);
                        Minecraft.getInstance().execute(() -> channel.confirmOptimisticSend(nonce, real));
                    } catch (Exception ignored) {
                        Minecraft.getInstance().execute(() -> channel.removeOptimisticMessage(nonce));
                    }
                });
            }
        });
        addChild(composerInput);
        addChild(attachButton);
    }

    /**
     * Returns the preferred height of this composer for the given available width.
     *
     * @param frame          render frame used for font measurement
     * @param availableWidth total available width in pixels
     * @return preferred height in pixels
     */
    public int preferredHeight(RenderFrame frame, int availableWidth) {
        int btnSize = frame.fontHeight() + 10;
        composerInput.setLeftInset(ATTACH_INSET_X + btnSize + ATTACH_GAP);
        int height = composerInput.preferredHeight(frame, availableWidth);
        if (pendingAttachment.get() != null) {
            height += PendingAttachmentPreviewNode.HEIGHT + PREVIEW_GAP;
        }
        return height;
    }

    @Override
    public void layout(RenderFrame frame, int parentX, int parentY, int parentWidth, int parentHeight) {
        int btnSize = frame.fontHeight() + 10;
        composerInput.setLeftInset(ATTACH_INSET_X + btnSize + ATTACH_GAP);

        boolean hasPreview = pendingAttachment.get() != null;
        int previewHeight = hasPreview ? PendingAttachmentPreviewNode.HEIGHT + PREVIEW_GAP : 0;
        int inputHeight = composerInput.preferredHeight(frame, parentWidth);
        int myHeight = previewHeight + inputHeight;
        bounds().set(parentX, parentY, parentWidth, myHeight);

        if (hasPreview) {
            attachmentPreview.layout(frame, parentX, parentY, parentWidth, PendingAttachmentPreviewNode.HEIGHT);
        }

        int inputY = parentY + previewHeight;
        int minH = composerInput.minHeight(frame);
        int btnInsetY = (minH - btnSize) / 2;
        attachButton.size(btnSize);
        attachButton.layout(frame, parentX + ATTACH_INSET_X, inputY + btnInsetY, btnSize, btnSize);
        composerInput.layout(frame, parentX, inputY, parentWidth, inputHeight);
    }
}