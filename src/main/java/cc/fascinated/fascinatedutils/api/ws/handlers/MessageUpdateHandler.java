package cc.fascinated.fascinatedutils.api.ws.handlers;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.channel.json.ChannelMessageUpdatePayloadDTO;
import cc.fascinated.fascinatedutils.api.ws.GatewayHandler;
import cc.fascinated.fascinatedutils.api.ws.OutboundMessage;
import com.google.gson.JsonElement;

import java.util.function.Consumer;

public class MessageUpdateHandler implements GatewayHandler {

    @Override
    public void handle(Consumer<OutboundMessage> send, JsonElement data) {
        ChannelMessageUpdatePayloadDTO payload = Alumite.INSTANCE.getGsonForDTO().fromJson(data, ChannelMessageUpdatePayloadDTO.class);
        Alumite.INSTANCE.channels().onMessageUpdate(payload.channelId(), payload.message());
    }
}
