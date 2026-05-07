package cc.fascinated.fascinatedutils.api.channel;

import cc.fascinated.fascinatedutils.api.channel.json.AttachmentDTO;
import lombok.NonNull;

import java.util.List;

public record LastMessagePreview(String messageId, String content, String authorName, @NonNull List<AttachmentDTO> attachments) {}
