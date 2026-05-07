package cc.fascinated.fascinatedutils.event.impl.social;

import cc.fascinated.fascinatedutils.api.channel.Message;

public record ChannelMessageUpdateEvent(String channelId, Message message) {}
