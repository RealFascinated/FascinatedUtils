package cc.fascinated.fascinatedutils.systems.config;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

public interface GsonSerializable<T> {

    /**
     * Serializes this object to JSON using the provided Gson instance.
     *
     * @param gson the Gson instance to use for nested serialization
     * @return the serialized JSON element
     */
    JsonElement serialize(Gson gson);

    /**
     * Deserializes data into a new instance of {@code T}.
     *
     * @param data the JSON element to read from
     * @param gson the Gson instance to use for nested deserialization
     * @return the deserialized instance
     */
    T deserialize(JsonElement data, Gson gson);
}
