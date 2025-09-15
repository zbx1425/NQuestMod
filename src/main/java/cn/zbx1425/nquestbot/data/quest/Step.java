package cn.zbx1425.nquestbot.data.quest;

import cn.zbx1425.nquestbot.data.criteria.AndCriterion;
import cn.zbx1425.nquestbot.data.criteria.Criterion;
import cn.zbx1425.nquestbot.data.criteria.LatchingCriterion;

import java.util.List;
import java.util.stream.Collectors;

public class Step {

    public List<Criterion> criteria;

    private transient Criterion sumCriterion;

    public Criterion getProductCriteria() {
        if (sumCriterion == null) {
            sumCriterion = new AndCriterion(criteria.stream().map(LatchingCriterion::new).collect(Collectors.toList()));
        }
        return sumCriterion;
    }

    public Criterion createStatefulCriteria() {
        return sumCriterion.createStatefulInstance();
    }
}
