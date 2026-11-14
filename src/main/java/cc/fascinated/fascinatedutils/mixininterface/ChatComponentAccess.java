package cc.fascinated.fascinatedutils.mixininterface;

import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Accessor for private {@link net.minecraft.client.gui.components.ChatComponent} internals.
 */
public interface ChatComponentAccess {

    /**
     * Returns the live list of stored chat messages, newest first.
     *
     * @return the mutable message list
     */
    List<GuiMessage> alumite$getAllMessages();

    /**
     * Rebuilds the wrapped display lines from the stored messages.
     */
    void alumite$refreshTrimmedMessages();

    /**
     * Adds a message to chat without running the chat module transforms or history recording.
     *
     * @param contents the message to add
     */
    void alumite$addDirect(Component contents);
}
