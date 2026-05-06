package cc.fascinated.fascinatedutils.api.user;

import com.google.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.network.chat.Component;

@AllArgsConstructor @Getter
@Accessors(fluent = true)
public enum UserStatus {
    @SerializedName("online") ONLINE(0xFF3BA55C, Component.translatable("alumite.social.user_status.online").getString()),
    @SerializedName("offline") OFFLINE(0xFF747F8D, Component.translatable("alumite.social.user_status.offline").getString()),
    @SerializedName("away") AWAY(0xFFF0B232, Component.translatable("alumite.social.user_status.away").getString()),
    @SerializedName("do_not_disturb") DO_NOT_DISTURB(0xFFF23F43, Component.translatable("alumite.social.user_status.do_not_disturb").getString()),
    @SerializedName("invisible") INVISIBLE(0xFF747F8D, Component.translatable("alumite.social.user_status.invisible").getString());

    private final int color;
    private final String label;
}
