package cn.zbx1425.nquestbot.interop;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.server.level.ServerPlayer;
import org.mtr.core.data.NameColorDataBase;

import java.util.Collection;
import java.util.UUID;

public class TscStatus {

    public static long updateRequestNonce;
    public static final Object2ObjectMap<UUID, ClientState> CLIENTS = Object2ObjectMaps.synchronize(new Object2ObjectOpenHashMap<>());

    public static void requestUpdate() {
        updateRequestNonce = System.currentTimeMillis();
    }

    public static ClientState getClientState(ServerPlayer player) {
        return CLIENTS.get(player.getGameProfile().getId());
    }

    public record NameIdData(String name, long id) {}

    public record ClientState(Collection<NameIdData> stations, NameIdData line) {

        public ClientState(Collection<NameIdData> stations) {
            this(stations, null);
        }

        public ClientState(ClientState baseOn, NameColorDataBase line) {
            this(baseOn.stations, line != null ? new NameIdData(line.getName(), line.getId()) : null);
        }
    }
}
