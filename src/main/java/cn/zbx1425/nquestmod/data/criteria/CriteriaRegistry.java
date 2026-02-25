package cn.zbx1425.nquestmod.data.criteria;

import cn.zbx1425.nquestmod.data.RuntimeTypeAdapterFactory;
import cn.zbx1425.nquestmod.data.criteria.mtr.*;

public class CriteriaRegistry {

    public static RuntimeTypeAdapterFactory<Criterion> getFactory() {
        return RuntimeTypeAdapterFactory.of(Criterion.class, "type")
                .registerSubtype(ConstantCriterion.class)
                .registerSubtype(Descriptor.class)
                .registerSubtype(ManualTriggerCriterion.class)
                .registerSubtype(AndCriterion.class)
                .registerSubtype(OrCriterion.class)
                .registerSubtype(NotCriterion.class)
                .registerSubtype(LatchingCriterion.class)
                .registerSubtype(RisingEdgeAndConditionCriterion.class)

                .registerSubtype(InBoundsCriterion.class)
                .registerSubtype(OverSpeedCriterion.class)
                .registerSubtype(TeleportDetectCriterion.class)
                .registerSubtype(InStationAreaCriterion.class)
                .registerSubtype(RideLineToStationCriterion.class)
                .registerSubtype(RideLineCriterion.class)
                .registerSubtype(RideToStationCriterion.class)
                .registerSubtype(VisitStationCriterion.class)
                .registerSubtype(StationStopCriterion.class)
                ;
    }
}
