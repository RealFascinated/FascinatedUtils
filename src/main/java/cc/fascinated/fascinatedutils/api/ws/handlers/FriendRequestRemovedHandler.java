package cc.fascinated.fascinatedutils.api.ws.handlers;

import cc.fascinated.fascinatedutils.Constants;
import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.ws.GatewayHandler;
import cc.fascinated.fascinatedutils.api.ws.OutboundMessage;
import cc.fascinated.fascinatedutils.event.impl.social.FriendRequestRemovedEvent;
import com.google.gson.JsonElement;

import java.util.function.Consumer;

public class FriendRequestRemovedHandler implements GatewayHandler {

    @Override
    public void handle(Consumer<OutboundMessage> send, JsonElement data) {
        FriendRequestRemovedEvent event = Constants.GSON.fromJson(data, FriendRequestRemovedEvent.class);
        Alumite.INSTANCE.users().onFriendRequestRemoved(event.requestId(), event.reason());
    }
}
