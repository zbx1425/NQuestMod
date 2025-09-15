package cn.zbx1425.nquestbot.data.criteria;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class LatchingCriterion implements Criterion {

    protected Criterion notLatchingCriterion;
    protected transient boolean onceFulfilled = false;

    public LatchingCriterion(Criterion notLatchingCriterion) {
        this.notLatchingCriterion = notLatchingCriterion;
    }

    public LatchingCriterion(LatchingCriterion singleton) {
        this.notLatchingCriterion = singleton.notLatchingCriterion.createStatefulInstance();
        this.onceFulfilled = false;
    }

    @Override
    public boolean isFulfilled(ServerPlayer player) {
        if (onceFulfilled) return true;
        if (notLatchingCriterion.isFulfilled(player)) {
            onceFulfilled = true;
            return true;
        }
        return false;
    }

    @Override
    public Component getDisplayRepr() {
        return notLatchingCriterion.getDisplayRepr();
    }

    @Override
    public Criterion createStatefulInstance() {
        return new LatchingCriterion(this);
    }

    @Override
    public void propagateManualTrigger(String triggerId) {
        notLatchingCriterion.propagateManualTrigger(triggerId);
    }
}
