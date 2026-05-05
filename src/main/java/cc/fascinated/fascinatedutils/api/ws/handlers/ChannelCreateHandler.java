package cc.fascinated.fascinatedutils.api.ws.handlers;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.channel.json.ChannelCreatePayloadWire;
import cc.fascinated.fascinatedutils.api.ws.GatewayHandler;
import cc.fascinated.fascinatedutils.api.ws.OutboundMessage;
import com.google.gson.JsonElement;

import java.util.function.Consumer;

public class ChannelCreateHandler implements GatewayHandler {

    @Override
    public void handle(Consumer<OutboundMessage> send, JsonElement data) {
        ChannelCreatePayloadWire payload = Alumite.INSTANCE.getGsonForWire().fromJson(data, ChannelCreatePayloadWire.class);
        Alumite.INSTANCE.channels().onChannelCreate(payload.summary());
    }
}
