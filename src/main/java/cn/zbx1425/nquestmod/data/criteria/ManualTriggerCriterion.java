package cn.zbx1425.nquestmod.data.criteria;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ManualTriggerCriterion implements Criterion {

    public String id;
    public String description;
    protected transient boolean isTriggered = false;

    public ManualTriggerCriterion(String id, String description) {
        this.id = id;
        this.description = description;
    }

    public ManualTriggerCriterion(ManualTriggerCriterion singleton) {
        this.id = singleton.id;
        this.description = singleton.description;
        this.isTriggered = false;
    }

    @Override
    public boolean isFulfilled(ServerPlayer player) {
        return isTriggered;
    }

    @Override
    public Component getDisplayRepr() {
        return Component.literal(description);
    }

    @Override
    public Criterion createStatefulInstance() {
        return new ManualTriggerCriterion(this);
    }

    @Override
    public void propagateManualTrigger(String triggerId) {
        if (this.id.equals(triggerId)) {
            isTriggered = true;
        }
    }
}
