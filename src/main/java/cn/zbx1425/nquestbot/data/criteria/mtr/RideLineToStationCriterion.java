package cn.zbx1425.nquestbot.data.criteria.mtr;

import cn.zbx1425.nquestbot.data.criteria.Criterion;
import cn.zbx1425.nquestbot.interop.TscStatus;
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
        TscStatus.ClientState state = TscStatus.getClientState(player);
        boolean lineFulfilled = state != null && state.line() != null && MtrNameUtil.matches(lineName, state.line());
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
        return Component.literal("Ride ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(lineName).withStyle(ChatFormatting.GREEN).withStyle(ChatFormatting.BOLD))
            .append(Component.literal(" to ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(stationName).withStyle(ChatFormatting.GOLD).withStyle(ChatFormatting.BOLD));
    }
}
