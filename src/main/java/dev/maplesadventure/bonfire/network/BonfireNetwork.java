package dev.maplesadventure.bonfire.network;

import dev.maplesadventure.bonfire.BonfireSessionService;
import dev.maplesadventure.client.bonfire.BonfireClient;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class BonfireNetwork {
    public static void register(PayloadRegistrar r) {
        r.playToServer(BonfirePayloads.Action.TYPE, BonfirePayloads.Action.CODEC, (p, c) -> {
            if (c.player() instanceof ServerPlayer player) BonfireSessionService.action(player, p.nonce(), p.action());
        });
        r.playToClient(BonfirePayloads.View.TYPE, BonfirePayloads.View.CODEC, (p, c) -> {
            if (FMLEnvironment.dist == Dist.CLIENT) BonfireClient.view(p);
        });
        r.playToClient(BonfirePayloads.Closed.TYPE, BonfirePayloads.Closed.CODEC, (p, c) -> {
            if (FMLEnvironment.dist == Dist.CLIENT) BonfireClient.closed(p);
        });
        r.playToClient(BonfirePayloads.Activated.TYPE, BonfirePayloads.Activated.CODEC, (p, c) -> {
            if (FMLEnvironment.dist == Dist.CLIENT) BonfireClient.activated(p);
        });
    }
    private BonfireNetwork() {}
}
