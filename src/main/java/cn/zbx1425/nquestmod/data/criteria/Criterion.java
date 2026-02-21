package cn.zbx1425.nquestmod.data.criteria;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public interface Criterion {

    boolean evaluate(ServerPlayer player, CriterionContext ctx);

    Component getDisplayRepr();

    default void propagateManualTrigger(String triggerId, CriterionContext ctx) {

    }

    default Criterion expand() {
        return this;
    }
}
