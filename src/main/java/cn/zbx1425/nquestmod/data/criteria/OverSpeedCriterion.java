package cn.zbx1425.nquestmod.data.criteria;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class OverSpeedCriterion implements Criterion {

    public double maxSpeedMps;

    public OverSpeedCriterion(double maxSpeedMps) {
        this.maxSpeedMps = maxSpeedMps;
    }

    @Override
    public boolean evaluate(ServerPlayer player, CriterionContext ctx) {
        double lastX = ctx.getDouble("lastX", Double.NaN);
        double lastY = ctx.getDouble("lastY", Double.NaN);
        double lastZ = ctx.getDouble("lastZ", Double.NaN);
        int lastTick = ctx.getInt("lastTick", 0);
        int currentTick = player.getServer().getTickCount();

        ctx.setDouble("lastX", player.getX());
        ctx.setDouble("lastY", player.getY());
        ctx.setDouble("lastZ", player.getZ());
        ctx.setInt("lastTick", currentTick);

        if (Double.isNaN(lastX)) return false;

        double dx = player.getX() - lastX;
        double dy = player.getY() - lastY;
        double dz = player.getZ() - lastZ;
        double distSqr = dx * dx + dy * dy + dz * dz;
        double deltaT = (currentTick - lastTick) / 20.0;
        if (deltaT <= 0) return false;
        return distSqr / (deltaT * deltaT) > maxSpeedMps * maxSpeedMps;
    }

    @Override
    public Component getDisplayRepr() {
        return Component.literal("Move faster than " + String.format("%.1f", maxSpeedMps) + " m/s");
    }
}
