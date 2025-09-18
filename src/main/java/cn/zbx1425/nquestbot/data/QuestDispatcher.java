package cn.zbx1425.nquestbot.data;

import cn.zbx1425.nquestbot.data.quest.Quest;
import cn.zbx1425.nquestbot.data.quest.Step;
import cn.zbx1425.nquestbot.data.quest.PlayerProfile;
import cn.zbx1425.nquestbot.data.quest.QuestCompletionData;
import cn.zbx1425.nquestbot.data.quest.QuestProgress;
import cn.zbx1425.nquestbot.data.ranking.RankingManager;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class QuestDispatcher {

    private final IQuestCallbacks callback;
    private final RankingManager rankingManager;
    public Map<String, Quest> quests;
    public final Map<UUID, PlayerProfile> playerProfiles = new HashMap<>();

    public QuestDispatcher(IQuestCallbacks callback, RankingManager rankingManager) {
        this.callback = callback;
        this.rankingManager = rankingManager;
        this.quests = Map.of();
    }

    public PlayerProfile getPlayerProfile(UUID playerUuid) {
        return playerProfiles.get(playerUuid);
    }

    public boolean updatePlayers(Function<UUID, ServerPlayer> playerGetter) {
        boolean isAnyQuestGoingOn = false;
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
                isAnyQuestGoingOn = true;

                Step currentStep = quest.steps.get(progress.currentStepIndex);
                ServerPlayer player = playerGetter.apply(profile.playerUuid);
                if (player == null) continue; // Player might not be online, but has active quest

                if (checkCriteriaFulfilled(progress, currentStep, player, null)) {
                    advanceQuestStep(profile, progress, quest, player);
                }
            }
        }
        return isAnyQuestGoingOn;
    }

    public void triggerManualCriterion(UUID playerUuid, String triggerId, ServerPlayer player) throws QuestException {
        PlayerProfile profile = playerProfiles.get(playerUuid);
        if (profile == null) throw new QuestException(QuestException.Type.PLAYER_NOT_FOUND);

        for (QuestProgress progress : new ArrayList<>(profile.activeQuests.values())) {
            Quest quest = quests.get(progress.questId);
            if (quest == null || progress.currentStepIndex >= quest.steps.size()) {
                continue;
            }

            Step currentStep = quest.steps.get(progress.currentStepIndex);

            if (checkCriteriaFulfilled(progress, currentStep, player, triggerId)) {
                advanceQuestStep(profile, progress, quest, player);
            }
        }
    }

    public void startQuest(UUID playerUuid, String questId) throws QuestException {
        PlayerProfile profile = playerProfiles.get(playerUuid);
        if (profile == null) throw new QuestException(QuestException.Type.PLAYER_NOT_FOUND);
        Quest quest = quests.get(questId);
        if (quest == null) throw new QuestException(QuestException.Type.QUEST_NOT_FOUND);
        if (profile.activeQuests.containsKey(questId)) throw new QuestException(QuestException.Type.QUEST_ALREADY_STARTED);
        if (!profile.activeQuests.isEmpty()) throw new QuestException(QuestException.Type.QUEST_ONLY_ONE_AT_A_TIME);

        QuestProgress progress = new QuestProgress();
        progress.questId = questId;
        progress.currentStepIndex = 0;
        progress.questStartTime = System.currentTimeMillis();
        progress.stepStartTimes = new HashMap<>();
        progress.stepStartTimes.put(0, progress.questStartTime);

        profile.activeQuests.put(questId, progress);
        callback.onQuestStarted(this, playerUuid, quest);
    }

    public void stopQuests(UUID playerUuid) throws QuestException {
        PlayerProfile profile = playerProfiles.get(playerUuid);
        if (profile == null) throw new QuestException(QuestException.Type.PLAYER_NOT_FOUND);
        if (profile.activeQuests.isEmpty()) throw new QuestException(QuestException.Type.QUEST_NOT_STARTED);
        List<QuestProgress> progresses = new ArrayList<>(profile.activeQuests.values());
        profile.activeQuests.clear();
        for (QuestProgress progress : progresses) {
            Quest quest = quests.get(progress.questId);
            if (quest != null) {
                callback.onQuestAborted(this, playerUuid, quest);
            }
        }
    }

    private void advanceQuestStep(PlayerProfile profile, QuestProgress progress, Quest quest, ServerPlayer player) {
        // Mark current step as complete
        long now = System.currentTimeMillis();
        progress.currentStepIndex++;
        callback.onStepCompleted(this, profile.playerUuid, quest, progress);

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
            if (rankingManager != null) {
                rankingManager.onQuestCompleted(profile.playerUuid, player.getGameProfile().getName(),
                        profile.totalQuestPoints, quest, completionData);
            }
        } else {
            // Advance to next step
            progress.stepStartTimes.put(progress.currentStepIndex, now);
        }
    }

    private boolean checkCriteriaFulfilled(QuestProgress progress, Step step, ServerPlayer player, String triggerId) {
        if (progress.currentStepStatefulCriteria == null) {
            progress.currentStepStatefulCriteria = step.createStatefulCriteria();
        }
        if (triggerId != null) {
            progress.currentStepStatefulCriteria.propagateManualTrigger(triggerId);
        }
        if (progress.currentStepStatefulCriteria.isFulfilled(player)) {
            progress.currentStepStatefulCriteria = null; // Reset for next step
            return true;
        } else {
            return false;
        }
    }
}
