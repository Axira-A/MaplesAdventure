package dev.maplesadventure.multiplayer.encounter.fog.network;

import dev.maplesadventure.multiplayer.encounter.fog.FogTraversalManager;
import dev.maplesadventure.multiplayer.encounter.fog.client.FogGateClientCache;
import dev.maplesadventure.multiplayer.encounter.fog.client.FogTraversalClientState;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class FogGateNetwork {
    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(FogGatePayloads.RequestEntry.TYPE, FogGatePayloads.RequestEntry.STREAM_CODEC,
                (payload, context) -> { if (context.player() instanceof ServerPlayer player) FogTraversalManager.request(player, payload.gateId()); });
        registrar.playToClient(FogGatePayloads.Snapshot.TYPE, FogGatePayloads.Snapshot.STREAM_CODEC,
                (payload, context) -> { if (FMLEnvironment.dist == Dist.CLIENT) FogGateClientCache.replace(payload.gates()); });
        registrar.playToClient(FogGatePayloads.StartTraversal.TYPE, FogGatePayloads.StartTraversal.STREAM_CODEC,
                (payload, context) -> { if (FMLEnvironment.dist == Dist.CLIENT) FogTraversalClientState.start(payload.direction(), payload.ticks()); });
        registrar.playToClient(FogGatePayloads.StopTraversal.TYPE, FogGatePayloads.StopTraversal.STREAM_CODEC,
                (payload, context) -> { if (FMLEnvironment.dist == Dist.CLIENT) FogTraversalClientState.stop(); });
    }
    private FogGateNetwork() {}
}
