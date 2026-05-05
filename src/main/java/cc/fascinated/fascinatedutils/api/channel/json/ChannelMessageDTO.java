package cc.fascinated.fascinatedutils.api.channel.json;

public record ChannelMessageDTO(int id, int channelId, int authorId, String content, String createdAt,
                                 String editedAt) {}
