package cn.zbx1425.nquestbot.data.platform;

import cn.zbx1425.nquestbot.data.quest.Quest;
import cn.zbx1425.nquestbot.data.quest.Step;
import cn.zbx1425.nquestbot.data.quest.PlayerProfile;
import cn.zbx1425.nquestbot.data.quest.QuestCompletionData;

import java.util.UUID;

public interface IQuestCallbacks {

    void onStepCompleted(UUID playerUuid, Step completedStep);

    void onQuestCompleted(UUID playerUuid, Quest quest, QuestCompletionData data);

}
