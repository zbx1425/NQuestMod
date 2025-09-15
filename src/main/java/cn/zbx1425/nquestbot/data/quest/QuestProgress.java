package cn.zbx1425.nquestbot.data.quest;

import java.util.Map;
import java.util.UUID;

public class QuestProgress {

    public String questId;
    public int currentStepIndex;
    public long questStartTime;
    public Map<Integer, Long> stepStartTimes;
}
