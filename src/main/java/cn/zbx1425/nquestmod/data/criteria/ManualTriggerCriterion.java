package cn.zbx1425.nquestmod.data.criteria;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ManualTriggerCriterion implements Criterion {

    public String id;
    public String description;

    public ManualTriggerCriterion(String id, String description) {
        this.id = id;
        this.description = description;
    }

    @Override
    public boolean evaluate(ServerPlayer player, CriterionContext ctx) {
        return ctx.getBoolean("triggered", false);
    }

    @Override
    public Component getDisplayRepr() {
        return Component.literal(description);
    }

    @Override
    public void propagateManualTrigger(String triggerId, CriterionContext ctx) {
        if (this.id.equals(triggerId)) {
            ctx.setBoolean("triggered", true);
        }
    }
}
