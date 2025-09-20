package cn.zbx1425.nquestmod.data.criteria;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ConstantCriterion implements Criterion {

    public boolean value;
    public String description;

    public ConstantCriterion(boolean value, String description) {
        this.value = value;
        this.description = description;
    }

    @Override
    public boolean isFulfilled(ServerPlayer player) {
        return value;
    }

    @Override
    public Component getDisplayRepr() {
        return Component.literal(description);
    }
}
