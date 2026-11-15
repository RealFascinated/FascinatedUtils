package cc.fascinated.fascinatedutils.systems.chat;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Predicate;

public class ChatSearch {
    public static final ChatSearch INSTANCE = new ChatSearch();

    private static final int MAX_CACHED_MESSAGES = 512;

    private final Predicate<GuiMessage> messageFilter = this::matches;
    // Lowercased plain text per chat message, keyed by the message's component instance so each message is flattened once, not once per query.
    private final Map<Component, String> lowercaseTextCache = new IdentityHashMap<>();

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
        return addingMessage || query.isEmpty() || lowercaseText(message.content()).contains(query);
    }

    private String lowercaseText(Component content) {
        String cached = lowercaseTextCache.get(content);
        if (cached != null) {
            return cached;
        }
        if (lowercaseTextCache.size() >= MAX_CACHED_MESSAGES) {
            lowercaseTextCache.clear();
        }
        String lowercase = content.getString().toLowerCase(Locale.ROOT);
        lowercaseTextCache.put(content, lowercase);
        return lowercase;
    }
}
