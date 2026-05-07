package cc.fascinated.fascinatedutils.api.channel;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.AlumiteApiException;
import cc.fascinated.fascinatedutils.api.channel.json.*;
import cc.fascinated.fascinatedutils.api.internal.AlumiteModelMapper;
import cc.fascinated.fascinatedutils.client.Client;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract sealed class Channel permits DmChannel, GroupChannel {

    private final Alumite alumite;
    @Getter(AccessLevel.NONE)
    private final String channelId;
    private final ChannelKind kind;

    private volatile String name;
    private volatile String lastMessageAt;
    private volatile LastMessagePreview lastMessagePreview;
    private volatile String lastReadMessageId;

    public final String id() {
        return channelId;
    }

    public List<Message> messagesOrNull() {
        return alumite.channels().messagesOrNull(channelId);
    }

    public List<Message> messages() {
        return alumite.channels().messages(channelId);
    }

    public DmChannel asDmChannel() {
        return null;
    }

    public GroupChannel asGroupChannel() {
        return null;
    }

    public Message sendMessage(String content, Path... attachments) throws AlumiteApiException {
        List<String> attachmentIds = new ArrayList<>();
        for (Path path : attachments) {
            try {
                byte[] data = Files.readAllBytes(path);
                attachmentIds.add(uploadAttachment(data, path.getFileName().toString()).id());
            } catch (IOException exception) {
                throw new AlumiteApiException(null, "Failed to read attachment: " + path.getFileName());
            }
        }
        String messageContent = (content == null || content.isEmpty()) ? null : content;
        ChannelMessageDTO dto = alumite.sendChannelMessage(channelId, new SendChannelMessageBodyDTO(messageContent, attachmentIds.isEmpty() ? null : attachmentIds));
        Message message = AlumiteModelMapper.toChannelMessage(dto);
        alumite.channels().cacheSentMessage(channelId, message);
        return message;
    }

    public AttachmentDTO uploadAttachment(byte[] data, String filename) throws AlumiteApiException {
        return alumite.uploadChannelAttachment(channelId, data, filename);
    }

    public Message editMessage(String messageId, String content) throws AlumiteApiException {
        ChannelMessageDTO dto = alumite.editChannelMessage(channelId, messageId, new EditChannelMessageBodyDTO(content));
        alumite.channels().onMessageUpdate(channelId, dto);
        return AlumiteModelMapper.toChannelMessage(dto);
    }

    public void deleteMessage(String messageId) throws AlumiteApiException {
        alumite.deleteChannelMessage(channelId, messageId);
        alumite.channels().onMessageDelete(channelId, messageId);
    }

    public void markRead(String lastReadMessageId) throws AlumiteApiException {
        alumite.markChannelRead(channelId, lastReadMessageId);
        alumite.channels().cacheReadState(channelId, lastReadMessageId);
    }

    public void resolveMessages(int limit) throws AlumiteApiException {
        if (messagesOrNull() != null) {
            return;
        }
        ChannelMessagePageDTO page = alumite.getChannelMessages(channelId, Map.of("limit", limit));
        List<ChannelMessageDTO> dtoMessages = page.messages() == null ? List.of() : page.messages();
        List<Message> loaded = dtoMessages.stream().map(AlumiteModelMapper::toChannelMessage).sorted(Comparator.comparing(Message::createdAt)).toList();
        alumite.channels().storeMessages(channelId, loaded);
        alumite.channels().storeHasMore(channelId, page.hasMore());
    }

    public void fetchMessages(int limit) {
        try {
            resolveMessages(limit);
        } catch (AlumiteApiException exception) {
            Client.LOG.warn("[{}] fetch messages: {}", getClass().getSimpleName(), exception.getMessage());
        }
    }

    public boolean hasMoreMessages() {
        return alumite.channels().hasMoreMessages(channelId);
    }

    public ChannelMessagePageDTO fetchMessagesPage(int limit, String beforeMessageId, String afterMessageId) throws AlumiteApiException {
        Map<String, Object> queryParameters = new LinkedHashMap<>();
        if (limit > 0) {
            queryParameters.put("limit", limit);
        }
        if (beforeMessageId != null) {
            queryParameters.put("before", beforeMessageId);
        }
        if (afterMessageId != null) {
            queryParameters.put("after", afterMessageId);
        }
        ChannelMessagePageDTO page = alumite.getChannelMessages(channelId, queryParameters);
        if (page.messages() == null) {
            return new ChannelMessagePageDTO(List.of(), false);
        }
        return page;
    }

    public void mergeOlderMessagesPage(ChannelMessagePageDTO page) {
        List<Message> olderMessages = page.messages() == null ? List.of()
                : page.messages().stream().map(AlumiteModelMapper::toChannelMessage).toList();
        alumite.channels().storeHasMore(channelId, page.hasMore());
        if (olderMessages.isEmpty()) {
            return;
        }
        List<Message> existingMessages = messagesOrNull();
        List<Message> merged = new ArrayList<>(existingMessages == null ? List.of() : existingMessages);
        for (Message olderMessage : olderMessages) {
            boolean alreadyPresent = merged.stream().anyMatch(existingMessage -> existingMessage.id().equals(olderMessage.id()));
            if (!alreadyPresent) {
                merged.addFirst(olderMessage);
            }
        }
        merged.sort(Comparator.comparing(Message::createdAt));
        alumite.channels().storeMessages(channelId, merged);
    }

    void applyLastMessageAt(String lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    void applyLastMessagePreview(LastMessagePreview preview) {
        this.lastMessagePreview = preview;
    }

    void applyLastReadMessageId(String lastReadMessageId) {
        this.lastReadMessageId = lastReadMessageId;
    }

    void applyName(String name) {
        this.name = name;
    }

}
