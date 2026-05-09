package cc.fascinated.fascinatedutils.gui2.node.social.chat;

import cc.fascinated.fascinatedutils.api.channel.Message;
import cc.fascinated.fascinatedutils.common.ClientUtils;
import cc.fascinated.fascinatedutils.gui2.core.UiState;
import cc.fascinated.fascinatedutils.gui2.core.UiStateStore;
import cc.fascinated.fascinatedutils.gui2.node.ContextMenuNode;

import java.util.ArrayList;
import java.util.List;

public class MessageContextMenuNode extends ContextMenuNode {

    public MessageContextMenuNode(UiStateStore stateStore) {
        UiState<Message> contextMenuMessage = stateStore.state("social.chat.contextMenu.message", null);
        UiState<Float> contextMenuX = stateStore.state("social.chat.contextMenu.x", 0f);
        UiState<Float> contextMenuY = stateStore.state("social.chat.contextMenu.y", 0f);
        UiState<Boolean> contextMenuIsOwn = stateStore.state("social.chat.contextMenu.isOwn", false);
        UiState<String> editingMessageId = stateStore.state("social.chat.editing.messageId", null);
        UiState<String> editingDraft = stateStore.state("social.chat.editing.draft", "");
        UiState<Message> msgPendingDelete = stateStore.state("social.chat.delete.message", null);

        Message message = contextMenuMessage.get();
        boolean isOwn = Boolean.TRUE.equals(contextMenuIsOwn.get());

        setOnClose(() -> contextMenuMessage.set(null));
        setPreferredPosition(Math.round(contextMenuX.get()), Math.round(contextMenuY.get()));

        List<Item> items = new ArrayList<>();
        if (isOwn) {
            items.add(new Item("Edit Message", () -> {
                contextMenuMessage.set(null);
                editingDraft.set(message.content() != null ? message.content() : "");
                editingMessageId.set(message.id());
                stateStore.requestFocus("social.chat.edit-input");
            }));
        }
        items.add(new Item("Copy Text", () -> {
            ClientUtils.copyToClipboard(message.content());
            contextMenuMessage.set(null);
        }));
        if (isOwn) {
            items.add(new Item("Delete Message", theme -> theme.danger(), () -> {
                contextMenuMessage.set(null);
                msgPendingDelete.set(message);
            }));
        }
        setItems(items);
    }
}
