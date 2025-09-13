package cn.zbx1425.nquestbot.data.quest;

import java.util.List;
import java.util.UUID;

public class Quest {

    public UUID id;
    public String name;
    public String description;
    public List<Step> steps;
    public int questPoints;
}
