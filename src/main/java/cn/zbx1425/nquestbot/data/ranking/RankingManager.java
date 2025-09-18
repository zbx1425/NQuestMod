package cn.zbx1425.nquestbot.data.ranking;

import cn.zbx1425.nquestbot.data.quest.Quest;
import cn.zbx1425.nquestbot.data.quest.QuestCompletionData;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class RankingManager {

    private static final int LEADERBOARD_SIZE = 100;

    private List<PlayerQPEntry> overallQPLeaderboard = new CopyOnWriteArrayList<>();
    private Map<String, List<QuestTimeEntry>> perQuestTimeLeaderboards = new ConcurrentHashMap<>();

    private Path savePath;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public RankingManager(Path savePath) {
        this.savePath = savePath;
        load();
    }

    public void onQuestCompleted(UUID playerUuid, String playerName, int newTotalQP, Quest quest, QuestCompletionData completionData) {
        updateOverallQP(playerUuid, playerName, newTotalQP);
        updateQuestTime(quest.id, playerUuid, playerName, completionData.durationMillis);
        save();
    }

    private void updateOverallQP(UUID playerUuid, String playerName, int newTotalQP) {
        overallQPLeaderboard.removeIf(entry -> entry.playerUuid.equals(playerUuid));
        PlayerQPEntry newEntry = new PlayerQPEntry(playerUuid, playerName, newTotalQP);
        overallQPLeaderboard.add(newEntry);
        overallQPLeaderboard.sort(Comparator.comparingInt(PlayerQPEntry::getTotalQP).reversed());
        if (overallQPLeaderboard.size() > LEADERBOARD_SIZE) {
            overallQPLeaderboard.subList(LEADERBOARD_SIZE, overallQPLeaderboard.size()).clear();
        }
    }

    private void updateQuestTime(String questId, UUID playerUuid, String playerName, long duration) {
        List<QuestTimeEntry> leaderboard = perQuestTimeLeaderboards.computeIfAbsent(questId, k -> new CopyOnWriteArrayList<>());
        Optional<QuestTimeEntry> existingEntry = leaderboard.stream()
                .filter(entry -> entry.playerUuid.equals(playerUuid))
                .findFirst();

        if (existingEntry.isPresent()) {
            if (duration < existingEntry.get().durationMillis) {
                existingEntry.get().durationMillis = duration;
            } else {
                return; // No improvement, no need to re-sort
            }
        } else {
            leaderboard.add(new QuestTimeEntry(playerUuid, playerName, duration));
        }

        leaderboard.sort(Comparator.comparingLong(QuestTimeEntry::getDurationMillis));
        if (leaderboard.size() > LEADERBOARD_SIZE) {
            leaderboard.subList(LEADERBOARD_SIZE, leaderboard.size()).clear();
        }
    }

    public List<PlayerQPEntry> getOverallQPLeaderboard() {
        return Collections.unmodifiableList(overallQPLeaderboard);
    }

    public List<QuestTimeEntry> getQuestTimeLeaderboard(String questId) {
        return Collections.unmodifiableList(perQuestTimeLeaderboards.getOrDefault(questId, Lists.newArrayList()));
    }

    public void load() {
        try (FileReader reader = new FileReader(savePath.toFile())) {
            RankingData data = gson.fromJson(reader, RankingData.class);
            if (data != null) {
                this.overallQPLeaderboard = new CopyOnWriteArrayList<>(data.overallQPLeaderboard);
                this.perQuestTimeLeaderboards = new ConcurrentHashMap<>(data.perQuestTimeLeaderboards);
            }
        } catch (IOException e) {
            // File might not exist yet, which is fine.
        }
    }

    public void save() {
        try (FileWriter writer = new FileWriter(savePath.toFile())) {
            RankingData data = new RankingData();
            data.overallQPLeaderboard = new ArrayList<>(this.overallQPLeaderboard);
            data.perQuestTimeLeaderboards = new HashMap<>(this.perQuestTimeLeaderboards);
            gson.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class RankingData {
        List<PlayerQPEntry> overallQPLeaderboard;
        Map<String, List<QuestTimeEntry>> perQuestTimeLeaderboards;
    }
}
