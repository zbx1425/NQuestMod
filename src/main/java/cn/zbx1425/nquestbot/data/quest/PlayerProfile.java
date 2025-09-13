package cn.zbx1425.nquestbot.data.quest;

import java.util.Map;
import java.util.UUID;

public class PlayerProfile {

    public UUID playerUuid;
    public Map<UUID, QuestProgress> activeQuests;
    public Map<UUID, QuestCompletionData> completedQuests;
    public int totalQuestPoints;
}
