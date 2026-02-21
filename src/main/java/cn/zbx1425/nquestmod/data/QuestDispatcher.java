package cn.zbx1425.nquestmod.data;

import cn.zbx1425.nquestmod.data.criteria.CriterionContext;
import cn.zbx1425.nquestmod.data.ranking.QuestUserDatabase;
import cn.zbx1425.nquestmod.data.quest.Quest;
import cn.zbx1425.nquestmod.data.quest.Step;
import cn.zbx1425.nquestmod.data.quest.StepState;
import cn.zbx1425.nquestmod.data.quest.PlayerProfile;
import cn.zbx1425.nquestmod.data.quest.QuestCompletionData;
import cn.zbx1425.nquestmod.data.quest.QuestProgress;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.Set;
import java.util.HashSet;

public class QuestDispatcher {

    private final IQuestCallbacks callback;
    private final QuestUserDatabase databaseManager;
    public Map<String, Quest> quests;
    public final Map<UUID, PlayerProfile> playerProfiles = new HashMap<>();
    private final Set<UUID> debugPlayers = new HashSet<>();

    public QuestDispatcher(IQuestCallbacks callback, QuestUserDatabase databaseManager) {
        this.callback = callback;
        this.databaseManager = databaseManager;
        this.quests = Map.of();
    }

    public boolean isDebugMode(UUID playerUuid) {
        return debugPlayers.contains(playerUuid);
    }

    public boolean toggleDebugMode(UUID playerUuid) {
        if (debugPlayers.contains(playerUuid)) {
            debugPlayers.remove(playerUuid);
            return false;
        } else {
            debugPlayers.add(playerUuid);
            return true;
        }
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

            for (QuestProgress progress : new ArrayList<>(profile.activeQuests.values())) {
                if (progress.questSnapshot == null
                    || progress.currentStepIndex >= progress.questSnapshot.steps.size()) {
                    continue;
                }
                isAnyQuestGoingOn = true;

                ServerPlayer player = playerGetter.apply(profile.playerUuid);
                if (player == null) continue;

                tryAdvance(profile, progress, player, null);
            }
        }
        return isAnyQuestGoingOn;
    }

    public void triggerManualCriterion(UUID playerUuid, String triggerId, ServerPlayer player) throws QuestException {
        PlayerProfile profile = playerProfiles.get(playerUuid);
        if (profile == null) throw new QuestException(QuestException.Type.PLAYER_NOT_FOUND);

        for (QuestProgress progress : new ArrayList<>(profile.activeQuests.values())) {
            if (progress.questSnapshot == null
                || progress.currentStepIndex >= progress.questSnapshot.steps.size()) {
                continue;
            }
            tryAdvance(profile, progress, player, triggerId);
        }
    }

    public void startQuest(UUID playerUuid, String questId) throws QuestException {
        PlayerProfile profile = playerProfiles.get(playerUuid);
        if (profile == null) throw new QuestException(QuestException.Type.PLAYER_NOT_FOUND);
        Quest quest = quests.get(questId);
        if (quest == null) throw new QuestException(QuestException.Type.QUEST_NOT_FOUND);
        if (!quest.isPublished() && !isDebugMode(playerUuid)) throw new QuestException(QuestException.Type.QUEST_NOT_PUBLISHED);
        if (profile.activeQuests.containsKey(questId)) throw new QuestException(QuestException.Type.QUEST_ALREADY_STARTED);
        if (!profile.activeQuests.isEmpty()) throw new QuestException(QuestException.Type.QUEST_ONLY_ONE_AT_A_TIME);

        QuestProgress progress = new QuestProgress();
        progress.questId = questId;
        progress.questSnapshot = quest;
        progress.currentStepIndex = 0;
        progress.questStartTime = System.currentTimeMillis();
        progress.stepStartTimes = new HashMap<>();
        progress.stepStartTimes.put(0, progress.questStartTime);
        progress.resetStepStates();

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
            Quest quest = progress.questSnapshot != null ? progress.questSnapshot : quests.get(progress.questId);
            if (quest != null) {
                callback.onQuestAborted(this, playerUuid, quest);
            }
        }
    }

    private void advanceQuestStep(PlayerProfile profile, QuestProgress progress, Quest quest) {
        long now = System.currentTimeMillis();
        progress.currentStepIndex++;
        callback.onStepCompleted(this, profile.playerUuid, quest, progress);

        if (progress.currentStepIndex >= quest.steps.size()) {
            profile.activeQuests.remove(progress.questId);

            QuestCompletionData completionData = new QuestCompletionData();
            completionData.playerUuid = profile.playerUuid;
            completionData.questId = quest.id;
            completionData.completionTime = now;
            completionData.durationMillis = now - progress.questStartTime;
            completionData.questPoints = quest.questPoints;

            completionData.stepDurations = new HashMap<>();
            long lastTimestamp = progress.questStartTime;
            for (int i = 0; i < quest.steps.size(); i++) {
                long stepEndTimestamp = progress.stepStartTimes.getOrDefault(i + 1, now);
                long stepStartTimestamp = progress.stepStartTimes.getOrDefault(i, lastTimestamp);
                completionData.stepDurations.put(i, stepEndTimestamp - stepStartTimestamp);
                lastTimestamp = stepEndTimestamp;
            }

            boolean debug = isDebugMode(profile.playerUuid);
            if (!debug) {
                profile.totalQuestPoints += quest.questPoints;
                profile.totalQuestCompletions += 1;
            }

            callback.onQuestCompleted(this, profile.playerUuid, quest, completionData);

            if (!debug) {
                try {
                    databaseManager.addQuestCompletion(profile.playerUuid, quest, completionData);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            progress.resetStepStates();
            progress.stepStartTimes.put(progress.currentStepIndex, now);
        }
    }

    private void tryAdvance(PlayerProfile profile, QuestProgress progress, ServerPlayer player, String triggerId) {
        if (progress.expandedCurrentStep == null) {
            Step originalStep = progress.questSnapshot.steps.get(progress.currentStepIndex);
            progress.expandedCurrentStep = originalStep.expand();
            progress.expandedDefaultCriteria = progress.questSnapshot.defaultCriteria != null
                ? progress.questSnapshot.defaultCriteria.expand() : null;
        }

        Step currentStep = progress.expandedCurrentStep;
        Step defaultCriteria = progress.expandedDefaultCriteria;

        CriterionContext criteriaCtx =
            new CriterionContext(progress.criteriaState, "");
        CriterionContext failureCtx =
            new CriterionContext(progress.failureCriteriaState, "");
        CriterionContext defaultFailCtx =
            new CriterionContext(progress.defaultFailureCriteriaState, "");

        if (triggerId != null) {
            currentStep.propagateManualTrigger(triggerId, criteriaCtx, failureCtx);
            if (defaultCriteria != null && defaultCriteria.failureCriteria != null) {
                defaultCriteria.failureCriteria.propagateManualTrigger(triggerId, defaultFailCtx);
            }
        }

        if (!isDebugMode(profile.playerUuid)) {
            Optional<Component> failed = currentStep.evaluateFailure(
                player, failureCtx, defaultCriteria, defaultFailCtx);
            if (failed.isPresent()) {
                profile.activeQuests.remove(progress.questId);
                callback.onQuestFailed(this, profile.playerUuid, progress.questSnapshot, failed.get());
                return;
            }
        }

        if (currentStep.evaluate(player, criteriaCtx)) {
            advanceQuestStep(profile, progress, progress.questSnapshot);
        }
    }
}
