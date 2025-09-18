package cn.zbx1425.nquestbot.data.ranking;

import java.util.UUID;

public class QuestTimeEntry {
    public UUID playerUuid;
    public String playerName;
    public long durationMillis;

    public QuestTimeEntry(UUID playerUuid, String playerName, long durationMillis) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.durationMillis = durationMillis;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public long getDurationMillis() {
        return durationMillis;
    }
}
