package cn.zbx1425.nquestbot.data;

import cn.zbx1425.nquestbot.NQuestBot;
import cn.zbx1425.nquestbot.data.quest.Quest;
import cn.zbx1425.nquestbot.data.criteria.CriteriaRegistry;
import cn.zbx1425.nquestbot.data.quest.PlayerProfile;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
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

    public void savePlayerProfile(PlayerProfile profile) throws IOException {
        Path profilePath = basePath.resolve("profiles").resolve(profile.playerUuid + ".json");
        Files.createDirectories(profilePath.getParent());
        try (Writer writer = Files.newBufferedWriter(profilePath)) {
            GSON.toJson(profile, writer);
        }
    }

    public PlayerProfile loadPlayerProfile(UUID playerUuid) throws IOException {
        Path profilePath = basePath.resolve("profiles").resolve(playerUuid + ".json");
        if (!Files.exists(profilePath)) {
            // Return a new profile if one doesn't exist
            PlayerProfile newProfile = new PlayerProfile();
            newProfile.playerUuid = playerUuid;
            return newProfile;
        }
        try (Reader reader = Files.newBufferedReader(profilePath)) {
            return GSON.fromJson(reader, PlayerProfile.class);
        }
    }

    public void saveQuestDefinition(Quest quest) throws IOException {
        Path questPath = basePath.resolve("quests").resolve(quest.id + ".json");
        Files.createDirectories(questPath.getParent());
        try (Writer writer = Files.newBufferedWriter(questPath)) {
            GSON.toJson(quest, writer);
        }
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
                    quests.put(quest.id, quest);
                } catch (IOException e) {
                    NQuestBot.LOGGER.error("Failed to load quest definition from {}", path, e);
                }
            });
        }
        return quests;
    }
}
