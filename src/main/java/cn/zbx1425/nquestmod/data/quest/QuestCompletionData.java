package cn.zbx1425.nquestmod.data.quest;

import java.util.Map;
import java.util.UUID;

public class QuestCompletionData {

    public UUID playerUuid;
    public String playerName;
    public String questId;
    public String questName;
    public long completionTime;
    public long durationMillis;
    public int questPoints;
    public Map<Integer, Long> stepDurations;
}
