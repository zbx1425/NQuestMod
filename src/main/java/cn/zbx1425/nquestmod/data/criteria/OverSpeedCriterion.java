package cn.zbx1425.nquestmod.data.criteria;

import cn.zbx1425.nquestmod.NQuestMod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class OverSpeedCriterion implements Criterion {

    public double maxSpeedMps;

    private transient Vec3 lastPlayerPos = null;
    private transient int lastTick;

    public OverSpeedCriterion(double maxSpeedMps) {
        this.maxSpeedMps = maxSpeedMps;
    }

    public OverSpeedCriterion(OverSpeedCriterion singleton) {
        this.maxSpeedMps = singleton.maxSpeedMps;
        this.lastPlayerPos = null;
        this.lastTick = 0;
    }

    @Override
    public boolean isFulfilled(ServerPlayer player) {
        if (lastPlayerPos == null) {
            lastPlayerPos = player.position();
            lastTick = player.getServer().getTickCount();
        }
        double distSqr = lastPlayerPos.distanceToSqr(player.position());
        double deltaT = (player.getServer().getTickCount() - lastTick) / 20.0;
        lastPlayerPos = player.position();
        lastTick = player.getServer().getTickCount();
        return distSqr / (deltaT * deltaT) > maxSpeedMps * maxSpeedMps;
    }

    @Override
    public Component getDisplayRepr() {
        return Component.literal("Move faster than " + String.format("%.1f", maxSpeedMps) + " m/s");
    }

    @Override
    public Criterion createStatefulInstance() {
        return new OverSpeedCriterion(this);
    }
}
