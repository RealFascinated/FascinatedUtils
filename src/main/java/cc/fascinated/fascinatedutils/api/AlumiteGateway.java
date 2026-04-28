package cc.fascinated.fascinatedutils.api;

import cc.fascinated.fascinatedutils.FascinatedUtils;
import cc.fascinated.fascinatedutils.api.dto.friend.FriendEntryDto;
import cc.fascinated.fascinatedutils.api.dto.friend.PendingFriendRequestDto;
import cc.fascinated.fascinatedutils.api.ws.GatewayHandler;
import cc.fascinated.fascinatedutils.api.ws.GatewayOpcode;
import cc.fascinated.fascinatedutils.api.ws.OutboundMessage;
import cc.fascinated.fascinatedutils.api.ws.impl.C2SAuthMessage;
import cc.fascinated.fascinatedutils.api.ws.impl.C2SHeartbeatAckMessage;
import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.common.AlumiteEnvironment;
import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.social.FriendAddEvent;
import cc.fascinated.fascinatedutils.event.impl.social.FriendRemoveEvent;
import cc.fascinated.fascinatedutils.event.impl.social.FriendRequestIncomingEvent;
import cc.fascinated.fascinatedutils.event.impl.social.FriendRequestRemovedEvent;
import cc.fascinated.fascinatedutils.event.impl.social.PresenceUpdateEvent;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

class AlumiteGateway implements WebSocket.Listener {

    private static final int CLOSE_AUTH_FAILURE = 4001;
    private static final int MAX_RECONNECT_DELAY_MS = 30_000;
    private static final Gson GSON = new Gson();

    private final HttpClient httpClient;
    private final Supplier<String> tokenSupplier;
    private final Runnable onAuthExpired;
    private final Map<GatewayOpcode, GatewayHandler> handlers = new EnumMap<>(GatewayOpcode.class);

    private volatile WebSocket ws;
    private volatile boolean running;
    private final StringBuilder messageBuffer = new StringBuilder();
    private int reconnectDelayMs = 1_000;

    AlumiteGateway(HttpClient httpClient, Supplier<String> tokenSupplier, Runnable onAuthExpired) {
        this.httpClient = httpClient;
        this.tokenSupplier = tokenSupplier;
        this.onAuthExpired = onAuthExpired;
        registerHandlers();
    }

    private void registerHandlers() {
        handlers.put(GatewayOpcode.HELLO, (send, data) -> {
            int heartbeatInterval = data.getAsJsonObject().get("heartbeatInterval").getAsInt();
            Client.LOG.info("[AlumiteGateway] HELLO received (heartbeatInterval={}ms), authenticating...", heartbeatInterval);
            send.accept(new C2SAuthMessage(tokenSupplier.get()));
        });
        handlers.put(GatewayOpcode.HEARTBEAT, (send, data) -> send.accept(new C2SHeartbeatAckMessage()));
        handlers.put(GatewayOpcode.AUTH_ACK, (send, data) -> {
            Client.LOG.info("[AlumiteGateway] Authenticated successfully.");
            reconnectDelayMs = 1_000;
        });
        handlers.put(GatewayOpcode.FRIEND_ADD, (send, data) -> {
            FriendEntryDto entry = GSON.fromJson(data, FriendEntryDto.class);
            FascinatedEventBus.INSTANCE.post(new FriendAddEvent(entry));
        });
        handlers.put(GatewayOpcode.FRIEND_REMOVE, (send, data) -> {
            int userId = data.getAsJsonObject().get("userId").getAsInt();
            FascinatedEventBus.INSTANCE.post(new FriendRemoveEvent(userId));
        });
        handlers.put(GatewayOpcode.FRIEND_REQUEST_INCOMING, (send, data) -> {
            PendingFriendRequestDto dto = GSON.fromJson(data, PendingFriendRequestDto.class);
            FascinatedEventBus.INSTANCE.post(new FriendRequestIncomingEvent(dto));
        });
        handlers.put(GatewayOpcode.FRIEND_REQUEST_REMOVED, (send, data) -> {
            FriendRequestRemovedEvent event = GSON.fromJson(data, FriendRequestRemovedEvent.class);
            FascinatedEventBus.INSTANCE.post(event);
        });
        handlers.put(GatewayOpcode.PRESENCE_UPDATE, (send, data) -> {
            PresenceUpdateEvent event = GSON.fromJson(data, PresenceUpdateEvent.class);
            FascinatedEventBus.INSTANCE.post(event);
        });
    }

    void connect() {
        running = true;
        reconnectDelayMs = 1_000;
        doConnect();
    }

    void disconnect() {
        running = false;
        WebSocket current = ws;
        if (current != null) {
            current.sendClose(WebSocket.NORMAL_CLOSURE, "").whenComplete((_, _) -> ws = null);
        }
    }

    void send(OutboundMessage message) {
        WebSocket current = ws;
        if (current != null) {
            current.sendText(message.toJson(), true);
        }
    }

    private void doConnect() {
        String token = tokenSupplier.get();
        if (token == null || token.isBlank()) {
            Client.LOG.warn("[AlumiteGateway] No access token available, skipping connect.");
            return;
        }
        httpClient.newWebSocketBuilder()
                .buildAsync(URI.create(AlumiteEnvironment.GATEWAY_URL + "/gateway"), this)
                .whenComplete((socket, throwable) -> {
                    if (throwable != null) {
                        Client.LOG.warn("[AlumiteGateway] Connection failed: {}", throwable.getMessage());
                        scheduleReconnect();
                    } else {
                        ws = socket;
                    }
                });
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        ws = webSocket;
        Client.LOG.info("[AlumiteGateway] Connection opened.");
        messageBuffer.setLength(0);
        webSocket.request(1);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        messageBuffer.append(data);
        if (last) {
            dispatch(messageBuffer.toString());
            messageBuffer.setLength(0);
        }
        webSocket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        ws = null;
        Client.LOG.info("[AlumiteGateway] Connection closed ({}).", statusCode);
        if (statusCode == CLOSE_AUTH_FAILURE) {
            running = false;
            onAuthExpired.run();
        } else if (running) {
            scheduleReconnect();
        }
        return null;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        ws = null;
        Client.LOG.warn("[AlumiteGateway] Error: {}", error.getMessage());
        if (running) {
            scheduleReconnect();
        }
    }

    private void dispatch(String raw) {
        try {
            JsonObject obj = GSON.fromJson(raw, JsonObject.class);
            GatewayOpcode opcode = GatewayOpcode.fromId(obj.get("op").getAsInt());
            if (opcode == null) {
                return;
            }
            GatewayHandler handler = handlers.get(opcode);
            if (handler == null) {
                return;
            }
            JsonElement data = obj.has("data") ? obj.get("data") : JsonNull.INSTANCE;
            handler.handle(this::send, data);
        } catch (Exception exception) {
            Client.LOG.warn("[AlumiteGateway] Failed to dispatch message: {}", exception.getMessage());
        }
    }

    private void scheduleReconnect() {
        int delay = reconnectDelayMs;
        reconnectDelayMs = Math.min(reconnectDelayMs * 2, MAX_RECONNECT_DELAY_MS);
        Client.LOG.info("[AlumiteGateway] Reconnecting in {}ms...", delay);
        FascinatedUtils.SCHEDULED_POOL.schedule(this::doConnect, delay, TimeUnit.MILLISECONDS);
    }
}
