package cc.fascinated.fascinatedutils.systems.config.serialization;

import cc.fascinated.fascinatedutils.systems.config.serialization.impl.ModSerializationGson;
import com.google.gson.*;

import java.lang.reflect.Type;

public interface Serializable<T> extends JsonSerializer<T>, JsonDeserializer<T> {

    /**
     * Gson entry point that delegates to {@link #serializeToJson(Object, com.google.gson.Gson)}.
     */
    @Override
    default JsonElement serialize(T src, Type ignored, JsonSerializationContext ignored_) {
        return serializeToJson(src, ModSerializationGson.get());
    }

    /**
     * Gson entry point that wraps the JSON object and delegates to {@link #deserializeFromJson}.
     */
    @Override
    default T deserialize(JsonElement json, Type ignored, JsonDeserializationContext ignored_) {
        if (json == null || !json.isJsonObject()) {
            throw new JsonParseException("Expected JSON object, got " + (json == null ? "null" : json.getClass().getSimpleName()));
        }
        return deserializeFromJson(new JsonObjectWrapper(json.getAsJsonObject()), ModSerializationGson.get());
    }

    /**
     * Serializes {@code object} to JSON using the provided Gson instance.
     */
    default JsonElement serializeToJson(T object, com.google.gson.Gson gson) {
        throw new UnsupportedOperationException("serializeToJson not implemented");
    }

    /**
     * Deserializes {@code data} into a Java value using the provided Gson instance.
     */
    default T deserializeFromJson(JsonObjectWrapper data, com.google.gson.Gson gson) {
        throw new UnsupportedOperationException("deserializeFromJson not implemented");
    }
}
