package cn.zbx1425.nquestbot.data;

import cn.zbx1425.nquestbot.data.criteria.Criterion;
import cn.zbx1425.nquestbot.data.quest.Quest;
import cn.zbx1425.nquestbot.data.quest.Step;
import cn.zbx1425.nquestbot.data.quest.PlayerProfile;
import cn.zbx1425.nquestbot.data.quest.QuestCompletionData;
import cn.zbx1425.nquestbot.data.quest.QuestProgress;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.function.Function;

public class QuestDispatcher {

    private final IQuestCallbacks callback;
    public Map<String, Quest> quests;
    public final Map<UUID, PlayerProfile> playerProfiles = new HashMap<>();

    public QuestDispatcher(IQuestCallbacks callback) {
        this.callback = callback;
        this.quests = Map.of();
    }

    public PlayerProfile getPlayerProfile(UUID playerUuid) {
        return playerProfiles.get(playerUuid);
    }

    public void updatePlayers(Function<UUID, ServerPlayer> playerGetter) {
        for (PlayerProfile profile : playerProfiles.values()) {
            if (profile.activeQuests.isEmpty()) {
                continue;
            }

            // Iterate over a copy to avoid ConcurrentModificationException
            for (QuestProgress progress : new ArrayList<>(profile.activeQuests.values())) {
                Quest quest = quests.get(progress.questId);
                if (quest == null || progress.currentStepIndex >= quest.steps.size()) {
                    continue;
                }

                Step currentStep = quest.steps.get(progress.currentStepIndex);
                if (currentStep.needsManualTrigger) {
                    continue; // Needs manual trigger, advance logic is in handleManualTrigger
                }

                ServerPlayer status = playerGetter.apply(profile.playerUuid);
                if (status == null) continue; // Player might not be online, but has active quest

                if (areAllCriteriaFulfilled(currentStep, status)) {
                    advanceQuestStep(profile, progress, quest);
                }
            }
        }
    }

    public void handleManualTrigger(UUID playerUuid, UUID triggerStepId, ServerPlayer player) {
        PlayerProfile profile = playerProfiles.get(playerUuid);
        if (profile == null) return;

        for (QuestProgress progress : new ArrayList<>(profile.activeQuests.values())) {
            Quest quest = quests.get(progress.questId);
            if (quest == null || progress.currentStepIndex >= quest.steps.size()) {
                continue;
            }

            Step currentStep = quest.steps.get(progress.currentStepIndex);

            if (currentStep.needsManualTrigger && currentStep.id.equals(triggerStepId)) {
                // Check if all other criteria for this step are met
                if (areAllCriteriaFulfilled(currentStep, player)) {
                    advanceQuestStep(profile, progress, quest);
                    return; // Assume one trigger per call
                }
            }
        }
    }

    private boolean areAllCriteriaFulfilled(Step step, ServerPlayer player) {
        for (Criterion criterion : step.criteria) {
            if (!criterion.isFulfilled(player)) {
                return false;
            }
        }
        return true;
    }

    public void startQuest(UUID playerUuid, String questId) {
        PlayerProfile profile = playerProfiles.get(playerUuid);
        if (profile == null) return;

        Quest quest = quests.get(questId);
        if (quest == null) return; // Quest doesn't exist

        // Player is already doing or has completed this quest
        if (profile.activeQuests.containsKey(questId) || profile.completedQuests.containsKey(questId)) {
            return;
        }

        QuestProgress progress = new QuestProgress();
        progress.questId = questId;
        progress.currentStepIndex = 0;
        progress.questStartTime = System.currentTimeMillis();
        progress.stepStartTimes = new HashMap<>();
        progress.stepStartTimes.put(0, progress.questStartTime);

        profile.activeQuests.put(questId, progress);
        callback.onQuestStarted(this, playerUuid, quest);
    }

    private void advanceQuestStep(PlayerProfile profile, QuestProgress progress, Quest quest) {
        // Mark current step as complete
        long now = System.currentTimeMillis();

        callback.onStepCompleted(this, profile.playerUuid, quest, progress);

        progress.currentStepIndex++;

        if (progress.currentStepIndex >= quest.steps.size()) {
            // Quest completed
            profile.activeQuests.remove(progress.questId);

            QuestCompletionData completionData = new QuestCompletionData();
            completionData.completionTime = now;
            completionData.durationMillis = now - progress.questStartTime;
            
            // Calculate and store step durations
            completionData.stepDurations = new HashMap<>();
            long lastTimestamp = progress.questStartTime;
            for (int i = 0; i < quest.steps.size(); i++) {
                long stepEndTimestamp = progress.stepStartTimes.getOrDefault(i + 1, now);
                long stepStartTimestamp = progress.stepStartTimes.getOrDefault(i, lastTimestamp);
                completionData.stepDurations.put(i, stepEndTimestamp - stepStartTimestamp);
                lastTimestamp = stepEndTimestamp;
            }

            profile.completedQuests.put(progress.questId, completionData);
            profile.totalQuestPoints += quest.questPoints;

            callback.onQuestCompleted(this, profile.playerUuid, quest, completionData);
        } else {
            // Advance to next step
            progress.stepStartTimes.put(progress.currentStepIndex, now);
        }
    }
}
