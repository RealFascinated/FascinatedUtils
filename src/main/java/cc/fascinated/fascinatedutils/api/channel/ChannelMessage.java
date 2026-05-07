package cc.fascinated.fascinatedutils.api.channel;

import cc.fascinated.fascinatedutils.api.channel.json.AttachmentDTO;

import java.util.List;

public record ChannelMessage(String id, String channelId, String authorId, String content, String createdAt,
                              String editedAt, List<AttachmentDTO> attachments) {}
