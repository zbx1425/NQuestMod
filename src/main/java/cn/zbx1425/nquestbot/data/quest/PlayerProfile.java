package cn.zbx1425.nquestbot.data.quest;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.Map;
import java.util.UUID;

public class PlayerProfile {

    public UUID playerUuid;
    public Map<String, QuestProgress> activeQuests;
    public Map<String, QuestCompletionData> completedQuests;
    public int totalQuestPoints;

    public PlayerProfile(UUID playerUuid) {
        this.playerUuid = playerUuid;
        activeQuests = new Object2ObjectOpenHashMap<>();
        completedQuests = new Object2ObjectOpenHashMap<>();
        totalQuestPoints = 0;
    }

    public void startQuest(Quest quest, long startTime) {
        if (activeQuests.containsKey(quest.id)) {
            throw new IllegalStateException("Quest already started");
        }
        QuestProgress progress = new QuestProgress();
        progress.questId = quest.id;
        progress.currentStepIndex = 0;
        progress.questStartTime = startTime;
        progress.stepStartTimes = Map.of(0, startTime);
        progress.currentStepStatefulCriteria = quest.steps.get(0).createStatefulCriteria();
        activeQuests.put(quest.id, progress);
    }
}
