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

    void alumite$refreshTrimmedMessages();

    /**
     * Removes the newest stored message and its wrapped lines from the visible chat, without re-wrapping older messages.
     */
    void alumite$removeNewestMessage();

    /**
     * Adds a message to chat without running the chat module transforms or history recording.
     *
     * @param contents the message to add
     */
    void alumite$addDirect(Component contents);
}
