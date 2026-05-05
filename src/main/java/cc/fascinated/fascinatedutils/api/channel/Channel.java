package cc.fascinated.fascinatedutils.api.channel;

import cc.fascinated.fascinatedutils.Constants;
import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.AlumiteApiException;
import cc.fascinated.fascinatedutils.api.channel.json.*;
import cc.fascinated.fascinatedutils.api.internal.AlumiteModelMapper;
import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.common.UrlUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

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

    public List<ChannelMessage> messagesOrNull() {
        return alumite.channels().messagesOrNull(channelId);
    }

    public List<ChannelMessage> messages() {
        return alumite.channels().messages(channelId);
    }

    public DmChannel asDmChannel() {
        return null;
    }

    public GroupChannel asGroupChannel() {
        return null;
    }

    public ChannelMessage sendMessage(String content) throws AlumiteApiException {
        ChannelMessageDTO dto = alumite.http().postObject(route() + "/messages", new SendChannelMessageBodyDTO(content), ChannelMessageDTO.class, "send message", "Failed to send message.");
        ChannelMessage message = AlumiteModelMapper.toChannelMessage(dto);
        alumite.channels().cacheSentMessage(channelId, message);
        return message;
    }

    public ChannelMessage editMessage(String messageId, String content) throws AlumiteApiException {
        ChannelMessageDTO dto = alumite.http().patchObject(route() + "/messages/" + messageId, new EditChannelMessageBodyDTO(content), ChannelMessageDTO.class, "edit message", "Failed to edit message.");
        alumite.channels().onMessageUpdate(channelId, dto);
        return AlumiteModelMapper.toChannelMessage(dto);
    }

    public void deleteMessage(String messageId) throws AlumiteApiException {
        alumite.http().sendAuthorizedExpectNoContent("DELETE", route() + "/messages/" + messageId, null, "delete message", "Failed to delete message.");
        alumite.channels().onMessageDelete(channelId, messageId);
    }

    public void markRead(String lastReadMessageId) throws AlumiteApiException {
        alumite.http().sendAuthorizedChecked("PATCH", route() + "/read", Constants.GSON.toJson(new UpdateReadStateBodyDTO(lastReadMessageId)), "update read state", "Failed to update channel read state.");
        alumite.channels().cacheReadState(channelId, lastReadMessageId);
    }

    public void resolveMessages(int limit) throws AlumiteApiException {
        if (messagesOrNull() != null) {
            return;
        }
        ChannelMessagePageDTO page = alumite.http().getObject(UrlUtils.buildUrl(route() + "/messages", Map.of("limit", limit)), ChannelMessagePageDTO.class, "get messages", "Failed to load messages.");
        List<ChannelMessageDTO> dtoMessages = page.messages() == null ? List.of() : page.messages();
        List<ChannelMessage> loaded = dtoMessages.stream().map(AlumiteModelMapper::toChannelMessage).sorted(Comparator.comparing(ChannelMessage::id)).toList();
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
        ChannelMessagePageDTO page = alumite.http().getObject(UrlUtils.buildUrl(route() + "/messages", queryParameters), ChannelMessagePageDTO.class, "get messages", "Failed to load messages.");
        if (page.messages() == null) {
            return new ChannelMessagePageDTO(List.of(), false);
        }
        return page;
    }

    public void mergeOlderMessagesPage(ChannelMessagePageDTO page) {
        List<ChannelMessage> olderMessages = page.messages() == null ? List.of()
                : page.messages().stream().map(AlumiteModelMapper::toChannelMessage).toList();
        alumite.channels().storeHasMore(channelId, page.hasMore());
        if (olderMessages.isEmpty()) {
            return;
        }
        List<ChannelMessage> existingMessages = messagesOrNull();
        List<ChannelMessage> merged = new ArrayList<>(existingMessages == null ? List.of() : existingMessages);
        for (ChannelMessage olderMessage : olderMessages) {
            boolean alreadyPresent = merged.stream().anyMatch(existingMessage -> existingMessage.id().equals(olderMessage.id()));
            if (!alreadyPresent) {
                merged.addFirst(olderMessage);
            }
        }
        merged.sort(Comparator.comparing(ChannelMessage::id));
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

    private String route() {
        return "/channels/" + channelId;
    }
}
