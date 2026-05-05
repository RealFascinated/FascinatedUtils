package cc.fascinated.fascinatedutils.api.channel;

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
    private final int channelId;
    private final ChannelKind kind;

    private volatile String name;
    private volatile String lastMessageAt;
    private volatile LastMessagePreview lastMessagePreview;
    private volatile Integer lastReadMessageId;

    public final int id() {
        return channelId;
    }

    public abstract boolean detailLoaded();

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
        ChannelMessageWire wire = alumite.http().postObject(route() + "/messages", new SendChannelMessageBodyWire(content), ChannelMessageWire.class, "send message", "Failed to send message.");
        ChannelMessage message = AlumiteModelMapper.toChannelMessage(wire);
        alumite.channels().cacheSentMessage(channelId, message);
        return message;
    }

    public void markRead(int lastReadMessageId) throws AlumiteApiException {
        alumite.http().sendAuthorizedChecked("PATCH", route() + "/read", alumite.getGsonForWire().toJson(new UpdateReadStateBodyWire(lastReadMessageId)), "update read state", "Failed to update channel read state.");
        alumite.channels().cacheReadState(channelId, lastReadMessageId);
    }

    public void resolveMessages(int limit) throws AlumiteApiException {
        if (messagesOrNull() != null) {
            return;
        }
        ChannelMessagePageWire page = alumite.http().getObject(UrlUtils.buildUrl(route() + "/messages", Map.of("limit", limit)), ChannelMessagePageWire.class, "get messages", "Failed to load messages.");
        List<ChannelMessageWire> wireMessages = page.messages() == null ? List.of() : page.messages();
        List<ChannelMessage> loaded = wireMessages.stream().map(AlumiteModelMapper::toChannelMessage).sorted(Comparator.comparingInt(ChannelMessage::id)).toList();
        alumite.channels().storeMessages(channelId, loaded);
    }

    public void fetchMessages(int limit) {
        try {
            resolveMessages(limit);
        } catch (AlumiteApiException exception) {
            Client.LOG.warn("[{}] fetch messages: {}", getClass().getSimpleName(), exception.getMessage());
        }
    }

    public List<ChannelMessage> fetchMessagesPage(int limit, Integer beforeMessageId, Integer afterMessageId) throws AlumiteApiException {
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
        ChannelMessagePageWire page = alumite.http().getObject(UrlUtils.buildUrl(route() + "/messages", queryParameters), ChannelMessagePageWire.class, "get messages", "Failed to load messages.");
        if (page.messages() == null) {
            return List.of();
        }
        return page.messages().stream().map(AlumiteModelMapper::toChannelMessage).toList();
    }

    public void resolveDetail() throws AlumiteApiException {
        if (detailLoaded()) {
            return;
        }
        ChannelDetailWire wire = alumite.http().getObject(route(), ChannelDetailWire.class, "get channel", "Failed to load channel.");
        alumite.channels().cacheChannelDetail(wire);
    }

    public void fetchDetail() {
        try {
            resolveDetail();
        } catch (AlumiteApiException exception) {
            Client.LOG.warn("[{}] fetch detail: {}", getClass().getSimpleName(), exception.getMessage());
        }
    }

    public void mergeOlderMessagesPage(List<ChannelMessage> olderMessages) {
        if (olderMessages == null || olderMessages.isEmpty()) {
            return;
        }
        List<ChannelMessage> existingMessages = messagesOrNull();
        List<ChannelMessage> merged = new ArrayList<>(existingMessages == null ? List.of() : existingMessages);
        for (ChannelMessage olderMessage : olderMessages) {
            boolean alreadyPresent = merged.stream().anyMatch(existingMessage -> existingMessage.id() == olderMessage.id());
            if (!alreadyPresent) {
                merged.addFirst(olderMessage);
            }
        }
        merged.sort(Comparator.comparingInt(ChannelMessage::id));
        alumite.channels().storeMessages(channelId, merged);
    }

    void applySummary(ChannelListItem summary) {
        if (summary == null || summary.kind() != kind) {
            return;
        }
        if (summary.name() != null || name == null) {
            name = summary.name();
        }
        lastMessageAt = summary.lastMessageAt();
        lastMessagePreview = summary.lastMessagePreview();
        lastReadMessageId = summary.lastReadMessageId();
    }

    void applyLastReadMessageId(Integer lastReadMessageId) {
        this.lastReadMessageId = lastReadMessageId;
    }

    void applyName(String name) {
        this.name = name;
    }

    private String route() {
        return "/channels/" + channelId;
    }
}
