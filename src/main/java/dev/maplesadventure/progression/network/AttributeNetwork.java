package dev.maplesadventure.progression.network;

import dev.maplesadventure.progression.AttributeSyncService;
import dev.maplesadventure.progression.client.ClientAttributeState;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class AttributeNetwork {
    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(AttributePayloads.RequestSnapshot.TYPE, AttributePayloads.RequestSnapshot.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player) AttributeSyncService.sync(player);
                });
        registrar.playToClient(AttributePayloads.Snapshot.TYPE, AttributePayloads.Snapshot.STREAM_CODEC,
                (payload, context) -> {
                    if (FMLEnvironment.dist == Dist.CLIENT) ClientAttributeState.update(payload.snapshot());
                });
    }
    private AttributeNetwork() {}
}
