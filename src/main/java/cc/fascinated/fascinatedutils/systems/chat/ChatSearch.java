package cc.fascinated.fascinatedutils.systems.chat;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;

import java.util.Locale;
import java.util.function.Predicate;

public class ChatSearch {
    public static final ChatSearch INSTANCE = new ChatSearch();

    private final Predicate<GuiMessage> messageFilter = this::matches;

    @Getter
    private String query = "";

    /**
     * True while a new message is being added, so it is stored even when it does not match the query.
     */
    @Setter
    private boolean addingMessage;

    /**
     * Whether a non-empty search query is currently filtering chat.
     *
     * @return true when chat is being filtered
     */
    public boolean isActive() {
        return !query.isEmpty();
    }

    /**
     * Applies a new search query and refreshes the visible chat lines.
     *
     * @param chat      the chat component to filter
     * @param newQuery  the raw text typed into the search box
     */
    public void apply(ChatComponent chat, String newQuery) {
        String normalized = newQuery.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals(query)) {
            return;
        }
        query = normalized;
        chat.setVisibleMessageFilter(messageFilter);
    }

    private boolean matches(GuiMessage message) {
        return addingMessage || query.isEmpty() || message.content().getString().toLowerCase(Locale.ROOT).contains(query);
    }
}
