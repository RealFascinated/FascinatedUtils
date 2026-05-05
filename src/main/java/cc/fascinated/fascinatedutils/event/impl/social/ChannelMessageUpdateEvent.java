package cc.fascinated.fascinatedutils.event.impl.social;

import cc.fascinated.fascinatedutils.api.channel.ChannelMessage;

public record ChannelMessageUpdateEvent(int channelId, ChannelMessage message) {
}
