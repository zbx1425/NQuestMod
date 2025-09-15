package cn.zbx1425.nquestbot.data.criteria.mtr;

import cn.zbx1425.nquestbot.data.criteria.Criterion;
import cn.zbx1425.nquestbot.interop.TscStatus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class RideToStationCriterion implements Criterion {

    public String stationName;

    public RideToStationCriterion(String stationName) {
        this.stationName = stationName;
    }

    @Override
    public boolean isFulfilled(ServerPlayer player) {
        TscStatus.ClientState state = TscStatus.getClientState(player);
        boolean lineFulfilled = state != null && state.line() != null;
        if (!lineFulfilled) return false;

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
        return Component.literal("Ride to ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(stationName).withStyle(ChatFormatting.GOLD).withStyle(ChatFormatting.BOLD));
    }
}
