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
    public List<Step> steps;
    public int questPoints;
    public String category;
    public String tier;

    public List<Component> formatDescription() {
        return Stream.of(description.split("\n"))
            .map(line -> Component.literal(line).withStyle(ChatFormatting.GRAY))
            .collect(Collectors.toList());
    }
}
