package dev.maplesadventure.multiplayer.invasion.network;

import dev.maplesadventure.multiplayer.echo.client.EchoPlaybackManager;
import dev.maplesadventure.multiplayer.invasion.InvasionQueueManager;
import dev.maplesadventure.multiplayer.invasion.client.InvasionMaterializationCache;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class InvasionNetwork {
    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(InvasionPayloads.ToggleSeek.TYPE, InvasionPayloads.ToggleSeek.STREAM_CODEC,
                (payload, context) -> { if (context.player() instanceof ServerPlayer player) InvasionQueueManager.toggle(player); });
        registrar.playToClient(InvasionPayloads.Materialize.TYPE, InvasionPayloads.Materialize.STREAM_CODEC,
                (payload, context) -> runClient(() -> {
                    EchoPlaybackManager.removeSource(payload.source());
                    InvasionMaterializationCache.add(payload.source(), payload.position(), payload.yaw(), payload.ticks());
                }));
        registrar.playToClient(InvasionPayloads.Clear.TYPE, InvasionPayloads.Clear.STREAM_CODEC,
                (payload, context) -> runClient(() -> {
                    EchoPlaybackManager.removeSource(payload.source());
                    InvasionMaterializationCache.remove(payload.source());
                }));
    }
    private static void runClient(Runnable action) { if (FMLEnvironment.dist == Dist.CLIENT) action.run(); }
    private InvasionNetwork() {}
}
