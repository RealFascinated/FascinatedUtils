package cc.fascinated.fascinatedutils.api.channel;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.AlumiteApiException;
import cc.fascinated.fascinatedutils.api.channel.json.ChannelDetailWire;
import cc.fascinated.fascinatedutils.api.channel.json.ChannelMessageWire;
import cc.fascinated.fascinatedutils.api.channel.json.ChannelSummaryWire;
import cc.fascinated.fascinatedutils.api.channel.json.OpenDmBodyWire;
import cc.fascinated.fascinatedutils.api.internal.AlumiteHttpClient;
import cc.fascinated.fascinatedutils.api.internal.AlumiteModelMapper;
import cc.fascinated.fascinatedutils.api.user.AlumiteUsers;
import cc.fascinated.fascinatedutils.api.user.User;
import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.social.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AlumiteChannels {

    private static final String ROUTE_CHANNELS = "/channels";

    private final Alumite alumite;
    private final AlumiteHttpClient http;
    private final AlumiteUsers users;
    private final Map<Integer, Channel> channelsById = new ConcurrentHashMap<>();
    private final Map<Integer, List<ChannelMessage>> messagesByChannel = new ConcurrentHashMap<>();
    private volatile List<Channel> channels = List.of();

    public AlumiteChannels(Alumite alumite, AlumiteHttpClient http, AlumiteUsers users) {
        this.alumite = alumite;
        this.http = http;
        this.users = users;
    }

    private static Instant parseInstantOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    public void clearSessionCaches() {
        channels = List.of();
        channelsById.clear();
        messagesByChannel.clear();
    }

    public List<Channel> all() {
        return channels;
    }

    public Channel get(int channelId) {
        return channelsById.get(channelId);
    }

    public List<ChannelMessage> messagesOrNull(int channelId) {
        return messagesByChannel.get(channelId);
    }

    public List<ChannelMessage> messages(int channelId) {
        List<ChannelMessage> list = messagesByChannel.get(channelId);
        return list == null ? List.of() : list;
    }

    public void replaceSummariesFromNetwork(List<ChannelSummaryWire> wireList) {
        if (wireList == null) {
            replaceSummaries(List.of());
            return;
        }
        List<ChannelListItem> mapped = wireList.stream().map(AlumiteModelMapper::toChannelSummary).toList();
        replaceSummaries(sortChannels(mapped));
    }

    public void preloadDetails() {
        List<Channel> snapshot = channels;
        for (Channel channel : snapshot) {
            channel.fetchDetail();
        }
    }

    public DmChannel openDmAndCache(int recipientUserId) throws AlumiteApiException {
        ChannelDetailWire wire = http.postObject(ROUTE_CHANNELS + "/dm", new OpenDmBodyWire(recipientUserId), ChannelDetailWire.class, "open dm", "Failed to open direct message.");
        return afterOpenDm(wire);
    }

    void cacheReadState(int channelId, int lastReadMessageId) {
        Channel channel = get(channelId);
        if (channel != null) {
            channel.applyLastReadMessageId(lastReadMessageId);
        }
    }

    void storeMessages(int channelId, List<ChannelMessage> messages) {
        messagesByChannel.put(channelId, List.copyOf(messages));
    }

    void cacheChannelDetail(ChannelDetailWire wire) {
        if (wire == null) {
            return;
        }
        if (wire instanceof ChannelDetailWire.DmChannelDetailWire dmDetailWire) {
            User recipient = users.upsertUser(AlumiteModelMapper.toUser(dmDetailWire.recipient()));
            getOrCreateDmChannel(dmDetailWire.id()).applyDetail(dmDetailWire.lastReadMessageId(), recipient);
            return;
        }
        if (wire instanceof ChannelDetailWire.GroupChannelDetailWire groupDetailWire) {
            List<GroupMember> members = groupDetailWire.members() == null ? List.of() : groupDetailWire.members().stream().map(AlumiteModelMapper::toGroupMember).map(member -> new GroupMember(users.upsertUser(member.user()), member.owner())).toList();
            int ownerUserId = groupDetailWire.ownerUserId() == null ? 0 : groupDetailWire.ownerUserId();
            getOrCreateGroupChannel(groupDetailWire.id()).applyDetail(groupDetailWire.lastReadMessageId(), groupDetailWire.name(), ownerUserId, members);
        }
    }

    void cacheSentMessage(int channelId, ChannelMessage message) {
        upsertChannelMessageFromSend(channelId, message);
    }

    public void onChannelCreate(ChannelSummaryWire summaryWire) {
        ChannelListItem summary = AlumiteModelMapper.toChannelSummary(summaryWire);
        if (summary == null) {
            return;
        }
        upsertChannelSummary(summary);
        Channel channel = get(summary.id());
        FascinatedEventBus.INSTANCE.post(new ChannelCreateEvent(channel == null ? summary : toChannelListItem(channel)));
    }

    public void onChannelRemove(int channelId) {
        removeChannel(channelId);
        messagesByChannel.remove(channelId);
        FascinatedEventBus.INSTANCE.post(new ChannelRemoveEvent(channelId));
    }

    public void onMessageCreate(int channelId, ChannelMessageWire messageWire) {
        ChannelMessage message = AlumiteModelMapper.toChannelMessage(messageWire);
        upsertChannelMessage(channelId, message);
        FascinatedEventBus.INSTANCE.post(new ChannelMessageCreateEvent(channelId, message));
        Channel channel = get(channelId);
        if (channel != null) {
            String authorName = users.previewAuthorName(message.authorId());
            upsertChannelSummary(new ChannelListItem(channel.id(), channel.kind(), channel.name(), message.createdAt(), new LastMessagePreview(message.id(), message.content(), authorName), channel.lastReadMessageId()));
        }
    }

    public void onMessageUpdate(int channelId, ChannelMessageWire messageWire) {
        ChannelMessage message = AlumiteModelMapper.toChannelMessage(messageWire);
        upsertChannelMessage(channelId, message);
        FascinatedEventBus.INSTANCE.post(new ChannelMessageUpdateEvent(channelId, message));
        Channel channel = get(channelId);
        if (channel == null) {
            return;
        }
        LastMessagePreview preview = channel.lastMessagePreview();
        if (preview != null && preview.messageId() == message.id()) {
            upsertChannelSummary(new ChannelListItem(channel.id(), channel.kind(), channel.name(), channel.lastMessageAt(), new LastMessagePreview(preview.messageId(), message.content(), preview.authorName()), channel.lastReadMessageId()));
        }
    }

    public void onMessageDelete(int channelId, int messageId) {
        List<ChannelMessage> existingMessages = messagesByChannel.get(channelId);
        if (existingMessages != null) {
            messagesByChannel.put(channelId, existingMessages.stream().filter(message -> message.id() != messageId).toList());
        }
        FascinatedEventBus.INSTANCE.post(new ChannelMessageDeleteEvent(channelId, messageId));
        Channel channel = get(channelId);
        if (channel == null) {
            return;
        }
        LastMessagePreview preview = channel.lastMessagePreview();
        if (preview == null || preview.messageId() != messageId) {
            return;
        }
        List<ChannelMessage> remaining = messagesByChannel.getOrDefault(channelId, List.of());
        if (remaining.isEmpty()) {
            upsertChannelSummary(new ChannelListItem(channel.id(), channel.kind(), channel.name(), null, null, channel.lastReadMessageId()));
        }
        else {
            ChannelMessage last = remaining.get(remaining.size() - 1);
            upsertChannelSummary(new ChannelListItem(channel.id(), channel.kind(), channel.name(), last.createdAt(), new LastMessagePreview(last.id(), last.content(), users.previewAuthorName(last.authorId())), channel.lastReadMessageId()));
        }
    }

    void hideChannelLocal(int channelId) {
        removeChannel(channelId);
        messagesByChannel.remove(channelId);
    }

    private DmChannel afterOpenDm(ChannelDetailWire detailWire) {
        cacheChannelDetail(detailWire);
        DmChannel channel = detailWire == null ? null : get(detailWire.id()) instanceof DmChannel dmChannel ? dmChannel : null;
        if (channel != null) {
            upsertChannelSummary(toChannelListItem(channel));
        }
        return channel;
    }

    private void upsertChannelMessageFromSend(int channelId, ChannelMessage message) {
        upsertChannelMessage(channelId, message);
        Channel channel = get(channelId);
        if (channel != null) {
            upsertChannelSummary(new ChannelListItem(channel.id(), channel.kind(), channel.name(), message.createdAt(), new LastMessagePreview(message.id(), message.content(), users.previewAuthorName(message.authorId())), channel.lastReadMessageId()));
        }
    }

    private void upsertChannelSummary(ChannelListItem summary) {
        List<ChannelListItem> updatedChannels = new ArrayList<>();
        boolean replacedExistingSummary = false;
        for (Channel existingChannel : channels) {
            ChannelListItem existingSummary = toChannelListItem(existingChannel);
            if (existingChannel.id() == summary.id()) {
                updatedChannels.add(summary);
                replacedExistingSummary = true;
            }
            else {
                updatedChannels.add(existingSummary);
            }
        }
        if (!replacedExistingSummary) {
            updatedChannels.add(summary);
        }
        replaceSummaries(sortChannels(updatedChannels));
    }

    private void upsertChannelMessage(int channelId, ChannelMessage message) {
        List<ChannelMessage> existingMessages = messagesByChannel.getOrDefault(channelId, List.of());
        List<ChannelMessage> updatedMessages = new ArrayList<>();
        boolean replacedExistingMessage = false;
        for (ChannelMessage existingMessage : existingMessages) {
            if (existingMessage.id() == message.id()) {
                updatedMessages.add(message);
                replacedExistingMessage = true;
            }
            else {
                updatedMessages.add(existingMessage);
            }
        }
        if (!replacedExistingMessage) {
            updatedMessages.add(message);
        }
        updatedMessages.sort(Comparator.comparingInt(ChannelMessage::id));
        messagesByChannel.put(channelId, List.copyOf(updatedMessages));
    }

    private List<ChannelListItem> sortChannels(List<ChannelListItem> sourceChannels) {
        return sourceChannels.stream().sorted((leftChannel, rightChannel) -> {
            Instant leftLastAt = parseInstantOrNull(leftChannel.lastMessageAt());
            Instant rightLastAt = parseInstantOrNull(rightChannel.lastMessageAt());
            if (leftLastAt == null && rightLastAt == null) {
                return Integer.compare(leftChannel.id(), rightChannel.id());
            }
            if (leftLastAt == null) {
                return 1;
            }
            if (rightLastAt == null) {
                return -1;
            }
            return rightLastAt.compareTo(leftLastAt);
        }).toList();
    }

    private DmChannel getOrCreateDmChannel(int channelId) {
        return (DmChannel) getOrCreateChannel(channelId, ChannelKind.DM);
    }

    private GroupChannel getOrCreateGroupChannel(int channelId) {
        return (GroupChannel) getOrCreateChannel(channelId, ChannelKind.GROUP);
    }

    private Channel getOrCreateChannel(int channelId, ChannelKind kind) {
        return channelsById.compute(channelId, (_, existingChannel) -> {
            if (existingChannel != null && existingChannel.kind() == kind) {
                return existingChannel;
            }
            return switch (kind) {
                case DM -> new DmChannel(alumite, channelId);
                case GROUP -> new GroupChannel(alumite, channelId);
            };
        });
    }

    private void replaceSummaries(List<ChannelListItem> updatedSummaries) {
        List<ChannelListItem> sortedSummaries = List.copyOf(updatedSummaries);
        Set<Integer> activeChannelIds = new HashSet<>();
        List<Channel> orderedChannels = new ArrayList<>(sortedSummaries.size());
        for (ChannelListItem summary : sortedSummaries) {
            Channel channel = getOrCreateChannel(summary.id(), summary.kind());
            channel.applySummary(summary);
            activeChannelIds.add(summary.id());
            orderedChannels.add(channel);
        }
        channelsById.keySet().removeIf(channelId -> !activeChannelIds.contains(channelId));
        messagesByChannel.keySet().removeIf(channelId -> !activeChannelIds.contains(channelId));
        channels = List.copyOf(orderedChannels);
    }

    private ChannelListItem toChannelListItem(Channel channel) {
        return new ChannelListItem(channel.id(), channel.kind(), channel.name(), channel.lastMessageAt(), channel.lastMessagePreview(), channel.lastReadMessageId());
    }

    private void removeChannel(int channelId) {
        List<ChannelListItem> remainingSummaries = channels.stream().filter(channel -> channel.id() != channelId).map(this::toChannelListItem).toList();
        replaceSummaries(remainingSummaries);
    }
}
