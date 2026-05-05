package cc.fascinated.fascinatedutils.api.ws.handlers;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.friend.json.PendingFriendRequestWire;
import cc.fascinated.fascinatedutils.api.ws.GatewayHandler;
import cc.fascinated.fascinatedutils.api.ws.OutboundMessage;
import com.google.gson.JsonElement;

import java.util.function.Consumer;

public class FriendRequestIncomingHandler implements GatewayHandler {

    @Override
    public void handle(Consumer<OutboundMessage> send, JsonElement data) {
        PendingFriendRequestWire request = Alumite.INSTANCE.getGsonForWire().fromJson(data, PendingFriendRequestWire.class);
        Alumite.INSTANCE.users().onFriendRequestIncoming(request);
    }
}
