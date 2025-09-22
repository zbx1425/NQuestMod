package cn.zbx1425.nquestmod.data.criteria.mtr;

import cn.zbx1425.nquestmod.data.criteria.Criterion;
import cn.zbx1425.nquestmod.data.criteria.Descriptor;
import cn.zbx1425.nquestmod.data.criteria.RisingEdgeAndConditionCriterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

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
            .append(Component.literal(MtrNameUtil.getStationDisplayName(stationName))
                .withStyle(ChatFormatting.AQUA).withStyle(ChatFormatting.BOLD));
    }

    @Override
    public Criterion createStatefulInstance() {
        return new Descriptor(
            new RisingEdgeAndConditionCriterion(new VisitStationCriterion(stationName), new RideLineCriterion(lineName)),
            getDisplayRepr()
        );
    }
}
