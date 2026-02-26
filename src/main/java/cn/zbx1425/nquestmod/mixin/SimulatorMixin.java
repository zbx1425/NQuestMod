package cn.zbx1425.nquestmod.mixin;

import cn.zbx1425.nquestmod.NQuestMod;
import cn.zbx1425.nquestmod.interop.TscStatus;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.core.data.Position;
import org.mtr.core.data.Station;
import org.mtr.core.data.Vehicle;
import org.mtr.core.data.VehicleExtraData;
import org.mtr.core.simulation.Simulator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Simulator.class, remap = false)
public class SimulatorMixin {

    @Unique
    long updateResponseNonce;

    @Inject(method = "tick()V", at = @At("RETURN"))
    void onEndTick(CallbackInfo ci) {
        if (updateResponseNonce == TscStatus.updateRequestNonce) return;
        updateResponseNonce = TscStatus.updateRequestNonce;
        Simulator simulator = (Simulator)(Object)this;

        for (var it = TscStatus.STATION_NAME_REQUESTS.iterator(); it.hasNext(); ) {
            long id = it.nextLong();
            Station stationOrNull = simulator.stationIdMap.get(id);
            if (stationOrNull != null) {
                TscStatus.STATION_NAMES.put(id, stationOrNull.getName());
                it.remove();
            }
        }

        if (!TscStatus.isAnyQuestGoingOn) return;

        simulator.clients.forEach(client -> {
            Position clientPosition = TscStatus.CLIENT_POSITIONS.get(client.uuid);
            if (clientPosition == null) return;
            TscStatus.CLIENTS.put(client.uuid, new TscStatus.ClientState(
                simulator.stations.stream().filter(station -> station.inArea(clientPosition))
                        .collect(ObjectArrayList::new, (list, station) ->
                                list.add(new TscStatus.NameIdData(station.getName(), station.getId())),
                                ObjectArrayList::addAll)
            ));
        });

        simulator.sidings.forEach(siding ->
            ((SidingAccessor)(Object)siding).getVehicles().forEach(vehicle ->
                vehicle.vehicleExtraData.iterateRidingEntities(vehicleRidingEntity -> {
                    final TscStatus.ClientState client = TscStatus.CLIENTS.get(vehicleRidingEntity.uuid);
                    final VehicleExtraData vehicleExtraData = vehicle.vehicleExtraData;
                    if (client != null) {
                        long routeId = vehicleExtraData.getThisRouteId();
                        if (routeId == 0) routeId = vehicleExtraData.getPreviousRouteId();
                        if (routeId == 0) routeId = vehicleExtraData.getNextRouteId();
                        TscStatus.CLIENTS.put(vehicleRidingEntity.uuid, new TscStatus.ClientState(
                                client,
                                simulator.routeIdMap.get(routeId),
                                vehicleExtraData.getDoorMultiplier() == -1,
                            ((VehicleAccessor)vehicle).getSpeed() * 1000
                        ));
                        // NQuestMod.LOGGER.info("Door {}, Speed {} km/h",
                        //         vehicleExtraData.getDoorMultiplier() == -1 ? "Close" : "Open",
                        //         ((VehicleAccessor)vehicle).getSpeed() * 1000 * 3.6
                        // );
                    }
                })
            )
        );
    }
}
