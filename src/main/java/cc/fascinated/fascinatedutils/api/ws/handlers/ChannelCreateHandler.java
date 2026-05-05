package cc.fascinated.fascinatedutils.api.ws.handlers;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.channel.json.ChannelCreatePayloadDTO;
import cc.fascinated.fascinatedutils.api.ws.GatewayHandler;
import cc.fascinated.fascinatedutils.api.ws.OutboundMessage;
import com.google.gson.JsonElement;

import java.util.function.Consumer;

public class ChannelCreateHandler implements GatewayHandler {

    @Override
    public void handle(Consumer<OutboundMessage> send, JsonElement data) {
        ChannelCreatePayloadDTO payload = Alumite.INSTANCE.getGsonForDTO().fromJson(data, ChannelCreatePayloadDTO.class);
        Alumite.INSTANCE.channels().onChannelCreate(payload.channel());
    }
}
