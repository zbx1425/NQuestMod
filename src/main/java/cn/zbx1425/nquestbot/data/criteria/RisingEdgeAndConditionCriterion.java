package cn.zbx1425.nquestbot.data.criteria;

import net.minecraft.server.level.ServerPlayer;

public class RisingEdgeAndConditionCriterion implements Criterion {

    protected final Criterion triggerCriterion;
    protected final Criterion conditionCriterion;
    protected final Criterion serializableCriterion;

    protected transient boolean wasTriggerFulfilled = false;

    public RisingEdgeAndConditionCriterion(Criterion triggerCriterion, Criterion conditionCriterion, Criterion serializableCriterion) {
        this.triggerCriterion = triggerCriterion;
        this.conditionCriterion = conditionCriterion;
        this.serializableCriterion = serializableCriterion;
    }

    public RisingEdgeAndConditionCriterion(RisingEdgeAndConditionCriterion singleton) {
        this.triggerCriterion = singleton.triggerCriterion.createStatefulInstance();
        this.conditionCriterion = singleton.conditionCriterion.createStatefulInstance();
        this.serializableCriterion = singleton.serializableCriterion.createStatefulInstance();
        this.wasTriggerFulfilled = false;
    }

    @Override
    public boolean isFulfilled(ServerPlayer player) {
        if (!triggerCriterion.isFulfilled(player)) {
            wasTriggerFulfilled = false;
            return false;
        }
        if (!wasTriggerFulfilled) {
            wasTriggerFulfilled = true;
            return conditionCriterion.isFulfilled(player);
        } else {
            return false;
        }
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayRepr() {
        return serializableCriterion.getDisplayRepr();
    }

    @Override
    public void propagateManualTrigger(String triggerId) {
        triggerCriterion.propagateManualTrigger(triggerId);
        conditionCriterion.propagateManualTrigger(triggerId);
    }

    @Override
    public Criterion createStatefulInstance() {
        return new RisingEdgeAndConditionCriterion(this);
    }
}
