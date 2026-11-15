package cc.fascinated.fascinatedutils.debug.perf.tests.chat;

import cc.fascinated.fascinatedutils.debug.perf.core.PerfTest;
import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.mixininterface.ChatComponentAccess;
import cc.fascinated.fascinatedutils.systems.chat.ChatSearch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * ChatSearch.apply with a new query followed by the chat's refresh of its visible lines, alternating between two queries
 * so every call changes the filter. The chat holds 100 messages, the most vanilla keeps (MAX_CHAT_HISTORY), and the
 * original message list and search state are restored afterwards. Messages are inserted directly so no chat module
 * transform or chat history write runs.
 */
public class ChatSearchPerfTest extends PerfTest {
    private static final int MESSAGE_COUNT = 100;
    private static final String[] QUERIES = {"diamond", "lava"};
    private static final String[] PLAYERS = {"Steve", "Alex", "Notch", "Herobrine", "Jeb_"};
    private static final String[] PHRASES = {
            "anyone trading diamonds?",
            "lava pool near spawn",
            "gg, that was close",
            "meet at the portal",
            "who has spare elytra?"
    };

    private ChatComponent chat;
    private List<GuiMessage> originalMessages;
    private int nextQuery;
    private int sink;

    public ChatSearchPerfTest() {
        super("chat.search", "ChatSearch.apply with a new query and the chat refresh over 100 messages", 10, 150);
    }

    @Override
    public void setup() {
        chat = Minecraft.getInstance().gui.getChat();
        ChatComponentAccess access = (ChatComponentAccess) chat;
        List<GuiMessage> messages = access.alumite$getAllMessages();
        originalMessages = List.copyOf(messages);
        messages.clear();
        for (int index = 0; index < MESSAGE_COUNT; index++) {
            String text = "<" + PLAYERS[index % PLAYERS.length] + "> " + PHRASES[index % PHRASES.length] + " #" + index;
            messages.add(new GuiMessage(0, Component.literal(text), null, GuiMessageSource.SYSTEM_CLIENT, GuiMessageTag.system()));
        }
        access.alumite$refreshTrimmedMessages();
    }

    @Override
    public void runOnce() {
        String query = QUERIES[nextQuery];
        nextQuery = (nextQuery + 1) % QUERIES.length;
        ChatSearch.INSTANCE.apply(chat, query);
        ((ChatComponentAccess) chat).alumite$refreshTrimmedMessages();
        sink += query.length();
    }

    @Override
    public void teardown() {
        if (chat == null) {
            return;
        }
        ChatComponentAccess access = (ChatComponentAccess) chat;
        List<GuiMessage> messages = access.alumite$getAllMessages();
        messages.clear();
        messages.addAll(originalMessages);
        ChatSearch.INSTANCE.apply(chat, "");
        access.alumite$refreshTrimmedMessages();
    }

    @Override
    public List<String> covers() {
        return List.of(PerfCoverage.system("chat"));
    }
}
