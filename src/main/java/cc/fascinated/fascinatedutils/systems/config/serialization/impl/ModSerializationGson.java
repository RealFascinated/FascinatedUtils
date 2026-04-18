package cc.fascinated.fascinatedutils.systems.config.serialization.impl;

import cc.fascinated.fascinatedutils.systems.config.ModulePersistentState;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ModSerializationGson {
    private static final ModulePersistentStateSerializer MODULE_STATE_SERIALIZER = new ModulePersistentStateSerializer();

    /**
     * Shared Gson instance configured for mod settings persistence (pretty-printed, HTML escaping off).
     */
    public static Gson get() {
        return Holder.INSTANCE;
    }

    private static class Holder {
        private static final Gson INSTANCE = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().registerTypeAdapter(ModulePersistentState.class, MODULE_STATE_SERIALIZER).create();
    }
}
