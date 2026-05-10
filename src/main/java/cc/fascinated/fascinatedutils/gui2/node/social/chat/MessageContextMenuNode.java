package cc.fascinated.fascinatedutils.gui2.node.social.chat;

import cc.fascinated.fascinatedutils.api.channel.Message;
import cc.fascinated.fascinatedutils.common.ClientUtils;
import cc.fascinated.fascinatedutils.gui2.core.UiState;
import cc.fascinated.fascinatedutils.gui2.node.ContextMenuNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Context menu shown when a user right-clicks a chat message.
 *
 * <p>Reads all required state from the provided store. Edit and Delete
 * are shown only for messages authored by the local player.
 */
public class MessageContextMenuNode extends ContextMenuNode {

    /**
     * @param message
     *         the message this menu acts on
     * @param isOwn
     *         whether the viewing user authored the message
     * @param onClose
     *         invoked to dismiss this menu
     * @param onEdit
     *         invoked when the user selects "Edit Message"; responsible for closing the menu
     *         and setting up edit state
     * @param msgPendingDelete
     *         state cell set to {@code message} when the user selects "Delete Message"
     */
    public MessageContextMenuNode(Message message, boolean isOwn, Runnable onClose,
                                  Runnable onEdit, UiState<Message> msgPendingDelete) {
        setOnClose(onClose);

        List<Item> items = new ArrayList<>();
        if (isOwn) {
            items.add(new Item("Edit Message", onEdit));
        }
        items.add(new Item("Copy Text", () -> {
            ClientUtils.copyToClipboard(message.content());
            onClose.run();
        }));
        if (isOwn) {
            items.add(Item.separator());
            items.add(new Item("Delete Message", theme -> theme.danger(), () -> {
                onClose.run();
                msgPendingDelete.set(message);
            }));
        }
        setItems(items);
    }
}
