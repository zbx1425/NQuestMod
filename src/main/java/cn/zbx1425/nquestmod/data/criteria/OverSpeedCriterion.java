package cn.zbx1425.nquestmod.data.criteria;

import cn.zbx1425.nquestmod.interop.TscStatus;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class OverSpeedCriterion implements Criterion {

    public double maxSpeedMps;

    public OverSpeedCriterion(double maxSpeedMps) {
        this.maxSpeedMps = maxSpeedMps;
    }

    @Override
    public boolean evaluate(ServerPlayer player, CriterionContext ctx) {
        return TscStatus.getClientState(player).trainSpeedTargetMps() > maxSpeedMps;
    }

    @Override
    public Component getDisplayRepr() {
        return Component.literal("Move faster than " + String.format("%.1f", maxSpeedMps * 3600 / 1000) + " km/h");
    }
}
