package cn.zbx1425.nquestbot.data;

import cn.zbx1425.nquestbot.data.quest.Quest;
import cn.zbx1425.nquestbot.data.quest.QuestCompletionData;
import cn.zbx1425.nquestbot.data.quest.QuestProgress;

import java.util.UUID;

public interface IQuestCallbacks {

    void onQuestStarted(QuestDispatcher questEngine, UUID playerUuid, Quest quest);

    void onStepCompleted(QuestDispatcher questEngine, UUID playerUuid, Quest quest, QuestProgress progress);

    void onQuestCompleted(QuestDispatcher questEngine, UUID playerUuid, Quest quest, QuestCompletionData data);

    void onQuestAborted(QuestDispatcher questEngine, UUID playerUuid, Quest quest);

}
