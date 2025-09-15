package cn.zbx1425.nquestbot.data.criteria;

import net.minecraft.server.level.ServerPlayer;
import cn.zbx1425.nquestbot.data.persistent.Vec3d;
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
        if (playerStatus.position == null) return false;
        return playerStatus.position.isWithin(min, max);
    }

    @Override
    public Component getDisplayRepr() {
        return Component.literal(description);
    }
}
