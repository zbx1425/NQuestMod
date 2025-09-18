package cn.zbx1425.nquestbot.data.ranking;

import java.util.UUID;

public class PlayerQPEntry {
    public UUID playerUuid;
    public String playerName;
    public int totalQP;

    public PlayerQPEntry(UUID playerUuid, String playerName, int totalQP) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.totalQP = totalQP;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getTotalQP() {
        return totalQP;
    }
}
