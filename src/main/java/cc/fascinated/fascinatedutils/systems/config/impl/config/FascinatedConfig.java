package cc.fascinated.fascinatedutils.systems.config.impl.config;

import cc.fascinated.fascinatedutils.systems.config.ConfigVersion;
import cc.fascinated.fascinatedutils.systems.config.GsonSerializable;
import cc.fascinated.fascinatedutils.systems.config.impl.settings.UIState;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.UUID;

@ConfigVersion(1)
public record FascinatedConfig(UUID activeProfileId, JsonObject globalSettings,
                               UIState uiState) implements GsonSerializable<FascinatedConfig> {

    public static FascinatedConfig defaults() {
        return new FascinatedConfig(null, new JsonObject(), UIState.defaults());
    }

    @Override
    public JsonElement serialize(Gson gson) {
        JsonObject root = new JsonObject();
        if (activeProfileId != null) {
            root.addProperty("active_profile_id", activeProfileId.toString());
        }
        root.add("settings", globalSettings != null ? globalSettings : new JsonObject());
        root.add("ui_state", uiState != null ? uiState.serialize(gson) : new JsonObject());
        return root;
    }

    @Override
    public FascinatedConfig deserialize(JsonElement data, Gson gson) {
        JsonObject root = data.getAsJsonObject();
        UUID deserializedActiveProfileId = null;
        if (root.has("active_profile_id")) {
            try {
                deserializedActiveProfileId = UUID.fromString(root.get("active_profile_id").getAsString());
            } catch (IllegalArgumentException ignored) {
            }
        }
        JsonElement settingsElement = root.get("settings");
        JsonObject deserializedGlobalSettings = settingsElement != null && settingsElement.isJsonObject() ? settingsElement.getAsJsonObject() : new JsonObject();
        JsonElement uiStateElement = root.get("ui_state");
        UIState deserializedUiState = uiStateElement != null && uiStateElement.isJsonObject() ? UIState.defaults().deserialize(uiStateElement, gson) : UIState.defaults();
        return new FascinatedConfig(deserializedActiveProfileId, deserializedGlobalSettings, deserializedUiState);
    }
}
