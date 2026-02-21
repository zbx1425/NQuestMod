package cn.zbx1425.nquestmod.data.quest;

import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class StepState {

    private Map<String, JsonObject> nodeStates = new HashMap<>();

    public JsonObject getOrCreate(String path, Supplier<JsonObject> defaultState) {
        return nodeStates.computeIfAbsent(path, k -> defaultState.get());
    }

    public JsonObject get(String path) {
        return nodeStates.get(path);
    }

    public boolean getBoolean(String path, String key, boolean defaultValue) {
        JsonObject node = nodeStates.get(path);
        if (node == null || !node.has(key)) return defaultValue;
        return node.get(key).getAsBoolean();
    }

    public void setBoolean(String path, String key, boolean value) {
        getOrCreate(path, JsonObject::new).addProperty(key, value);
    }

    public double getDouble(String path, String key, double defaultValue) {
        JsonObject node = nodeStates.get(path);
        if (node == null || !node.has(key)) return defaultValue;
        return node.get(key).getAsDouble();
    }

    public void setDouble(String path, String key, double value) {
        getOrCreate(path, JsonObject::new).addProperty(key, value);
    }

    public int getInt(String path, String key, int defaultValue) {
        JsonObject node = nodeStates.get(path);
        if (node == null || !node.has(key)) return defaultValue;
        return node.get(key).getAsInt();
    }

    public void setInt(String path, String key, int value) {
        getOrCreate(path, JsonObject::new).addProperty(key, value);
    }
}
