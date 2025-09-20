package cn.zbx1425.nquestmod.data.ranking;

import java.util.UUID;

public class QuestTimeEntry {
    public UUID playerUuid;
    public String questId;
    public long durationMillis;

    public QuestTimeEntry(UUID playerUuid, String questId, long durationMillis) {
        this.playerUuid = playerUuid;
        this.questId = questId;
        this.durationMillis = durationMillis;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getQuestId() {
        return questId;
    }

    public long getDurationMillis() {
        return durationMillis;
    }
}
