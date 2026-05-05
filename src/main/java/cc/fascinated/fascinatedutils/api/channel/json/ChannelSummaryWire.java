package cc.fascinated.fascinatedutils.api.channel.json;

public record ChannelSummaryWire(
        int id,
        ChannelKindWire type,
        String name,
        String lastMessageAt,
        LastMessagePreviewWire lastMessagePreview,
        Integer lastReadMessageId
) {
}
