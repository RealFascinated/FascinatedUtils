package cc.fascinated.fascinatedutils.api.ws.handlers;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.friend.json.FriendEntryWire;
import cc.fascinated.fascinatedutils.api.ws.GatewayHandler;
import cc.fascinated.fascinatedutils.api.ws.OutboundMessage;
import com.google.gson.JsonElement;

import java.util.function.Consumer;

public class FriendAddHandler implements GatewayHandler {

    @Override
    public void handle(Consumer<OutboundMessage> send, JsonElement data) {
        FriendEntryWire entry = Alumite.INSTANCE.getGsonForWire().fromJson(data, FriendEntryWire.class);
        Alumite.INSTANCE.users().onFriendAdd(entry);
    }
}
