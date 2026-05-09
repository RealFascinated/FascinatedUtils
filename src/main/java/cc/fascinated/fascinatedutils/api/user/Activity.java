package cc.fascinated.fascinatedutils.api.user;

import com.google.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.client.resources.language.I18n;

@AllArgsConstructor @Getter
@Accessors(fluent = true)
public enum Activity {
    @SerializedName("in_main_menu") IN_MAIN_MENU("alumite.social.activity.in_main_menu"),
    @SerializedName("in_server") IN_SERVER("alumite.social.activity.in_server"),
    @SerializedName("in_singleplayer") IN_SINGLEPLAYER("alumite.social.activity.in_singleplayer");

    private final String labelId;

    public String label() {
        return I18n.get(labelId);
    }
}
