package cc.fascinated.fascinatedutils.api.dto;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public enum Presence {
    @SerializedName("online")
    ONLINE(0xFF3BA55C),
    @SerializedName("offline")
    OFFLINE(0xFF747F8D),
    @SerializedName("away")
    AWAY(0xFFF0B232),
    @SerializedName("invisible")
    INVISIBLE(0xFF747F8D);

    @Getter
    private final int color;

    Presence(int color) {
        this.color = color;
    }
}
