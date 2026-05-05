package cc.fascinated.fascinatedutils.event.impl.social;

import cc.fascinated.fascinatedutils.api.channel.ChannelMessage;

public record ChannelMessageCreateEvent(String channelId, ChannelMessage message) {}
