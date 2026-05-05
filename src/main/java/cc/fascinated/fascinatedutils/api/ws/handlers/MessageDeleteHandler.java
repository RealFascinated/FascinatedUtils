package cc.fascinated.fascinatedutils.api.ws.handlers;

import cc.fascinated.fascinatedutils.Constants;
import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.channel.json.ChannelMessageDeletePayloadDTO;
import cc.fascinated.fascinatedutils.api.ws.GatewayHandler;
import cc.fascinated.fascinatedutils.api.ws.OutboundMessage;
import com.google.gson.JsonElement;

import java.util.function.Consumer;

public class MessageDeleteHandler implements GatewayHandler {

    @Override
    public void handle(Consumer<OutboundMessage> send, JsonElement data) {
        ChannelMessageDeletePayloadDTO payload = Constants.GSON.fromJson(data, ChannelMessageDeletePayloadDTO.class);
        Alumite.INSTANCE.channels().onMessageDelete(payload.channelId(), payload.messageId());
    }
}
