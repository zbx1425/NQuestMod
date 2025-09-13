package cn.zbx1425.nquestbot.data.quest;

import cn.zbx1425.nquestbot.data.criteria.Criterion;

import java.util.List;
import java.util.UUID;

public class Step {

    public UUID id;
    public String name;
    public List<Criterion> criteria;
    public boolean needsManualTrigger;
}
