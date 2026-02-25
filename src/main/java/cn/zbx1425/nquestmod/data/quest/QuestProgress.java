package cn.zbx1425.nquestmod.data.quest;

import java.util.*;

public class QuestProgress {

    public String questId;
    public int currentStepIndex;
    public long questStartTime;

    /** Start time (ms) per step, after last disconnect. */
    public Map<Integer, Long> stepStartTimes;
    /** Accumulated play time (ms) per step, excluding offline periods. Updated on disconnect. */
    public Map<Integer, Long> stepAccumulatedMillis;
    /** Lines ridden per step (insertion-ordered, no duplicates). */
    public Map<Integer, List<String>> stepLinesRidden;

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

    /** Ensures maps are non-null (guards against old serialized data missing these fields). */
    public void ensureInitialized() {
        if (stepStartTimes == null) stepStartTimes = new HashMap<>();
        if (stepAccumulatedMillis == null) stepAccumulatedMillis = new HashMap<>();
        if (stepLinesRidden == null) stepLinesRidden = new HashMap<>();
    }

    /** Pause the current step timer, adding elapsed time to accumulated millis. */
    public void pauseCurrentStep() {
        ensureInitialized();
        if (currentStepIndex < (questSnapshot != null ? questSnapshot.steps.size() : Integer.MAX_VALUE)) {
            long now = System.currentTimeMillis();
            long start = stepStartTimes.getOrDefault(currentStepIndex, questStartTime);
            stepAccumulatedMillis.merge(currentStepIndex, now - start, Long::sum);
        }
    }

    /** Resume the current step timer by resetting its start time to now. */
    public void resumeCurrentStep() {
        ensureInitialized();
        stepStartTimes.put(currentStepIndex, System.currentTimeMillis());
    }

    /** Get the effective duration for a step, accounting for accumulated offline-adjusted time. */
    public long getStepDuration(int stepIndex, long stepEndTimestamp) {
        ensureInitialized();
        long accumulated = stepAccumulatedMillis.getOrDefault(stepIndex, 0L);
        long start = stepStartTimes.getOrDefault(stepIndex, questStartTime);
        return accumulated + (stepEndTimestamp - start);
    }
}
