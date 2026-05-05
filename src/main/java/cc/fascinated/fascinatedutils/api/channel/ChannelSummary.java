package cc.fascinated.fascinatedutils.api.channel;

public record ChannelSummary(int id, ChannelKind kind, String name, String lastMessageAt,
                             LastMessagePreview lastMessagePreview, Integer lastReadMessageId) {}
