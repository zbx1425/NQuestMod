package cn.zbx1425.nquestbot.data.leaderboard;

import cn.zbx1425.nquestbot.data.quest.PlayerProfile;
import cn.zbx1425.nquestbot.data.quest.QuestCompletionData;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class LeaderboardManager {

    // 总 QP 排行榜
    private final Map<UUID, Integer> questPointsLeaderboard = new ConcurrentHashMap<>();

    // 每个任务的速通排行榜
    private final Map<UUID, QuestLeaderboard> timeLeaderboards = new ConcurrentHashMap<>();

    public void updatePlayerQP(PlayerProfile profile) {
        questPointsLeaderboard.put(profile.playerUuid, profile.totalQuestPoints);
    }

    public void recordQuestCompletion(UUID playerUuid, UUID questId, QuestCompletionData data) {
        timeLeaderboards.computeIfAbsent(questId, k -> new QuestLeaderboard(questId))
                .addEntry(playerUuid, data.durationMillis);
    }

    public List<PlayerQPScore> getTopQP(int limit) {
        return questPointsLeaderboard.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(limit)
                .map(entry -> new PlayerQPScore(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    public QuestLeaderboard getQuestLeaderboard(UUID questId) {
        return timeLeaderboards.get(questId);
    }
}
