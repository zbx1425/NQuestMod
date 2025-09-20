package cn.zbx1425.nquestmod.data.criteria;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class LatchingCriterion implements Criterion {

    protected Criterion baseCriterion;
    protected transient boolean onceFulfilled = false;

    public LatchingCriterion(Criterion baseCriterion) {
        this.baseCriterion = baseCriterion;
    }

    public LatchingCriterion(LatchingCriterion singleton) {
        this.baseCriterion = singleton.baseCriterion.createStatefulInstance();
        this.onceFulfilled = false;
    }

    @Override
    public boolean isFulfilled(ServerPlayer player) {
        if (onceFulfilled) return true;
        if (baseCriterion.isFulfilled(player)) {
            onceFulfilled = true;
            return true;
        }
        return false;
    }

    @Override
    public Component getDisplayRepr() {
        return baseCriterion.getDisplayRepr();
    }

    @Override
    public Criterion createStatefulInstance() {
        return new LatchingCriterion(this);
    }

    @Override
    public void propagateManualTrigger(String triggerId) {
        baseCriterion.propagateManualTrigger(triggerId);
    }
}
