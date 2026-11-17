package cc.fascinated.fascinatedutils.event.impl.chat;

import cc.fascinated.fascinatedutils.event.CancellableFascinatedEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.network.chat.Component;

@Getter
@RequiredArgsConstructor
public class ChatMessageEvent extends CancellableFascinatedEvent {
    private final Component contents;
    private final GuiMessageSource source;
    private String rawMessage;

    /**
     * Returns the plain text of the message, flattening the component on first access only.
     *
     * @return the message contents without any formatting
     */
    public String getRawMessage() {
        if (rawMessage == null) {
            rawMessage = contents.getString();
        }
        return rawMessage;
    }
}