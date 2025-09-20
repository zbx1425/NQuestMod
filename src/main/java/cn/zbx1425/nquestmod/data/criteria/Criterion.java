package cn.zbx1425.nquestmod.data.criteria;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public interface Criterion {

    boolean isFulfilled(ServerPlayer player);

    Component getDisplayRepr();

    default Criterion createStatefulInstance() {
        return this; // Default to stateless
    }

    default void propagateManualTrigger(String triggerId) {

    }
}
