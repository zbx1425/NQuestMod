package cn.zbx1425.nquestmod.data.quest;

import java.util.Map;

public class QuestProgress {

    public String questId;
    public int currentStepIndex;
    public long questStartTime;
    public Map<Integer, Long> stepStartTimes;

    public Quest questSnapshot;

    public StepState criteriaState;
    public StepState failureCriteriaState;
    public StepState defaultFailureCriteriaState;

    public transient Step expandedCurrentStep;
    public transient Step expandedDefaultCriteria;

    public void resetStepStates() {
        this.criteriaState = new StepState();
        this.failureCriteriaState = new StepState();
        this.defaultFailureCriteriaState = new StepState();
        this.expandedCurrentStep = null;
        this.expandedDefaultCriteria = null;
    }
}
