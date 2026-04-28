package cc.fascinated.fascinatedutils.api.ws;

public enum GatewayOpcode {
    // Server -> Client
    HELLO(0),
    HEARTBEAT(1),
    AUTH_ACK(2),
    FRIEND_REQUEST_INCOMING(3),
    FRIEND_ADD(4),
    FRIEND_REMOVE(5),
    PRESENCE_UPDATE(6),
    FRIEND_REQUEST_REMOVED(7),

    // Client -> Server
    HEARTBEAT_ACK(1000),
    AUTH(1001);

    public final int id;

    GatewayOpcode(int id) {
        this.id = id;
    }

    public static GatewayOpcode fromId(int id) {
        for (GatewayOpcode opcode : values()) {
            if (opcode.id == id) {
                return opcode;
            }
        }
        return null;
    }
}
