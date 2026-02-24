package cn.zbx1425.nquestmod;

import com.google.common.base.CaseFormat;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

public class ServerConfig {

    public static class ConfigItem<T> {

        private final String camelKey;
        public final T value;
        public final boolean isPresentInJson;
        private final JsonElement jsonRawValue;

        public ConfigItem(JsonObject json, String camelKey, Supplier<T> defaultValue, Function<String, T> stringParser) {
            T toBeValue;
            this.camelKey = camelKey;

            String snakeKey = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, camelKey);
            String envValueStr = System.getenv("NQUEST_" + snakeKey);

            if (json.has(camelKey)) {
                this.isPresentInJson = true;
                this.jsonRawValue = json.get(camelKey);
            } else {
                this.isPresentInJson = false;
                this.jsonRawValue = null;
            }

            if (envValueStr != null) {
                toBeValue = stringParser.apply(envValueStr);
            } else {
                if (isPresentInJson) {
                    try {
                        if (jsonRawValue.isJsonPrimitive()) {
                            toBeValue = stringParser.apply(jsonRawValue.getAsString());
                        } else {
                            toBeValue = stringParser.apply(jsonRawValue.toString());
                        }
                    } catch (Exception e) {
                        NQuestMod.LOGGER.warn("Failed to parse JSON value for {}", camelKey, e);
                        toBeValue = defaultValue.get();
                    }
                } else {
                    toBeValue = defaultValue.get();
                }
            }
            this.value = toBeValue;
        }

        public ConfigItem(JsonObject json, String camelKey, T defaultValue, Function<String, T> stringParser) {
            this(json, camelKey, () -> defaultValue, stringParser);
        }

        private ConfigItem(String camelKey, T value, boolean isPresentInJson, JsonElement jsonRawValue) {
            this.camelKey = camelKey;
            this.value = value;
            this.isPresentInJson = isPresentInJson;
            this.jsonRawValue = jsonRawValue;
        }

        public ConfigItem<T> withNewValueToPersist(T newValue, JsonElement jsonRawValue) {
            return new ConfigItem<>(camelKey, newValue, true, jsonRawValue);
        }

        public void writeJson(JsonObject json) {
            if (isPresentInJson) {
                json.add(camelKey, jsonRawValue);
            }
        }
    }

    public ConfigItem<String> webBackendUrl;
    public ConfigItem<String> webApiKey;
    public ConfigItem<Boolean> webSyncEnabled;
    public ConfigItem<Integer> webSyncPollIntervalSeconds;

    public ConfigItem<UUID> commandSigningKey;

    private Path path;

    private static <T extends Enum<T>> T parseEnum(String name, Class<T> enumClass) {
        return Enum.valueOf(enumClass, name.toUpperCase());
    }

    public void load(Path configPath) throws IOException {
        this.path = configPath;
        JsonObject json = Files.exists(configPath)
            ? JsonParser.parseString(Files.readString(configPath)).getAsJsonObject()
            : new JsonObject();

        webBackendUrl = new ConfigItem<>(json, "webBackendUrl", "", value -> value);
        webApiKey = new ConfigItem<>(json, "webApiKey", "", value -> value);
        webSyncEnabled = new ConfigItem<>(json, "webSyncEnabled", true, Boolean::parseBoolean);
        webSyncPollIntervalSeconds = new ConfigItem<>(json, "webSyncPollIntervalSeconds", 60, Integer::parseInt);

        commandSigningKey = new ConfigItem<UUID>(json, "commandSigningKey", () -> null, UUID::fromString);

        if (!Files.exists(configPath)) save(configPath);
    }

    public void save(Path configPath) throws IOException {
        JsonObject json = new JsonObject();

        webBackendUrl.writeJson(json);
        webApiKey.writeJson(json);
        webSyncEnabled.writeJson(json);
        webSyncPollIntervalSeconds.writeJson(json);

        commandSigningKey.writeJson(json);

        Files.writeString(configPath, new GsonBuilder().setPrettyPrinting().create().toJson(json));
    }

    public void save() throws IOException {
        save(this.path);
    }
}