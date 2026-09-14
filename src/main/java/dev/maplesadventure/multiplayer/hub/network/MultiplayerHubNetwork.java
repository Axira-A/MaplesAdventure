package dev.maplesadventure.multiplayer.hub.network;

import dev.maplesadventure.multiplayer.hub.MultiplayerHubService;
import dev.maplesadventure.multiplayer.hub.client.MultiplayerHubClientState;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class MultiplayerHubNetwork {
    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(MultiplayerHubPayloads.RequestState.TYPE, MultiplayerHubPayloads.RequestState.STREAM_CODEC,
                (payload, context) -> { if (context.player() instanceof ServerPlayer player) MultiplayerHubService.sync(player); });
        registrar.playToServer(MultiplayerHubPayloads.Action.TYPE, MultiplayerHubPayloads.Action.STREAM_CODEC,
                (payload, context) -> { if (context.player() instanceof ServerPlayer player)
                    MultiplayerHubService.handle(player, payload.action()); });
        registrar.playToClient(MultiplayerHubPayloads.State.TYPE, MultiplayerHubPayloads.State.STREAM_CODEC,
                (payload, context) -> { if (FMLEnvironment.dist == Dist.CLIENT)
                    MultiplayerHubClientState.update(payload.state()); });
    }
    private MultiplayerHubNetwork() {}
}
