package cc.fascinated.fascinatedutils.debug.perf.tests.chat;

import cc.fascinated.fascinatedutils.debug.perf.core.PerfTest;
import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.mixininterface.ChatComponentAccess;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.chat.ChatModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * A repeated system message arriving over a full chat of 100 messages, through the real addMessage path so the chat
 * module stacks it onto the previous copy. Chat history saving is switched off for the run, and the original message
 * list and setting are restored afterwards.
 */
public class ChatDuplicateStackPerfTest extends PerfTest {
    private static final int MESSAGE_COUNT = 100;
    private static final Component REPEATED_MESSAGE = Component.literal("Set [Packet Flood] for perf_holder to 12");

    private ChatComponent chat;
    private List<GuiMessage> originalMessages;
    private boolean moduleEnabled;
    private boolean stackDuplicatesEnabled;
    private boolean saveHistoryEnabled;

    public ChatDuplicateStackPerfTest() {
        super("chat.stack_duplicate", "A duplicate system message stacked onto the previous one in a 100 message chat", 20, 500);
    }

    @Override
    public void setup() {
        ChatModule chatModule = ModuleRegistry.INSTANCE.getModule(ChatModule.class).orElseThrow();
        moduleEnabled = chatModule.isEnabled();
        stackDuplicatesEnabled = chatModule.getStackDuplicates().getValue();
        saveHistoryEnabled = chatModule.getSaveHistory().getValue();
        chatModule.setEnabled(true);
        chatModule.getStackDuplicates().setValue(true);
        chatModule.getSaveHistory().setValue(false);

        chat = Minecraft.getInstance().gui.getChat();
        ChatComponentAccess access = (ChatComponentAccess) chat;
        List<GuiMessage> messages = access.alumite$getAllMessages();
        originalMessages = List.copyOf(messages);
        messages.clear();
        for (int index = 0; index < MESSAGE_COUNT; index++) {
            Component text = Component.literal("<Steve> filler message number " + index + " with enough words to wrap once or twice in chat");
            messages.add(new GuiMessage(0, text, null, GuiMessageSource.SYSTEM_CLIENT, GuiMessageTag.system()));
        }
        access.alumite$refreshTrimmedMessages();
        chat.addClientSystemMessage(REPEATED_MESSAGE);
    }

    @Override
    public void runOnce() {
        chat.addClientSystemMessage(REPEATED_MESSAGE);
    }

    @Override
    public void teardown() {
        if (chat != null) {
            ChatComponentAccess access = (ChatComponentAccess) chat;
            List<GuiMessage> messages = access.alumite$getAllMessages();
            messages.clear();
            messages.addAll(originalMessages);
            access.alumite$refreshTrimmedMessages();
        }
        ModuleRegistry.INSTANCE.getModule(ChatModule.class).ifPresent(chatModule -> {
            chatModule.getStackDuplicates().setValue(stackDuplicatesEnabled);
            chatModule.getSaveHistory().setValue(saveHistoryEnabled);
            chatModule.setEnabled(moduleEnabled);
        });
    }

    @Override
    public List<String> covers() {
        return List.of(PerfCoverage.system("chat"));
    }
}
