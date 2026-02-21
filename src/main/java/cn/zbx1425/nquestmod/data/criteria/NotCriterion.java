package cn.zbx1425.nquestmod.data.criteria;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class NotCriterion implements Criterion {

    protected Criterion base;
    public String description;

    public NotCriterion(Criterion base, String description) {
        this.base = base;
        this.description = description;
    }

    @Override
    public boolean evaluate(ServerPlayer player, CriterionContext ctx) {
        return !base.evaluate(player, ctx.child("b"));
    }

    @Override
    public Component getDisplayRepr() {
        return Component.literal(description);
    }

    @Override
    public void propagateManualTrigger(String triggerId, CriterionContext ctx) {
        base.propagateManualTrigger(triggerId, ctx.child("b"));
    }

    @Override
    public Criterion expand() {
        return new NotCriterion(base.expand(), description);
    }
}
