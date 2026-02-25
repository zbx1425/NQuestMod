package cn.zbx1425.nquestmod.data.criteria.mtr;

import cn.zbx1425.nquestmod.data.criteria.Criterion;
import cn.zbx1425.nquestmod.data.criteria.CriterionContext;
import cn.zbx1425.nquestmod.interop.TscStatus;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class StationStopCriterion implements Criterion {

    // Evaluates true if the train is not riding a train,
    //   or is riding a train but the train has its doors open at the station.
    // In other words, the player is considered to have "stopped" at the station.

    @Override
    public boolean evaluate(ServerPlayer player, CriterionContext ctx) {
        TscStatus.ClientState state = TscStatus.getClientState(player);
        if (state == null) return false;

        return !state.trainDoorClosed();
    }

    @Override
    public Component getDisplayRepr() {
        return Component.literal("Stop at the station");
    }
}
