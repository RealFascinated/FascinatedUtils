package cc.fascinated.fascinatedutils.api.channel;

public record ChannelMessage(String id, String channelId, String authorId, String content, String createdAt, String editedAt) {}
