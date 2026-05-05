package cc.fascinated.fascinatedutils.api.ws.handlers;

import cc.fascinated.fascinatedutils.api.ws.GatewayHandler;
import cc.fascinated.fascinatedutils.api.ws.OutboundMessage;
import cc.fascinated.fascinatedutils.client.Client;
import com.google.gson.JsonElement;
import lombok.RequiredArgsConstructor;

import java.util.function.Consumer;

@RequiredArgsConstructor
public class AuthAckHandler implements GatewayHandler {

    private final Runnable onAuthenticated;

    @Override
    public void handle(Consumer<OutboundMessage> send, JsonElement data) {
        Client.LOG.info("[AlumiteGateway] Authenticated successfully.");
        onAuthenticated.run();
    }
}
