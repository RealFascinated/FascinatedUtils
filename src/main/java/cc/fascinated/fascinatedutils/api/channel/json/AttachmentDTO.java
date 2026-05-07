package cc.fascinated.fascinatedutils.api.channel.json;

public record AttachmentDTO(String id, String name, String mimeType, int size, Integer width, Integer height,
                             String url) {}
