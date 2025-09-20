package cn.zbx1425.nquestmod.data.criteria;

import net.minecraft.server.level.ServerPlayer;

public class RisingEdgeAndConditionCriterion implements Criterion {

    protected final Criterion triggerCriterion;
    protected final Criterion conditionCriterion;
    protected final Criterion descriptionSupplier;

    protected transient boolean wasTriggerFulfilled = false;

    public RisingEdgeAndConditionCriterion(Criterion triggerCriterion, Criterion conditionCriterion, Criterion descriptionSupplier) {
        this.triggerCriterion = triggerCriterion;
        this.conditionCriterion = conditionCriterion;
        this.descriptionSupplier = descriptionSupplier;
    }

    public RisingEdgeAndConditionCriterion(RisingEdgeAndConditionCriterion singleton) {
        this.triggerCriterion = singleton.triggerCriterion.createStatefulInstance();
        this.conditionCriterion = singleton.conditionCriterion.createStatefulInstance();
        this.descriptionSupplier = singleton.descriptionSupplier.createStatefulInstance();
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
        return descriptionSupplier.getDisplayRepr();
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
