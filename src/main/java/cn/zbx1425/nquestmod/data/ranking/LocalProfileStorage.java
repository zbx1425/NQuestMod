package cn.zbx1425.nquestmod.data.ranking;

import cn.zbx1425.nquestmod.NQuestMod;
import cn.zbx1425.nquestmod.data.NQuestGson;
import cn.zbx1425.nquestmod.data.quest.QuestProgress;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LocalProfileStorage {

    private static final Type ACTIVE_QUESTS_TYPE = new TypeToken<Map<String, QuestProgress>>() {}.getType();

    private final Path profilesDir;
    private final Gson gson = NQuestGson.INSTANCE;

    public LocalProfileStorage(Path basePath) throws IOException {
        this.profilesDir = basePath.resolve("profiles");
        Files.createDirectories(profilesDir);
    }

    public Map<String, QuestProgress> load(UUID playerUuid) {
        Path file = profilesDir.resolve(playerUuid.toString() + ".json");
        if (!Files.exists(file)) {
            return new HashMap<>();
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            Map<String, QuestProgress> result = gson.fromJson(reader, ACTIVE_QUESTS_TYPE);
            return result != null ? result : new HashMap<>();
        } catch (Exception e) {
            NQuestMod.LOGGER.error("Failed to load local profile for {}", playerUuid, e);
            return new HashMap<>();
        }
    }

    public void save(UUID playerUuid, Map<String, QuestProgress> activeQuests) {
        Path file = profilesDir.resolve(playerUuid.toString() + ".json");
        if (activeQuests == null || activeQuests.isEmpty()) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                NQuestMod.LOGGER.error("Failed to delete local profile for {}", playerUuid, e);
            }
            return;
        }
        try (Writer writer = Files.newBufferedWriter(file)) {
            gson.toJson(activeQuests, ACTIVE_QUESTS_TYPE, writer);
        } catch (IOException e) {
            NQuestMod.LOGGER.error("Failed to save local profile for {}", playerUuid, e);
        }
    }
}
