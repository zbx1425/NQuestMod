package cn.zbx1425.nquestmod.data.criteria.mtr;

import cn.zbx1425.nquestmod.data.criteria.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

public class RideToStationCriterion implements Criterion {

    public String stationName;

    public RideToStationCriterion(String stationName) {
        this.stationName = stationName;
    }

    @Override
    public boolean evaluate(ServerPlayer player, CriterionContext ctx) {
        throw new UnsupportedOperationException("Must be expanded before evaluation");
    }

    @Override
    public Component getDisplayRepr() {
        return Component.literal("Ride to ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(MtrNameUtil.getStationDisplayName(stationName))
                .withStyle(ChatFormatting.AQUA).withStyle(ChatFormatting.BOLD));
    }

    @Override
    public Criterion expand() {
        return new Descriptor(
            new AndCriterion(List.of(
                new RisingEdgeAndConditionCriterion(new InStationAreaCriterion(stationName), new RideLineCriterion("")),
                new StationStopCriterion()
            )),
            getDisplayRepr()
        );
    }
}
