package cn.zbx1425.nquestmod.data.quest;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Quest {

    public enum QuestStatus { PRIVATE, STAGING, PUBLIC }

    public String id;
    public String name;
    public String description;
    public String category;
    public String tier;
    public int questPoints;
    public QuestStatus status;
    public List<String> creators;

    public Step defaultCriteria; // Optional
    public List<Step> steps;

    public QuestStatus getEffectiveStatus() {
        return status != null ? status : QuestStatus.PUBLIC;
    }

    public boolean isVisibleTo(UUID playerUuid, boolean debugMode, boolean hasPermLevel2) {
        return switch (getEffectiveStatus()) {
            case PUBLIC -> true;
            case STAGING -> debugMode && hasPermLevel2;
            case PRIVATE -> debugMode && hasPermLevel2
                    && creators != null && creators.contains(playerUuid.toString());
        };
    }

    public List<Component> formatDescription() {
        return Stream.of(description.split("\n"))
            .map(line -> Component.literal(line).withStyle(ChatFormatting.GRAY))
            .collect(Collectors.toList());
    }

    // Some criterion's getDisplayRepr() has async loading (e.g. MTR station names),
    // we call this method after loading quests to trigger fetching those names.
    // Maybe there can be some better way to do this?
    public void preTouchDescriptions() {
        for (Step step : steps) {
            Step expanded = step.expand();
            if (expanded.criteria != null) expanded.criteria.getDisplayRepr();
            if (expanded.failureCriteria != null) expanded.failureCriteria.getDisplayRepr();
        }
        if (defaultCriteria != null) {
            Step expandedDefault = defaultCriteria.expand();
            if (expandedDefault.failureCriteria != null) expandedDefault.failureCriteria.getDisplayRepr();
        }
    }
}
