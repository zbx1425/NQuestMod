package cn.zbx1425.nquestmod.data.criteria;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class LatchingCriterion implements Criterion {

    protected Criterion base;

    public LatchingCriterion(Criterion base) {
        this.base = base;
    }

    @Override
    public boolean evaluate(ServerPlayer player, CriterionContext ctx) {
        if (ctx.getBoolean("fulfilled", false)) return true;
        if (base.evaluate(player, ctx.child("b"))) {
            ctx.setBoolean("fulfilled", true);
            return true;
        }
        return false;
    }

    @Override
    public Component getDisplayRepr() {
        return base.getDisplayRepr();
    }

    @Override
    public void propagateManualTrigger(String triggerId, CriterionContext ctx) {
        base.propagateManualTrigger(triggerId, ctx.child("b"));
    }

    @Override
    public Criterion expand() {
        return new LatchingCriterion(base.expand());
    }
}
