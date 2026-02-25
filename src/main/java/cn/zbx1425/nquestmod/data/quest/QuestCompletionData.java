package cn.zbx1425.nquestmod.data.quest;

import java.util.*;

public class QuestCompletionData {

    public UUID playerUuid;
    public String playerName;
    public String questId;
    public String questName;
    public long completionTime;
    public long durationMillis;
    public int questPoints;
    public Map<Integer, StepDetail> stepDetails;

    public static class StepDetail {
        public long durationMillis;
        public String description;
        public List<String> linesRidden;

        public StepDetail() {
            this.linesRidden = new ArrayList<>();
        }

        public StepDetail(long durationMillis, String description, List<String> linesRidden) {
            this.durationMillis = durationMillis;
            this.description = description;
            this.linesRidden = linesRidden != null ? linesRidden : new ArrayList<>();
        }
    }
}
