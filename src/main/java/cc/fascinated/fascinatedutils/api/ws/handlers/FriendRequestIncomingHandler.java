package cc.fascinated.fascinatedutils.api.ws.handlers;

import cc.fascinated.fascinatedutils.Constants;
import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.friend.json.PendingFriendRequestDTO;
import cc.fascinated.fascinatedutils.api.ws.GatewayHandler;
import cc.fascinated.fascinatedutils.api.ws.OutboundMessage;
import com.google.gson.JsonElement;

import java.util.function.Consumer;

public class FriendRequestIncomingHandler implements GatewayHandler {

    @Override
    public void handle(Consumer<OutboundMessage> send, JsonElement data) {
        PendingFriendRequestDTO request = Constants.GSON.fromJson(data, PendingFriendRequestDTO.class);
        Alumite.INSTANCE.users().onFriendRequestIncoming(request);
    }
}
