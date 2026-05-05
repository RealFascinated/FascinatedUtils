package cc.fascinated.fascinatedutils.api.ws.handlers;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.channel.json.ChannelMessageCreatePayloadWire;
import cc.fascinated.fascinatedutils.api.ws.GatewayHandler;
import cc.fascinated.fascinatedutils.api.ws.OutboundMessage;
import com.google.gson.JsonElement;

import java.util.function.Consumer;

public class MessageCreateHandler implements GatewayHandler {

    @Override
    public void handle(Consumer<OutboundMessage> send, JsonElement data) {
        ChannelMessageCreatePayloadWire payload = Alumite.INSTANCE.getGsonForWire().fromJson(data, ChannelMessageCreatePayloadWire.class);
        Alumite.INSTANCE.channels().onMessageCreate(payload.channelId(), payload.message());
    }
}
