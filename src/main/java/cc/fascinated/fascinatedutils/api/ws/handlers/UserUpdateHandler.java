package cc.fascinated.fascinatedutils.api.ws.handlers;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.user.User;
import cc.fascinated.fascinatedutils.api.user.json.PublicUserWire;
import cc.fascinated.fascinatedutils.api.ws.GatewayHandler;
import cc.fascinated.fascinatedutils.api.ws.OutboundMessage;
import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.social.UserUpdateEvent;
import com.google.gson.JsonElement;

import java.util.function.Consumer;

public class UserUpdateHandler implements GatewayHandler {

    @Override
    public void handle(Consumer<OutboundMessage> send, JsonElement data) {
        PublicUserWire user = Alumite.INSTANCE.getGsonForWire().fromJson(data, PublicUserWire.class);
        User updated = Alumite.INSTANCE.users().upsertUser(user);
        if (updated != null) {
            FascinatedEventBus.INSTANCE.post(new UserUpdateEvent(updated));
        }
    }
}
