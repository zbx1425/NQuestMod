package cn.zbx1425.nquestmod.data.quest;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Quest {

    public String id;
    public String name;
    public String description;
    public String category;
    public String tier;
    public int questPoints;

    public Step defaultCriteria; // Optional
    public List<Step> steps;

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
            if (step.criteria != null) step.criteria.createStatefulInstance().getDisplayRepr();
            if (step.failureCriteria != null) step.failureCriteria.createStatefulInstance().getDisplayRepr();
        }
        if (defaultCriteria != null && defaultCriteria.failureCriteria != null) {
            defaultCriteria.failureCriteria.createStatefulInstance().getDisplayRepr();
        }
    }
}
