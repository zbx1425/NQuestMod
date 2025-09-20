package cn.zbx1425.nquestmod.data.ranking;

import java.util.UUID;

public class PlayerCompletionsEntry {

    public UUID playerUuid;
    public int totalCompletions;

    public PlayerCompletionsEntry(UUID playerUuid, int totalCompletions) {
        this.playerUuid = playerUuid;
        this.totalCompletions = totalCompletions;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public int getTotalCompletions() {
        return totalCompletions;
    }
}
