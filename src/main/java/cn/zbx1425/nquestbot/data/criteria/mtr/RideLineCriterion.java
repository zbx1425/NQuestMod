package cn.zbx1425.nquestbot.data.criteria.mtr;

import cn.zbx1425.nquestbot.data.criteria.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class RideLineCriterion implements Criterion {

    public String lineName;

    public RideLineCriterion(String lineName) {
        this.lineName = lineName;
    }

    @Override
    public boolean isFulfilled(ServerPlayer player) {
        return lineName.equals(playerStatus.ridingTrainLine);
    }

    @Override
    public Component getDisplayRepr() {
        return Component.literal("Ride ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(lineName).withStyle(ChatFormatting.GREEN).withStyle(ChatFormatting.BOLD));
    }
}
