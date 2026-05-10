package cc.fascinated.fascinatedutils.api.user;

import cc.fascinated.fascinatedutils.client.ModUiTextures;
import com.google.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;

@AllArgsConstructor @Getter
@Accessors(fluent = true)
public enum UserStatus {
    @SerializedName("online") ONLINE(0xFF3BA55C, "alumite.social.user_status.online", ModUiTextures.STATUS_ONLINE.getId()),
    @SerializedName("offline") OFFLINE(0xFF747F8D, "alumite.social.user_status.offline", ModUiTextures.STATUS_INVISIBLE.getId()),
    @SerializedName("away") AWAY(0xFFF0B232, "alumite.social.user_status.away", ModUiTextures.STATUS_AWAY.getId()),
    @SerializedName("do_not_disturb") DO_NOT_DISTURB(0xFFF23F43, "alumite.social.user_status.do_not_disturb", ModUiTextures.STATUS_DO_NOT_DISTURB.getId()),
    @SerializedName("invisible") INVISIBLE(0xFF747F8D, "alumite.social.user_status.invisible", ModUiTextures.STATUS_INVISIBLE.getId());

    private final int color;
    private final String labelId;
    private final Identifier icon;

    public String label() {
        return I18n.get(labelId);
    }
}
