package cc.fascinated.fascinatedutils.systems.config.serialization.impl;

import cc.fascinated.fascinatedutils.systems.config.ModulePersistentState;
import cc.fascinated.fascinatedutils.systems.config.serialization.JsonObjectWrapper;
import cc.fascinated.fascinatedutils.systems.config.serialization.Serializable;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

class ModulePersistentStateSerializer implements Serializable<ModulePersistentState> {

    private static JsonObject objectOrEmpty(JsonElement element) {
        return element != null && element.isJsonObject() ? element.getAsJsonObject().deepCopy() : new JsonObject();
    }

    @Override
    public JsonElement serializeToJson(ModulePersistentState object, Gson gson) {
        JsonObject root = new JsonObject();
        root.add("settings", object.settings().deepCopy());
        root.add("hud", object.hud().deepCopy());
        if (object.enabled() != null) {
            root.addProperty("enabled", object.enabled());
        }
        return root;
    }

    @Override
    public ModulePersistentState deserializeFromJson(JsonObjectWrapper data, Gson gson) {
        JsonObject settings = data.has("settings") ? objectOrEmpty(data.getMember("settings")) : new JsonObject();
        JsonObject hud = data.has("hud") ? objectOrEmpty(data.getMember("hud")) : new JsonObject();
        Boolean enabled = null;
        if (data.has("enabled")) {
            JsonElement enabledElement = data.getMember("enabled");
            if (enabledElement != null && enabledElement.isJsonPrimitive() && enabledElement.getAsJsonPrimitive().isBoolean()) {
                enabled = enabledElement.getAsBoolean();
            }
        }
        return new ModulePersistentState(settings, enabled, hud);
    }
}
