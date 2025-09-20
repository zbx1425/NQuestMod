package cn.zbx1425.nquestmod.data.ranking;

import java.util.UUID;

public class PlayerQPEntry {
    public UUID playerUuid;
    public int totalQP;

    public PlayerQPEntry(UUID playerUuid, int totalQP) {
        this.playerUuid = playerUuid;
        this.totalQP = totalQP;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public int getTotalQP() {
        return totalQP;
    }
}
