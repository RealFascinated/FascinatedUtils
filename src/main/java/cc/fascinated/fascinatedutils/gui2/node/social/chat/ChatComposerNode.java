package cc.fascinated.fascinatedutils.gui2.node.social.chat;

import cc.fascinated.fascinatedutils.AlumiteMod;
import cc.fascinated.fascinatedutils.api.channel.Channel;
import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.core.UiState;
import cc.fascinated.fascinatedutils.gui2.core.UiStateStore;
import cc.fascinated.fascinatedutils.gui2.node.TextInputNode;

import java.nio.file.Path;

public class ChatComposerNode extends PositionedNode {

    private static final int ATTACH_SIZE = 30;
    private static final int ATTACH_GAP = 4;

    public ChatComposerNode(Channel channel, UiStateStore stateStore, String placeholder) {
        UiState<Path> pendingAttachment = stateStore.<Path>state("social.composer.attachment." + channel.id(), null);
        UiState<String> composerDraft = stateStore.state("social.composer.draft." + channel.id(), "");

        AttachButtonNode attachButton = new AttachButtonNode(pendingAttachment);
        attachButton.fullHeight().width(ATTACH_SIZE).left(0).top(0);
        addChild(attachButton);

        TextInputNode composer = new TextInputNode();
        composer.setNodeId("social.chat-composer");
        composer.setValue(composerDraft.get());
        composer.setPlaceholder(placeholder);
        composer.setOnChange(composerDraft::set);
        composer.setOnSubmit(text -> {
            String trimmedText = text.trim();
            Path attachment = pendingAttachment.get();
            if (trimmedText.isBlank() && attachment == null) {
                return;
            }
            composerDraft.set("");
            if (attachment != null) {
                pendingAttachment.set(null);
                AlumiteMod.SCHEDULED_POOL.execute(() -> {
                    try {
                        channel.sendMessage(trimmedText, attachment);
                    } catch (Exception ignored) {
                    }
                });
            } else {
                try {
                    channel.sendMessage(trimmedText);
                } catch (Exception ignored) {
                }
            }
        });
        composer.fullHeight().left(ATTACH_SIZE + ATTACH_GAP).right(0);
        addChild(composer);
    }
}