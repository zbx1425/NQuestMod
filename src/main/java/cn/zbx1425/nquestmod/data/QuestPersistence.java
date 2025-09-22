package cn.zbx1425.nquestmod.data;

import cn.zbx1425.nquestmod.CommandSigner;
import cn.zbx1425.nquestmod.NQuestMod;
import cn.zbx1425.nquestmod.data.quest.Quest;
import cn.zbx1425.nquestmod.data.quest.QuestCategory;
import cn.zbx1425.nquestmod.data.criteria.CriteriaRegistry;
import cn.zbx1425.nquestmod.data.quest.PlayerProfile;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapterFactory(CriteriaRegistry.getFactory())
            .setPrettyPrinting()
            .create();

    public final Path basePath;

    public QuestPersistence(Path basePath) {
        this.basePath = basePath;
    }

    public void saveQuestDefinition(Quest quest) throws IOException {
        Path questPath = basePath.resolve("quests").resolve(quest.id + ".json");
        Files.createDirectories(questPath.getParent());
        try (Writer writer = Files.newBufferedWriter(questPath)) {
            GSON.toJson(quest, writer);
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
                    Quest quest = GSON.fromJson(reader, Quest.class);
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
            Map<String, QuestCategory> categories = GSON.fromJson(reader, mapType);
            if (categories == null) return new HashMap<>();
            return new HashMap<>(categories);
        }
    }

    public void saveQuestCategories(Map<String, QuestCategory> categories) throws IOException {
        Path categoriesFile = basePath.resolve("categories.json");
        Files.createDirectories(categoriesFile.getParent());
        try (Writer writer = Files.newBufferedWriter(categoriesFile)) {
            GSON.toJson(categories, writer);
        }
    }

    public CommandSigner getOrCreateCommandSigner() throws IOException {
        Path signerFile = basePath.resolve("sign_secret.json");
        if (Files.exists(signerFile)) {
            try (Reader reader = Files.newBufferedReader(signerFile)) {
                return GSON.fromJson(reader, CommandSigner.class);
            }
        } else {
            CommandSigner signer = new CommandSigner();
            try (Writer writer = Files.newBufferedWriter(signerFile)) {
                GSON.toJson(signer, writer);
            }
            return signer;
        }
    }

    public static String serializeQuest(Quest quest) {
        return GSON.toJson(quest);
    }

    public static Quest deserializeQuest(String json) throws Exception {
        return GSON.fromJson(json, Quest.class);
    }

    public static String serializePlayerProfile(PlayerProfile profile) {
        return GSON.toJson(profile);
    }

    public static PlayerProfile deserializePlayerProfile(String json) throws Exception {
        return GSON.fromJson(json, PlayerProfile.class);
    }

    public static String serializeCategories(Map<String, QuestCategory> categories) {
        return GSON.toJson(categories);
    }

    public static Map<String, QuestCategory> deserializeCategories(String json) throws Exception {
        Type mapType = new TypeToken<Map<String, QuestCategory>>() {}.getType();
        return GSON.fromJson(json, mapType);
    }
}
