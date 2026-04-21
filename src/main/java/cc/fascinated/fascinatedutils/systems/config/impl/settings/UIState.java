package cc.fascinated.fascinatedutils.systems.config.impl.settings;

import cc.fascinated.fascinatedutils.systems.config.GsonSerializable;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public record UIState(String lastShellContentTabKey) implements GsonSerializable<UIState> {

    private static final String LAST_SHELL_CONTENT_TAB_KEY_FIELD = "last_shell_content_tab";

    public static UIState defaults() {
        return new UIState(null);
    }

    @Override
    public JsonElement serialize(Gson gson) {
        JsonObject root = new JsonObject();
        if (lastShellContentTabKey != null) {
            root.addProperty(LAST_SHELL_CONTENT_TAB_KEY_FIELD, lastShellContentTabKey);
        }
        return root;
    }

    @Override
    public UIState deserialize(JsonElement data, Gson gson) {
        if (!data.isJsonObject()) {
            return defaults();
        }
        JsonObject root = data.getAsJsonObject();
        String tabKey = root.has(LAST_SHELL_CONTENT_TAB_KEY_FIELD) ? root.get(LAST_SHELL_CONTENT_TAB_KEY_FIELD).getAsString() : null;
        return new UIState(tabKey);
    }
}
