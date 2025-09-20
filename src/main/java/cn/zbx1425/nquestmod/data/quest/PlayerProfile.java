package cn.zbx1425.nquestmod.data.quest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerProfile {

    public transient UUID playerUuid;
    public transient int totalQuestPoints;
    public transient int totalQuestCompletions;

    public Map<String, QuestProgress> activeQuests = new HashMap<>();
}
