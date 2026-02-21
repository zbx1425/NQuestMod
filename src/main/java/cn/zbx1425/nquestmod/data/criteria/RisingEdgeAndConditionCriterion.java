package cn.zbx1425.nquestmod.data.criteria;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class RisingEdgeAndConditionCriterion implements Criterion {

    protected Criterion triggerCriteria;
    protected Criterion conditionCriteria;

    public RisingEdgeAndConditionCriterion(Criterion triggerCriteria, Criterion conditionCriteria) {
        this.triggerCriteria = triggerCriteria;
        this.conditionCriteria = conditionCriteria;
    }

    @Override
    public boolean evaluate(ServerPlayer player, CriterionContext ctx) {
        if (!triggerCriteria.evaluate(player, ctx.child("t"))) {
            ctx.setBoolean("wasTriggerFulfilled", false);
            return false;
        }
        if (!ctx.getBoolean("wasTriggerFulfilled", true)) {
            ctx.setBoolean("wasTriggerFulfilled", true);
            return conditionCriteria.evaluate(player, ctx.child("c"));
        } else {
            return false;
        }
    }

    @Override
    public Component getDisplayRepr() {
        return triggerCriteria.getDisplayRepr().copy()
            .append(Component.literal(" while: ").withStyle(ChatFormatting.GRAY))
            .append(conditionCriteria.getDisplayRepr());
    }

    @Override
    public void propagateManualTrigger(String triggerId, CriterionContext ctx) {
        triggerCriteria.propagateManualTrigger(triggerId, ctx.child("t"));
        conditionCriteria.propagateManualTrigger(triggerId, ctx.child("c"));
    }

    @Override
    public Criterion expand() {
        return new RisingEdgeAndConditionCriterion(
            triggerCriteria.expand(), conditionCriteria.expand());
    }
}
