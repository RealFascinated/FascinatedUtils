package cc.fascinated.fascinatedutils.api.ws.handlers;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.ws.GatewayHandler;
import cc.fascinated.fascinatedutils.api.ws.OutboundMessage;
import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.social.PresenceUpdateEvent;
import com.google.gson.JsonElement;

import java.util.function.Consumer;

public class PresenceUpdateHandler implements GatewayHandler {

    @Override
    public void handle(Consumer<OutboundMessage> send, JsonElement data) {
        PresenceUpdateEvent event = Alumite.INSTANCE.getGsonForWire().fromJson(data, PresenceUpdateEvent.class);
        Alumite.INSTANCE.users().mergePresenceUpdate(event.userId(), event.status());
        FascinatedEventBus.INSTANCE.post(event);
    }
}
