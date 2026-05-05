package cc.fascinated.fascinatedutils.api.ws.handlers;

import cc.fascinated.fascinatedutils.api.ws.GatewayHandler;
import cc.fascinated.fascinatedutils.api.ws.OutboundMessage;
import cc.fascinated.fascinatedutils.api.ws.impl.C2SHeartbeatAckMessage;
import com.google.gson.JsonElement;

import java.util.function.Consumer;

public class HeartbeatHandler implements GatewayHandler {

    @Override
    public void handle(Consumer<OutboundMessage> send, JsonElement data) {
        send.accept(new C2SHeartbeatAckMessage());
    }
}
