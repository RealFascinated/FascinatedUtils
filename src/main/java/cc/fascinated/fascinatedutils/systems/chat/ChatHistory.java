package cc.fascinated.fascinatedutils.systems.chat;

import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.ClientTickEvent;
import cc.fascinated.fascinatedutils.event.impl.lifecycle.ClientStoppingEvent;
import cc.fascinated.fascinatedutils.mixininterface.ChatComponentAccess;
import cc.fascinated.fascinatedutils.systems.config.ModConfig;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.ChatModule;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ChatHistory {
    public static final ChatHistory INSTANCE = new ChatHistory();

    private static final int MAX_ENTRIES = 100;
    private static final int FLUSH_INTERVAL_TICKS = 200;

    private final List<Component> entries = new ArrayList<>();
    private ClientPacketListener lastConnection;
    private boolean dirty;
    private int ticksSinceFlush;

    /**
     * True while saved messages are being re-added to chat, so they are not transformed or recorded again.
     */
    @Getter
    private boolean replaying;

    private ChatHistory() {
        load();
        FascinatedEventBus.INSTANCE.subscribe(this);
    }

    /**
     * Records a finished chat message so it can be restored in a later session.
     *
     * @param message the message exactly as it was shown in chat
     */
    public void record(Component message) {
        entries.add(message);
        trim();
        dirty = true;
    }

    /**
     * Replaces the most recently recorded message, used when a duplicate is stacked.
     *
     * @param message the updated message
     */
    public void replaceLast(Component message) {
        if (entries.isEmpty()) {
            record(message);
            return;
        }
        entries.set(entries.size() - 1, message);
        dirty = true;
    }

    @EventHandler
    private void onClientTick(ClientTickEvent event) {
        replayIfNewConnection(event.minecraftClient());
        if (dirty && ++ticksSinceFlush >= FLUSH_INTERVAL_TICKS) {
            save();
        }
    }

    /**
     * Restores saved messages the first time it is called for a new server connection, so they appear before any message the server sends.
     *
     * @param minecraft the Minecraft client
     */
    public void replayIfNewConnection(Minecraft minecraft) {
        ClientPacketListener connection = minecraft.getConnection();
        if (connection == null) {
            lastConnection = null;
        } else if (connection != lastConnection) {
            lastConnection = connection;
            replay(minecraft);
        }
    }

    @EventHandler
    private void onClientStopping(ClientStoppingEvent event) {
        save();
    }

    private void replay(Minecraft minecraft) {
        Optional<ChatModule> moduleOptional = ModuleRegistry.INSTANCE.getModule(ChatModule.class);
        if (moduleOptional.isEmpty() || !moduleOptional.get().isEnabled() || !moduleOptional.get().getSaveHistory().isEnabled() || entries.isEmpty()) {
            return;
        }
        ChatComponentAccess chat = (ChatComponentAccess) minecraft.gui.getChat();
        List<Component> snapshot = List.copyOf(entries);
        replaying = true;
        try {
            for (Component entry : snapshot) {
                chat.alumite$addDirect(entry);
            }
            chat.alumite$addDirect(Component.translatable("alumite.module.chat.previous_session").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        } finally {
            replaying = false;
        }
    }

    private void trim() {
        while (entries.size() > MAX_ENTRIES) {
            entries.removeFirst();
        }
    }

    private static Path historyPath() {
        return ModConfig.getDirectory().resolve("chat_history.jsonl");
    }

    private void save() {
        dirty = false;
        ticksSinceFlush = 0;
        List<String> lines = new ArrayList<>(entries.size());
        for (Component entry : entries) {
            ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, entry).result().ifPresent(json -> lines.add(json.toString()));
        }
        try {
            Path path = historyPath();
            Files.createDirectories(path.getParent());
            Files.write(path, lines, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            Client.LOG.warn("Failed to save chat history: {}", exception.toString());
        }
    }

    private void load() {
        Path path = historyPath();
        if (!Files.exists(path)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                if (line.isBlank()) {
                    continue;
                }
                JsonElement json = JsonParser.parseString(line);
                ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, json).result().ifPresent(entries::add);
            }
            trim();
        } catch (IOException | RuntimeException exception) {
            Client.LOG.warn("Failed to load chat history: {}", exception.toString());
        }
    }
}
