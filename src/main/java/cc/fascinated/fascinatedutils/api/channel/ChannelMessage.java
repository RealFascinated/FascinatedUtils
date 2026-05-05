package cc.fascinated.fascinatedutils.api.channel;

public record ChannelMessage(int id, int channelId, int authorId, String content, String createdAt, String editedAt) {}
