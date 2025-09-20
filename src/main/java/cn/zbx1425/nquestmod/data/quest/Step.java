package cn.zbx1425.nquestmod.data.quest;

import cn.zbx1425.nquestmod.data.criteria.Criterion;

public class Step {

    public Criterion criteria;

    public Criterion createStatefulCriteria() {
        return criteria.createStatefulInstance();
    }
}
