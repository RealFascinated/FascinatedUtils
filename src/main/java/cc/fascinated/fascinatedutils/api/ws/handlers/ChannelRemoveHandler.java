package cc.fascinated.fascinatedutils.api.ws.handlers;

import cc.fascinated.fascinatedutils.Constants;
import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.channel.json.ChannelRemovePayloadDTO;
import cc.fascinated.fascinatedutils.api.ws.GatewayHandler;
import cc.fascinated.fascinatedutils.api.ws.OutboundMessage;
import com.google.gson.JsonElement;

import java.util.function.Consumer;

public class ChannelRemoveHandler implements GatewayHandler {

    @Override
    public void handle(Consumer<OutboundMessage> send, JsonElement data) {
        ChannelRemovePayloadDTO payload = Constants.GSON.fromJson(data, ChannelRemovePayloadDTO.class);
        Alumite.INSTANCE.channels().onChannelRemove(payload.channelId());
    }
}
