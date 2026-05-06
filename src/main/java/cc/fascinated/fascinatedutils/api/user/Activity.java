package cc.fascinated.fascinatedutils.api.user;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.network.chat.Component;

@AllArgsConstructor @Getter
@Accessors(fluent = true)
public enum Activity {
    @SerializedName("in_main_menu") IN_MAIN_MENU(Component.translatable("alumite.social.user_status.in_main_menu").getString()),
    @SerializedName("in_server") IN_SERVER(Component.translatable("alumite.social.user_status.in_server").getString()),
    @SerializedName("in_singleplayer") IN_SINGLEPLAYER(Component.translatable("alumite.social.user_status.in_singleplayer").getString());

    private final String label;
}
