package cc.fascinated.fascinatedutils.api.ws.handlers;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.channel.json.ChannelMessageDeletePayloadWire;
import cc.fascinated.fascinatedutils.api.ws.GatewayHandler;
import cc.fascinated.fascinatedutils.api.ws.OutboundMessage;
import com.google.gson.JsonElement;

import java.util.function.Consumer;

public class MessageDeleteHandler implements GatewayHandler {

    @Override
    public void handle(Consumer<OutboundMessage> send, JsonElement data) {
        ChannelMessageDeletePayloadWire payload = Alumite.INSTANCE.getGsonForWire().fromJson(data, ChannelMessageDeletePayloadWire.class);
        Alumite.INSTANCE.channels().onMessageDelete(payload.channelId(), payload.messageId());
    }
}
