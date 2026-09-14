package dev.maplesadventure.multiplayer.echo.network;

import dev.maplesadventure.multiplayer.echo.client.EchoPlaybackManager;
import dev.maplesadventure.multiplayer.echo.EpicFightEchoServerSamples;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class EchoNetwork {
    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(EchoPayloads.AnimationSample.TYPE, EchoPayloads.AnimationSample.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player && player.getServer() != null) {
                        EpicFightEchoServerSamples.accept(player.getUUID(), player.getServer().getTickCount(), payload.state());
                    }
                });
        registrar.playToClient(EchoPayloads.Playback.TYPE, EchoPayloads.Playback.STREAM_CODEC,
                (payload, context) -> {
                    if (FMLEnvironment.dist == Dist.CLIENT) EchoPlaybackManager.accept(payload);
                });
    }
    private EchoNetwork() {}
}
