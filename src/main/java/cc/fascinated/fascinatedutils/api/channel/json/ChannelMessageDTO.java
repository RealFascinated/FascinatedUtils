package cc.fascinated.fascinatedutils.api.channel.json;

public record ChannelMessageDTO(String id, String channelId, String authorId, String content, String createdAt,
                                 String editedAt) {}
