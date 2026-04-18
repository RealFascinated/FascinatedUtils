package cc.fascinated.fascinatedutils.event.impl.chat;

import cc.fascinated.fascinatedutils.event.CancellableFascinatedEvent;
import lombok.Getter;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.network.chat.Component;

@Getter
public class ChatMessageEvent extends CancellableFascinatedEvent {
    private final Component contents;
    private final GuiMessageSource source;
    private final String rawMessage;

    public ChatMessageEvent(Component contents, GuiMessageSource source) {
        this.contents = contents;
        this.source = source;
        this.rawMessage = contents.getString();
    }
}