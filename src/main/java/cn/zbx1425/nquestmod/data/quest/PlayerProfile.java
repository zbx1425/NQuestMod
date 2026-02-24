package cn.zbx1425.nquestmod.data.quest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerProfile {

    public transient UUID playerUuid;
    public transient int qpBalance;
    public transient int totalQuestCompletions;
    public transient long lastStatsSyncTime;

    public Map<String, QuestProgress> activeQuests = new HashMap<>();

    public boolean isStatsCacheStale(long maxAgeMillis) {
        return System.currentTimeMillis() - lastStatsSyncTime > maxAgeMillis;
    }
}
