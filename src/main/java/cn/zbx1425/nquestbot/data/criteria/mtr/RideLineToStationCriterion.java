package cn.zbx1425.nquestbot.data.criteria.mtr;

import cn.zbx1425.nquestbot.data.criteria.Criterion;
import cn.zbx1425.nquestbot.data.criteria.RisingEdgeAndConditionCriterion;
import cn.zbx1425.nquestbot.interop.TscStatus;
import com.google.gson.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Type;

public class RideLineToStationCriterion implements Criterion {

    public String lineName;
    public String stationName;

    public RideLineToStationCriterion(String lineName, String stationName) {
        this.lineName = lineName;
        this.stationName = stationName;
    }

    @Override
    public boolean isFulfilled(ServerPlayer player) {
        throw new UnsupportedOperationException("Use stateful instance");
    }

    @Override
    public Component getDisplayRepr() {
        return Component.literal("Ride ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(lineName).withStyle(ChatFormatting.GREEN).withStyle(ChatFormatting.BOLD))
            .append(Component.literal(" to ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(stationName).withStyle(ChatFormatting.AQUA).withStyle(ChatFormatting.BOLD));
    }

    @Override
    public Criterion createStatefulInstance() {
        return new RisingEdgeAndConditionCriterion(new VisitStationCriterion(stationName), new RideLineCriterion(lineName), this);
    }
}
