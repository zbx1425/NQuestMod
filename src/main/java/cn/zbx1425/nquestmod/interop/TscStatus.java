package cn.zbx1425.nquestmod.interop;

import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.mtr.core.data.NameColorDataBase;
import org.mtr.core.data.Position;
import org.mtr.core.tool.Utilities;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

public class TscStatus {

    public static long updateRequestNonce;
    public static boolean isAnyQuestGoingOn = false;
    public static final Object2ObjectMap<UUID, Position> CLIENT_POSITIONS = Object2ObjectMaps.synchronize(new Object2ObjectOpenHashMap<>());
    public static final LongSet STATION_NAME_REQUESTS = LongSets.synchronize(new LongOpenHashSet());

    public static final Object2ObjectMap<UUID, ClientState> CLIENTS = Object2ObjectMaps.synchronize(new Object2ObjectOpenHashMap<>());
    public static final Long2ObjectMap<String> STATION_NAMES = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>());

    public static void requestUpdate(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            CLIENT_POSITIONS.put(player.getGameProfile().getId(), new Position(player.getBlockX(), player.getBlockY(), player.getBlockZ()));
        }
        updateRequestNonce = System.currentTimeMillis();
    }

    public static ClientState getClientState(ServerPlayer player) {
        return CLIENTS.get(player.getGameProfile().getId());
    }

    public static String getStationName(long id) {
        if (STATION_NAMES.containsKey(id)) return STATION_NAMES.get(id);
        STATION_NAME_REQUESTS.add(id);
        return Utilities.numberToPaddedHexString(id);
    }

    public record NameIdData(String name, long id) {}

    public record ClientState(Collection<NameIdData> stations, NameIdData line) {

        public ClientState(Collection<NameIdData> stations) {
            this(stations, null);
        }

        public ClientState(ClientState baseOn, NameColorDataBase line) {
            this(baseOn.stations, line != null ? new NameIdData(line.getName(), line.getId()) : null);
        }

        @Override
        public String toString() {
            return "ClientState{" +
                "stations=" + stations.stream().map(s -> s.name).collect(Collectors.joining(",")) +
                ", line=" + (line == null ? "" : line.name) +
                '}';
        }
    }
}
