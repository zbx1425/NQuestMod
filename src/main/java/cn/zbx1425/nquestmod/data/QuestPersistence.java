package cn.zbx1425.nquestmod.data;

import cn.zbx1425.nquestmod.CommandSigner;
import cn.zbx1425.nquestmod.NQuestMod;
import cn.zbx1425.nquestmod.data.quest.Quest;
import cn.zbx1425.nquestmod.data.quest.QuestCategory;
import cn.zbx1425.nquestmod.data.quest.PlayerProfile;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class QuestPersistence {

    public final Path basePath;

    public QuestPersistence(Path basePath) {
        this.basePath = basePath;
    }

    public void saveQuestDefinition(Quest quest) throws IOException {
        Path questPath = basePath.resolve("quests").resolve(quest.id + ".json");
        Files.createDirectories(questPath.getParent());
        try (Writer writer = Files.newBufferedWriter(questPath)) {
            NQuestGson.PRETTY.toJson(quest, writer);
        }
        quest.preTouchDescriptions();
    }

    public void removeQuestDefinition(String questId) throws IOException {
        Path questPath = basePath.resolve("quests").resolve(questId + ".json");
        Files.deleteIfExists(questPath);
    }

    public Map<String, Quest> loadQuestDefinitions() throws IOException {
        Path questsDir = basePath.resolve("quests");
        Map<String, Quest> quests = new HashMap<>();
        if (!Files.isDirectory(questsDir)) {
            return quests;
        }
        try (Stream<Path> files = Files.list(questsDir)) {
            files.filter(path -> path.toString().endsWith(".json")).forEach(path -> {
                try (Reader reader = Files.newBufferedReader(path)) {
                    Quest quest = NQuestGson.INSTANCE.fromJson(reader, Quest.class);
                    quest.preTouchDescriptions();
                    quests.put(quest.id, quest);
                } catch (Exception e) {
                    NQuestMod.LOGGER.error("Failed to load quest definition from {}", path, e);
                }
            });
        }
        return quests;
    }

    public Map<String, QuestCategory> loadQuestCategories() throws IOException {
        Path categoriesFile = basePath.resolve("categories.json");
        if (!Files.exists(categoriesFile)) {
            return new HashMap<>();
        }
        try (Reader reader = Files.newBufferedReader(categoriesFile)) {
            Type mapType = new TypeToken<Map<String, QuestCategory>>() {}.getType();
            Map<String, QuestCategory> categories = NQuestGson.INSTANCE.fromJson(reader, mapType);
            if (categories == null) return new HashMap<>();
            return new HashMap<>(categories);
        }
    }

    public void saveQuestCategories(Map<String, QuestCategory> categories) throws IOException {
        Path categoriesFile = basePath.resolve("categories.json");
        Files.createDirectories(categoriesFile.getParent());
        try (Writer writer = Files.newBufferedWriter(categoriesFile)) {
            NQuestGson.PRETTY.toJson(categories, writer);
        }
    }

    public void removeStaleQuests(java.util.Set<String> keepIds) throws IOException {
        Path questsDir = basePath.resolve("quests");
        if (!Files.isDirectory(questsDir)) return;
        try (Stream<Path> files = Files.list(questsDir)) {
            files.filter(p -> p.toString().endsWith(".json")).forEach(p -> {
                String filename = p.getFileName().toString();
                String questId = filename.substring(0, filename.length() - ".json".length());
                if (!keepIds.contains(questId)) {
                    try {
                        Files.delete(p);
                        NQuestMod.LOGGER.info("Removed stale quest cache: {}", questId);
                    } catch (IOException e) {
                        NQuestMod.LOGGER.warn("Failed to delete stale quest cache: {}", p, e);
                    }
                }
            });
        }
    }

    public SyncConfig loadSyncConfig() {
        Path configFile = basePath.resolve("sync_config.json");
        if (!Files.exists(configFile)) {
            return new SyncConfig();
        }
        try (Reader reader = Files.newBufferedReader(configFile)) {
            SyncConfig config = NQuestGson.INSTANCE.fromJson(reader, SyncConfig.class);
            return config != null ? config : new SyncConfig();
        } catch (Exception e) {
            NQuestMod.LOGGER.warn("Failed to load sync config, sync disabled", e);
            return new SyncConfig();
        }
    }

    public CommandSigner getOrCreateCommandSigner() throws IOException {
        Path signerFile = basePath.resolve("sign_secret.json");
        if (Files.exists(signerFile)) {
            try (Reader reader = Files.newBufferedReader(signerFile)) {
                return NQuestGson.INSTANCE.fromJson(reader, CommandSigner.class);
            }
        } else {
            CommandSigner signer = new CommandSigner();
            try (Writer writer = Files.newBufferedWriter(signerFile)) {
                NQuestGson.INSTANCE.toJson(signer, writer);
            }
            return signer;
        }
    }

    public static String serializeQuest(Quest quest) {
        return NQuestGson.INSTANCE.toJson(quest);
    }

    public static Quest deserializeQuest(String json) throws Exception {
        return NQuestGson.INSTANCE.fromJson(json, Quest.class);
    }

    public static String serializePlayerProfile(PlayerProfile profile) {
        return NQuestGson.INSTANCE.toJson(profile);
    }

    public static PlayerProfile deserializePlayerProfile(String json) throws Exception {
        return NQuestGson.INSTANCE.fromJson(json, PlayerProfile.class);
    }

    public static String serializeCategories(Map<String, QuestCategory> categories) {
        return NQuestGson.INSTANCE.toJson(categories);
    }

    public static Map<String, QuestCategory> deserializeCategories(String json) throws Exception {
        Type mapType = new TypeToken<Map<String, QuestCategory>>() {}.getType();
        return NQuestGson.INSTANCE.fromJson(json, mapType);
    }
}
