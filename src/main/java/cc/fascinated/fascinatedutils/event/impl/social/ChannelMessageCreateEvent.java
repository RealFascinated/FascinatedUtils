package cc.fascinated.fascinatedutils.event.impl.social;

import cc.fascinated.fascinatedutils.api.channel.Message;

public record ChannelMessageCreateEvent(String channelId, Message message) {}
