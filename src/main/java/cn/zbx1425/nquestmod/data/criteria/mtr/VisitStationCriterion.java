package cn.zbx1425.nquestmod.data.criteria.mtr;

import cn.zbx1425.nquestmod.data.criteria.Criterion;
import cn.zbx1425.nquestmod.interop.TscStatus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class VisitStationCriterion implements Criterion {

    public String stationName;

    public VisitStationCriterion(String stationName) {
        this.stationName = stationName;
    }

    @Override
    public boolean isFulfilled(ServerPlayer player) {
        TscStatus.ClientState state = TscStatus.getClientState(player);

        if (state == null) return false;
        boolean stationFulfilled = false;
        for (var station : state.stations()) {
            if (MtrNameUtil.matches(stationName, station)) {
                stationFulfilled = true;
                break;
            }
        }
        return stationFulfilled;
    }

    @Override
    public Component getDisplayRepr() {
        return Component.literal("Visit ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(MtrNameUtil.getStationDisplayName(stationName))
                .withStyle(ChatFormatting.AQUA).withStyle(ChatFormatting.BOLD));
    }
}
