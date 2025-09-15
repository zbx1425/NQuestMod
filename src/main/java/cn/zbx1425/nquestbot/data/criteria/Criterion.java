package cn.zbx1425.nquestbot.data.criteria;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public interface Criterion {

    boolean isFulfilled(ServerPlayer player);

    Component getDisplayRepr();
}
