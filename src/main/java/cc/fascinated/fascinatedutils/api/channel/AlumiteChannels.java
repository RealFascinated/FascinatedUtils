package cc.fascinated.fascinatedutils.api.channel;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.AlumiteApiException;
import cc.fascinated.fascinatedutils.api.channel.json.ChannelDetailDTO;
import cc.fascinated.fascinatedutils.api.channel.json.ChannelMessageDTO;
import cc.fascinated.fascinatedutils.api.channel.json.OpenDmBodyDTO;
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
    private final Map<String, Channel> channelsById = new ConcurrentHashMap<>();
    private final Map<String, List<ChannelMessage>> messagesByChannel = new ConcurrentHashMap<>();
    private final Map<String, Boolean> hasMoreByChannel = new ConcurrentHashMap<>();
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
        hasMoreByChannel.clear();
    }

    public List<Channel> all() {
        return channels;
    }

    public Channel get(String channelId) {
        return channelsById.get(channelId);
    }

    public List<ChannelMessage> messagesOrNull(String channelId) {
        return messagesByChannel.get(channelId);
    }

    public List<ChannelMessage> messages(String channelId) {
        List<ChannelMessage> list = messagesByChannel.get(channelId);
        return list == null ? List.of() : list;
    }

    public boolean hasMoreMessages(String channelId) {
        return Boolean.TRUE.equals(hasMoreByChannel.get(channelId));
    }

    public void replaceChannelsFromNetwork(List<ChannelDetailDTO> dtoList) {
        if (dtoList == null) {
            dtoList = List.of();
        }
        Set<String> activeIds = new HashSet<>();
        List<Channel> orderedChannels = new ArrayList<>(dtoList.size());
        for (ChannelDetailDTO dto : dtoList) {
            cacheChannelDetail(dto);
            Channel channel = channelsById.get(dto.id());
            if (channel != null) {
                activeIds.add(dto.id());
                orderedChannels.add(channel);
            }
        }
        channelsById.keySet().removeIf(channelId -> !activeIds.contains(channelId));
        messagesByChannel.keySet().removeIf(channelId -> !activeIds.contains(channelId));
        hasMoreByChannel.keySet().removeIf(channelId -> !activeIds.contains(channelId));
        channels = List.copyOf(orderedChannels);
    }

    public DmChannel openDmAndCache(String recipientUserId) throws AlumiteApiException {
        ChannelDetailDTO dto = http.postObject(ROUTE_CHANNELS + "/dm", new OpenDmBodyDTO(recipientUserId), ChannelDetailDTO.class, "open dm", "Failed to open direct message.");
        return afterOpenDm(dto);
    }

    void cacheReadState(String channelId, String lastReadMessageId) {
        Channel channel = get(channelId);
        if (channel != null) {
            channel.applyLastReadMessageId(lastReadMessageId);
        }
    }

    void storeMessages(String channelId, List<ChannelMessage> messages) {
        messagesByChannel.put(channelId, List.copyOf(messages));
    }

    void storeHasMore(String channelId, boolean hasMore) {
        hasMoreByChannel.put(channelId, hasMore);
    }

    void cacheChannelDetail(ChannelDetailDTO dto) {
        if (dto == null) {
            return;
        }
        LastMessagePreview preview = dto.lastMessagePreview() == null ? null
                : AlumiteModelMapper.toLastMessagePreview(dto.lastMessagePreview());
        if (dto instanceof ChannelDetailDTO.DmChannelDetailDTO dmDto) {
            User recipient = users.upsertUser(AlumiteModelMapper.toUser(dmDto.recipient()));
            getOrCreateDmChannel(dmDto.id()).applyDetail(dmDto.lastReadMessageId(), recipient, dmDto.lastMessageAt(), preview);
            return;
        }
        if (dto instanceof ChannelDetailDTO.GroupChannelDetailDTO groupDto) {
            List<GroupMember> members = groupDto.members() == null ? List.of() : groupDto.members().stream()
                    .map(AlumiteModelMapper::toGroupMember)
                    .map(member -> new GroupMember(users.upsertUser(member.user()), member.owner()))
                    .toList();
            getOrCreateGroupChannel(groupDto.id()).applyDetail(groupDto.lastReadMessageId(), groupDto.name(), groupDto.ownerUserId(), members, groupDto.lastMessageAt(), preview);
        }
    }

    void cacheSentMessage(String channelId, ChannelMessage message) {
        upsertChannelMessageFromSend(channelId, message);
    }

    public void onChannelCreate(ChannelDetailDTO dto) {
        if (dto == null) {
            return;
        }
        cacheChannelDetail(dto);
        if (!channelsById.containsKey(dto.id())) {
            return;
        }
        Channel channel = channelsById.get(dto.id());
        List<Channel> updated = new ArrayList<>(channels);
        if (updated.stream().noneMatch(existing -> existing.id().equals(dto.id()))) {
            updated.add(channel);
        }
        channels = List.copyOf(resortChannels(updated));
        FascinatedEventBus.INSTANCE.post(new ChannelCreateEvent(channel));
    }

    public void onChannelRemove(String channelId) {
        removeChannel(channelId);
        messagesByChannel.remove(channelId);
        FascinatedEventBus.INSTANCE.post(new ChannelRemoveEvent(channelId));
    }

    public void onMessageCreate(String channelId, ChannelMessageDTO messageDto) {
        ChannelMessage message = AlumiteModelMapper.toChannelMessage(messageDto);
        upsertChannelMessage(channelId, message);
        FascinatedEventBus.INSTANCE.post(new ChannelMessageCreateEvent(channelId, message));
        Channel channel = get(channelId);
        if (channel != null) {
            String authorName = users.previewAuthorName(message.authorId());
            applyChannelLastMessage(channel, message.createdAt(), new LastMessagePreview(message.id(), message.content(), authorName));
        }
    }

    public void onMessageUpdate(String channelId, ChannelMessageDTO messageDto) {
        ChannelMessage message = AlumiteModelMapper.toChannelMessage(messageDto);
        upsertChannelMessage(channelId, message);
        FascinatedEventBus.INSTANCE.post(new ChannelMessageUpdateEvent(channelId, message));
        Channel channel = get(channelId);
        if (channel == null) {
            return;
        }
        LastMessagePreview existingPreview = channel.lastMessagePreview();
        if (existingPreview != null && existingPreview.messageId().equals(message.id())) {
            applyChannelLastMessage(channel, channel.lastMessageAt(), new LastMessagePreview(existingPreview.messageId(), message.content(), existingPreview.authorName()));
        }
    }

    public void onMessageDelete(String channelId, String messageId) {
        List<ChannelMessage> existingMessages = messagesByChannel.get(channelId);
        if (existingMessages != null) {
            messagesByChannel.put(channelId, existingMessages.stream().filter(message -> !message.id().equals(messageId)).toList());
        }
        FascinatedEventBus.INSTANCE.post(new ChannelMessageDeleteEvent(channelId, messageId));
        Channel channel = get(channelId);
        if (channel == null) {
            return;
        }
        LastMessagePreview preview = channel.lastMessagePreview();
        if (preview == null || !preview.messageId().equals(messageId)) {
            return;
        }
        List<ChannelMessage> remaining = messagesByChannel.getOrDefault(channelId, List.of());
        if (remaining.isEmpty()) {
            applyChannelLastMessage(channel, null, null);
        } else {
            ChannelMessage last = remaining.get(remaining.size() - 1);
            applyChannelLastMessage(channel, last.createdAt(), new LastMessagePreview(last.id(), last.content(), users.previewAuthorName(last.authorId())));
        }
    }

    void hideChannelLocal(String channelId) {
        removeChannel(channelId);
        messagesByChannel.remove(channelId);
    }

    private DmChannel afterOpenDm(ChannelDetailDTO detailDto) {
        cacheChannelDetail(detailDto);
        if (detailDto == null) {
            return null;
        }
        Channel channel = channelsById.get(detailDto.id());
        if (channel instanceof DmChannel dmChannel) {
            if (channels.stream().noneMatch(existing -> existing.id() == detailDto.id())) {
                List<Channel> updated = new ArrayList<>(channels);
                updated.add(dmChannel);
                channels = List.copyOf(resortChannels(updated));
            }
            return dmChannel;
        }
        return null;
    }

    private void applyChannelLastMessage(Channel channel, String lastMessageAt, LastMessagePreview preview) {
        channel.applyLastMessageAt(lastMessageAt);
        channel.applyLastMessagePreview(preview);
        channels = List.copyOf(resortChannels(new ArrayList<>(channels)));
    }

    private void upsertChannelMessageFromSend(String channelId, ChannelMessage message) {
        upsertChannelMessage(channelId, message);
        Channel channel = get(channelId);
        if (channel != null) {
            String authorName = users.previewAuthorName(message.authorId());
            applyChannelLastMessage(channel, message.createdAt(), new LastMessagePreview(message.id(), message.content(), authorName));
        }
    }

    private void upsertChannelMessage(String channelId, ChannelMessage message) {
        List<ChannelMessage> existingMessages = messagesByChannel.getOrDefault(channelId, List.of());
        List<ChannelMessage> updatedMessages = new ArrayList<>();
        boolean replaced = false;
        for (ChannelMessage existingMessage : existingMessages) {
            if (existingMessage.id().equals(message.id())) {
                updatedMessages.add(message);
                replaced = true;
            } else {
                updatedMessages.add(existingMessage);
            }
        }
        if (!replaced) {
            updatedMessages.add(message);
        }
        updatedMessages.sort(Comparator.comparing(ChannelMessage::id));
        messagesByChannel.put(channelId, List.copyOf(updatedMessages));
    }

    private List<Channel> resortChannels(List<Channel> source) {
        return source.stream().sorted((left, right) -> {
            Instant leftAt = parseInstantOrNull(left.lastMessageAt());
            Instant rightAt = parseInstantOrNull(right.lastMessageAt());
            if (leftAt == null && rightAt == null) {
                return left.id().compareTo(right.id());
            }
            if (leftAt == null) {
                return 1;
            }
            if (rightAt == null) {
                return -1;
            }
            return rightAt.compareTo(leftAt);
        }).toList();
    }

    private DmChannel getOrCreateDmChannel(String channelId) {
        return (DmChannel) getOrCreateChannel(channelId, ChannelKind.DM);
    }

    private GroupChannel getOrCreateGroupChannel(String channelId) {
        return (GroupChannel) getOrCreateChannel(channelId, ChannelKind.GROUP);
    }

    private Channel getOrCreateChannel(String channelId, ChannelKind kind) {
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

    private void removeChannel(String channelId) {
        channelsById.remove(channelId);
        channels = List.copyOf(channels.stream().filter(channel -> !channel.id().equals(channelId)).toList());
    }
}
