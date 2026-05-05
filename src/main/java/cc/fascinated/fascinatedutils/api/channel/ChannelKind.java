package cc.fascinated.fascinatedutils.api.channel;

import com.google.gson.annotations.SerializedName;

public enum ChannelKind {
    @SerializedName("dm")
    DM,
    @SerializedName("group")
    GROUP
}
