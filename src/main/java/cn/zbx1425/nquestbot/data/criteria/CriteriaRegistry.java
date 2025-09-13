package cn.zbx1425.nquestbot.data.criteria;

import cn.zbx1425.nquestbot.RuntimeTypeAdapterFactory;
import cn.zbx1425.nquestbot.data.criteria.mtr.RideLineToStationCriterion;
import cn.zbx1425.nquestbot.data.criteria.mtr.RideLineCriterion;
import cn.zbx1425.nquestbot.data.criteria.mtr.RideToStationCriterion;
import cn.zbx1425.nquestbot.data.criteria.mtr.VisitStationCriterion;

public class CriteriaRegistry {

    public static RuntimeTypeAdapterFactory<Criterion> getFactory() {
        return RuntimeTypeAdapterFactory.of(Criterion.class, "type")
                .registerSubtype(InBoundsCriterion.class)
                .registerSubtype(RideLineToStationCriterion.class)
                .registerSubtype(RideLineCriterion.class)
                .registerSubtype(RideToStationCriterion.class)
                .registerSubtype(VisitStationCriterion.class);
    }
}
