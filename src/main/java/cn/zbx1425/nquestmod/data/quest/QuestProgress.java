package cn.zbx1425.nquestmod.data.quest;

import java.util.Map;

public class QuestProgress {

    public String questId;
    public int currentStepIndex;
    public long questStartTime;
    public Map<Integer, Long> stepStartTimes;

    public transient Step defaultCriteriaStateful;
    public transient Step currentStepStateful;
}
