package cc.fascinated.fascinatedutils.gui2.node.social.chat;

import cc.fascinated.fascinatedutils.AlumiteMod;
import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.AlumiteApiException;
import cc.fascinated.fascinatedutils.api.channel.Channel;
import cc.fascinated.fascinatedutils.api.channel.Message;
import cc.fascinated.fascinatedutils.gui2.core.ScrollColumnNode;
import cc.fascinated.fascinatedutils.gui2.core.UiState;
import cc.fascinated.fascinatedutils.gui2.core.UiStateStore;
import cc.fascinated.fascinatedutils.gui2.node.TextInputNode;
import cc.fascinated.fascinatedutils.gui2.node.TextNode;
import cc.fascinated.fascinatedutils.gui2.theme.UiThemeRepository;
import cc.fascinated.fascinatedutils.oldgui.toast.Toast;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import cc.fascinated.fascinatedutils.api.user.User;
import cc.fascinated.fascinatedutils.gui2.node.social.player.PlayerContextMenuHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ChatMessagesNode extends ScrollColumnNode {
    private static final int BOTTOM_SCROLL_ANCHOR = -1;
    private static final int SCROLL_EDGE_EPSILON = 2;

    private final Channel channel;
    private final PlayerContextMenuHandler playerContextMenuHandler;
    private final UiState<String> loadingMessagesChannelId;
    private final UiState<Map<String, Integer>> messageScrollByChannel;
    private final UiState<String> editingMessageId;
    private final UiState<String> editingDraft;
    private final UiState<Message> contextMenuMessage;
    private final UiState<Float> contextMenuX;
    private final UiState<Float> contextMenuY;
    private final UiState<Boolean> contextMenuIsOwn;

    public ChatMessagesNode(Channel channel, UiStateStore stateStore, PlayerContextMenuHandler playerContextMenuHandler) {
        this.channel = channel;
        this.playerContextMenuHandler = playerContextMenuHandler;
        this.loadingMessagesChannelId = stateStore.state("social.loadingMessagesChannel", null);
        this.messageScrollByChannel = stateStore.state("social.messageScrollByChannel", new HashMap<>());
        this.editingMessageId = stateStore.state("social.chat.editing.messageId", null);
        this.editingDraft = stateStore.state("social.chat.editing.draft", "");
        this.contextMenuMessage = stateStore.state("social.chat.contextMenu.message", null);
        this.contextMenuX = stateStore.state("social.chat.contextMenu.x", 0f);
        this.contextMenuY = stateStore.state("social.chat.contextMenu.y", 0f);
        this.contextMenuIsOwn = stateStore.state("social.chat.contextMenu.isOwn", false);
        setGap(2);

        configureScrollState();
        buildContent();
    }

    private void configureScrollState() {
        Integer savedOffset = messageScrollByChannel.get().get(channel.id());
        if (savedOffset == null || savedOffset == BOTTOM_SCROLL_ANCHOR) {
            setScrollOffset(Integer.MAX_VALUE);
        } else {
            setScrollOffset(savedOffset);
        }

        setOnScrollOffsetChanged(offset -> {
            int maxOffset = maxScrollOffset();
            if (maxOffset <= SCROLL_EDGE_EPSILON || offset >= maxOffset - SCROLL_EDGE_EPSILON) {
                messageScrollByChannel.get().put(channel.id(), BOTTOM_SCROLL_ANCHOR);
            } else {
                messageScrollByChannel.get().put(channel.id(), offset);
            }
        });
    }

    private void buildContent() {
        List<Message> messages = channel.messagesOrNull();
        if (messages == null) {
            triggerLoadMessages();
            addStatusRow("Loading messages...");
            return;
        }
        if (messages.isEmpty()) {
            addStatusRow("No messages yet");
            return;
        }
        boolean editingFound = false;
        for (Message message : messages) {
            ChatMessageNode messageNode = new ChatMessageNode(message);
            boolean isEditing = Objects.equals(editingMessageId.get(), message.id());
            if (isEditing) {
                editingFound = true;
                TextInputNode editInput = buildEditInput(message);
                messageNode.setEditInput(editInput);
            } else {
                boolean own = isOwnMessage(message);
                messageNode.setOnContextMenu((pointerX, pointerY) -> {
                    contextMenuMessage.set(message);
                    contextMenuX.set(pointerX);
                    contextMenuY.set(pointerY);
                    contextMenuIsOwn.set(own);
                });
            }
            if (playerContextMenuHandler != null) {
                messageNode.setOnAuthorClick((pointerX, pointerY) -> {
                    User author = Alumite.INSTANCE != null ? Alumite.INSTANCE.users().getUser(message.authorId()) : null;
                    playerContextMenuHandler.open(author, pointerX, pointerY);
                });
            }
            addChild(messageNode);
        }
        // Stale edit state (e.g. message not in channel) — clear it
        if (!editingFound && editingMessageId.get() != null) {
            editingMessageId.set(null);
            editingDraft.set("");
        }
    }

    private TextInputNode buildEditInput(Message message) {
        String draft = editingDraft.get();
        if (draft == null || draft.isEmpty()) {
            draft = message.content() != null ? message.content() : "";
        }
        TextInputNode editInput = new TextInputNode();
        editInput.setNodeId("social.chat.edit-input");
        editInput.setValue(draft);
        editInput.setOnChange(editingDraft::set);
        String initialContent = message.content() != null ? message.content() : "";
        editInput.setOnSubmit(text -> submitEdit(message, text.trim(), initialContent));
        editInput.setOnCancel(() -> {
            editingMessageId.set(null);
            editingDraft.set("");
        });
        return editInput;
    }

    private boolean isOwnMessage(Message message) {
        if (Alumite.INSTANCE == null) {
            return false;
        }
        String selfId = Alumite.INSTANCE.users().selfUser() != null
                ? Alumite.INSTANCE.users().selfUser().user().id()
                : null;
        return selfId != null && selfId.equals(message.authorId());
    }

    private void submitEdit(Message message, String newContent, String originalContent) {
        if (newContent.isEmpty() || newContent.equals(originalContent)) {
            editingMessageId.set(null);
            editingDraft.set("");
            return;
        }
        editingMessageId.set(null);
        editingDraft.set("");
        AlumiteMod.SCHEDULED_POOL.execute(() -> {
            try {
                channel.editMessage(message.id(), newContent);
            } catch (AlumiteApiException exception) {
                Toast.show().message(Component.translatable("alumite.social.error.generic").getString()).error();
            } catch (Exception exception) {
                Toast.show().message(Component.translatable("alumite.social.error.generic").getString()).error();
            }
        });
    }

    private void triggerLoadMessages() {
        if (loadingMessagesChannelId.get() != null && loadingMessagesChannelId.get().equals(channel.id())) {
            return;
        }
        loadingMessagesChannelId.set(channel.id());
        AlumiteMod.SCHEDULED_POOL.execute(() -> {
            channel.fetchMessages(50);
            Minecraft.getInstance().execute(() -> loadingMessagesChannelId.set(null));
        });
    }

    private void addStatusRow(String text) {
        TextNode emptyLabel = new TextNode(text).setColorArgb(UiThemeRepository.get().textSubtle());
        emptyLabel.fullWidth().height(40);
        addChild(emptyLabel);
    }
}