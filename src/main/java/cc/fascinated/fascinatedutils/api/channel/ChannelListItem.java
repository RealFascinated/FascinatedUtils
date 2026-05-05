package cc.fascinated.fascinatedutils.api.channel;

public record ChannelListItem(int id, ChannelKind kind, String name, String lastMessageAt,
                             LastMessagePreview lastMessagePreview, Integer lastReadMessageId) {}
