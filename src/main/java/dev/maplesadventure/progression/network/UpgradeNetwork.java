package dev.maplesadventure.progression.network;

import dev.maplesadventure.progression.client.UpgradeClient;
import dev.maplesadventure.progression.upgrade.UpgradeAccessService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class UpgradeNetwork {
    public static void register(PayloadRegistrar r) {
        r.playToServer(UpgradePayloads.Open.TYPE, UpgradePayloads.Open.CODEC, (p, c) -> {
            if (c.player() instanceof ServerPlayer player) UpgradeAccessService.open(player, p.nonce());
        });
        r.playToServer(UpgradePayloads.Close.TYPE, UpgradePayloads.Close.CODEC, (p, c) -> {
            if (c.player() instanceof ServerPlayer player) UpgradeAccessService.close(player, p.nonce());
        });
        r.playToServer(UpgradePayloads.Submit.TYPE, UpgradePayloads.Submit.CODEC, (p, c) -> {
            if (c.player() instanceof ServerPlayer player) UpgradeAccessService.submit(player, p.nonce(), p.revision(), p.deltas());
        });
        r.playToClient(UpgradePayloads.View.TYPE, UpgradePayloads.View.CODEC, (p, c) -> {
            if (FMLEnvironment.dist == Dist.CLIENT) UpgradeClient.receive(p);
        });
        r.playToClient(UpgradePayloads.Closed.TYPE, UpgradePayloads.Closed.CODEC, (p, c) -> {
            if (FMLEnvironment.dist == Dist.CLIENT) UpgradeClient.closed(p.nonce(), p.reason());
        });
    }
    private UpgradeNetwork() {}
}
