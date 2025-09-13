package cn.zbx1425.nquestbot;

import cn.zbx1425.nquestbot.data.platform.IQuestCallbacks;
import cn.zbx1425.nquestbot.data.quest.Quest;
import cn.zbx1425.nquestbot.data.quest.QuestCompletionData;
import cn.zbx1425.nquestbot.data.quest.Step;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

public class QuestNotifications implements IQuestCallbacks {

    private MinecraftServer server;

    public QuestNotifications(MinecraftServer server) {
        this.server = server;
    }

    public void onPlayerJoin(UUID playerUuid) {

    }

    @Override
    public void onStepCompleted(UUID playerUuid, Step completedStep) {

    }

    @Override
    public void onQuestCompleted(UUID playerUuid, Quest quest, QuestCompletionData data) {

    }
}
