package cn.zbx1425.nquestmod.data.criteria;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class RisingEdgeAndConditionCriterion implements Criterion {

    // Latches when triggerCriteria changes from false -> true, AND conditionCriteria must be true at that moment.
    // Once latched, it will return true until triggerCriteria becomes false again.
    // If conditionCriteria is false at the moment of rising edge, it will not latch and will return false until the next rising edge.

    protected Criterion triggerCriteria;
    protected Criterion conditionCriteria;

    public RisingEdgeAndConditionCriterion(Criterion triggerCriteria, Criterion conditionCriteria) {
        this.triggerCriteria = triggerCriteria;
        this.conditionCriteria = conditionCriteria;
    }

    @Override
    public boolean evaluate(ServerPlayer player, CriterionContext ctx) {
        boolean currentTrigger = triggerCriteria.evaluate(player, ctx.child("t"));

        boolean wasTriggerActive = ctx.getBoolean("wasTriggerActive", false);
        boolean isLatched = ctx.getBoolean("isLatched", false);

        if (currentTrigger) {
            ctx.setBoolean("wasTriggerActive", true);
            if (!wasTriggerActive) {
                // Rising edge
                boolean conditionMet = conditionCriteria.evaluate(player, ctx.child("c"));
                ctx.setBoolean("isLatched", conditionMet);
                return conditionMet;
            } else {
                // Trigger is holding high
                return isLatched;
            }
        } else {
            // Reset
            ctx.setBoolean("wasTriggerActive", false);
            ctx.setBoolean("isLatched", false);
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
