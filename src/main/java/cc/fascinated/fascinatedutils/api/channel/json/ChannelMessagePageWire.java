package cc.fascinated.fascinatedutils.api.channel.json;

import java.util.List;

public record ChannelMessagePageWire(List<ChannelMessageWire> messages, boolean hasMore) {
}
