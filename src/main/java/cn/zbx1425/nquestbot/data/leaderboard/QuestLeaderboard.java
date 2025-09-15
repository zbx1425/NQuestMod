package cn.zbx1425.nquestbot.data.leaderboard;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class QuestLeaderboard {

    public final String questId;
    private final Map<UUID, Long> scores = new ConcurrentHashMap<>(); // Player UUID -> Duration

    public QuestLeaderboard(String questId) {
        this.questId = questId;
    }

    public void addEntry(UUID playerUuid, long durationMillis) {
        scores.merge(playerUuid, durationMillis, Math::min); // Only keep the best time
    }

    public List<TimeEntry> getTop(int limit) {
        return scores.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(limit)
                .map(entry -> new TimeEntry(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    public static class TimeEntry {
        public final UUID playerUuid;
        public final long durationMillis;

        public TimeEntry(UUID playerUuid, long durationMillis) {
            this.playerUuid = playerUuid;
            this.durationMillis = durationMillis;
        }
    }
}
