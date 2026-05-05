package cc.fascinated.fascinatedutils.api.channel.json;

import java.util.List;

public record ChannelMessagePageDTO(List<ChannelMessageDTO> messages, boolean hasMore) {}
