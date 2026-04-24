package com.codeanalyzer.util;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class JsonUtil {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private JsonUtil() {}

    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }

    public static <T> List<T> fromJsonList(String json, Class<T> clazz) {
        Type listType = TypeToken.getParameterized(List.class, clazz).getType();
        return GSON.fromJson(json, listType);
    }

    public static JsonObject parseObject(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    public static JsonArray parseArray(String json) {
        return JsonParser.parseString(json).getAsJsonArray();
    }

    /** Safely pull a String field, returns "" if absent */
    public static String getString(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return (el == null || el.isJsonNull()) ? "" : el.getAsString();
    }

    /** Safely pull an int field, returns defaultVal if absent */
    public static int getInt(JsonObject obj, String key, int defaultVal) {
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) return defaultVal;
        try { return el.getAsInt(); } catch (Exception e) { return defaultVal; }
    }
}
