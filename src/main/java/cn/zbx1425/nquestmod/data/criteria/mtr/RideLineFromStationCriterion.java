package cn.zbx1425.nquestmod.data.criteria.mtr;

import cn.zbx1425.nquestmod.data.criteria.*;
import cn.zbx1425.nquestmod.interop.TscStatus;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class RideLineFromStationCriterion implements Criterion {

    public String lineName;
    public String stationName;

    public RideLineFromStationCriterion(String lineName, String stationName) {
        this.lineName = lineName;
        this.stationName = stationName;
    }

    @Override
    public boolean evaluate(ServerPlayer player, CriterionContext ctx) {
        throw new UnsupportedOperationException("Must be expanded before evaluation");
    }

    @Override
    public Component getDisplayRepr() {
        return Component.literal("Depart from ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(MtrNameUtil.getStationDisplayName(stationName))
                .withStyle(ChatFormatting.AQUA).withStyle(ChatFormatting.BOLD))
            .append(Component.literal(" on ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(lineName).withStyle(ChatFormatting.GREEN).withStyle(ChatFormatting.BOLD));
    }

    @Override
    public Criterion expand() {
        return new Descriptor(
            new RisingEdgeAndConditionCriterion(
                new NotCriterion(new StationStopCriterion(), ""),
                new AndCriterion(List.of(
                    new InStationAreaCriterion(stationName),
                    new RideLineCriterion(lineName)
                ))
            ),
            getDisplayRepr()
        );
    }
}
