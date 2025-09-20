package cn.zbx1425.nquestmod.data.criteria;

import net.minecraft.server.level.ServerPlayer;
import cn.zbx1425.nquestmod.data.persistent.Vec3d;
import net.minecraft.network.chat.Component;

public class InBoundsCriterion implements Criterion {

    public Vec3d min;
    public Vec3d max;
    public String description;

    public InBoundsCriterion(Vec3d min, Vec3d max, String description) {
        this.min = min;
        this.max = max;
        this.description = description;
    }

    @Override
    public boolean isFulfilled(ServerPlayer player) {
        return new Vec3d(player.getX(), player.getY(), player.getZ()).isWithin(min, max);
    }

    @Override
    public Component getDisplayRepr() {
        return Component.literal(description);
    }
}
