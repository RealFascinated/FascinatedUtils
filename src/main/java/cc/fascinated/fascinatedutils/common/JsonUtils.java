package cc.fascinated.fascinatedutils.common;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JsonUtils {

    public static JsonObject objectMember(JsonObject root, String memberKey) {
        if (root == null) {
            return null;
        }
        JsonElement memberElement = root.get(memberKey);
        if (memberElement == null || !memberElement.isJsonObject()) {
            return null;
        }
        return memberElement.getAsJsonObject();
    }

    public static JsonObject objectMemberOrEmpty(JsonObject root, String memberKey) {
        JsonObject objectMember = objectMember(root, memberKey);
        if (objectMember == null) {
            return new JsonObject();
        }
        return objectMember;
    }

    public static JsonArray arrayMember(JsonObject root, String memberKey) {
        if (root == null) {
            return null;
        }
        JsonElement memberElement = root.get(memberKey);
        if (memberElement == null || !memberElement.isJsonArray()) {
            return null;
        }
        return memberElement.getAsJsonArray();
    }

    public static List<JsonObject> objectElements(JsonArray jsonArray) {
        List<JsonObject> objectElements = new ArrayList<>();
        if (jsonArray == null) {
            return objectElements;
        }
        for (JsonElement jsonElement : jsonArray) {
            if (jsonElement != null && jsonElement.isJsonObject()) {
                objectElements.add(jsonElement.getAsJsonObject());
            }
        }
        return objectElements;
    }

    public static String stringMember(JsonObject root, String memberKey) {
        if (root == null) {
            return null;
        }
        JsonElement memberElement = root.get(memberKey);
        if (memberElement == null || !memberElement.isJsonPrimitive()) {
            return null;
        }
        return memberElement.getAsString();
    }

    public static String stringMember(JsonObject root, String memberKey, String fallback) {
        String value = stringMember(root, memberKey);
        if (value == null) {
            return fallback;
        }
        return value;
    }

    public static UUID uuidMember(JsonObject root, String memberKey) {
        String value = stringMember(root, memberKey);
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (Exception ignored) {
            return null;
        }
    }
}