package cc.fascinated.fascinatedutils.api.channel.json;

import java.util.List;

public record ChannelMessageDTO(String id, String channelId, String authorId, String content, String createdAt,
                                 String editedAt, List<AttachmentDTO> attachments) {}
