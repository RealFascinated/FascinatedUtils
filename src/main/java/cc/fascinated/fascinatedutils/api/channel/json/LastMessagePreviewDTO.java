package cc.fascinated.fascinatedutils.api.channel.json;

import java.util.List;

public record LastMessagePreviewDTO(String messageId, String content, String authorName, List<AttachmentDTO> attachments) {}
