package cn.zbx1425.nquestbot.data.quest;

import java.util.Map;
import java.util.UUID;

public class PlayerProfile {

    public UUID playerUuid;
    public Map<String, QuestProgress> activeQuests;
    public Map<String, QuestCompletionData> completedQuests;
    public int totalQuestPoints;
}
