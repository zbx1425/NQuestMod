package cn.zbx1425.nquestmod.data.criteria;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class NotCriterion implements Criterion {

    protected Criterion baseCriterion;
    public String description;

    public NotCriterion(Criterion baseCriterion, String description) {
        this.baseCriterion = baseCriterion;
        this.description = description;
    }

    public NotCriterion(NotCriterion singleton) {
        this.baseCriterion = singleton.baseCriterion.createStatefulInstance();
        this.description = singleton.description;
    }

    @Override
    public boolean isFulfilled(ServerPlayer player) {
        return !baseCriterion.isFulfilled(player);
    }

    @Override
    public Component getDisplayRepr() {
        return Component.literal(description);
    }

    @Override
    public Criterion createStatefulInstance() {
        return new NotCriterion(this);
    }

    @Override
    public void propagateManualTrigger(String triggerId) {
        baseCriterion.propagateManualTrigger(triggerId);
    }
}
