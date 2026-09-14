package dev.maplesadventure.multiplayer.coop.network;

import dev.maplesadventure.multiplayer.coop.CoopSessionManager;
import dev.maplesadventure.multiplayer.coop.SummonSignManager;
import dev.maplesadventure.multiplayer.coop.client.SummonSignRenderCache;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class CoopNetwork {
    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(CoopPayloads.ToggleSign.TYPE, CoopPayloads.ToggleSign.STREAM_CODEC,
                (payload, context) -> { if (context.player() instanceof ServerPlayer player) SummonSignManager.toggle(player); });
        registrar.playToServer(CoopPayloads.RequestSummon.TYPE, CoopPayloads.RequestSummon.STREAM_CODEC,
                (payload, context) -> { if (context.player() instanceof ServerPlayer player) {
                    SummonSignManager.find(payload.signId()).ifPresentOrElse(sign -> {
                        if (sign.type() == dev.maplesadventure.multiplayer.coop.SummonSignType.DUEL)
                            dev.maplesadventure.multiplayer.invasion.InvasionSessionManager.startDuel(player, payload.signId());
                        else CoopSessionManager.summon(player, payload.signId());
                    }, () -> player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                            "summon.maplesadventure.failure.sign_unavailable"), true));
                }});
        registrar.playToServer(CoopPayloads.Leave.TYPE, CoopPayloads.Leave.STREAM_CODEC,
                (payload, context) -> { if (context.player() instanceof ServerPlayer player) CoopSessionManager.leave(player); });
        registrar.playToServer(CoopPayloads.Dismiss.TYPE, CoopPayloads.Dismiss.STREAM_CODEC,
                (payload, context) -> { if (context.player() instanceof ServerPlayer player) CoopSessionManager.dismiss(player); });

        registrar.playToClient(CoopPayloads.SignSnapshot.TYPE, CoopPayloads.SignSnapshot.STREAM_CODEC,
                (payload, context) -> runClient(() -> SummonSignRenderCache.replace(payload.signs())));
        registrar.playToClient(CoopPayloads.SignUpsert.TYPE, CoopPayloads.SignUpsert.STREAM_CODEC,
                (payload, context) -> runClient(() -> SummonSignRenderCache.upsert(payload.sign())));
        registrar.playToClient(CoopPayloads.SignRemove.TYPE, CoopPayloads.SignRemove.STREAM_CODEC,
                (payload, context) -> runClient(() -> SummonSignRenderCache.remove(payload.signId())));
    }

    private static void runClient(Runnable runnable) {
        if (FMLEnvironment.dist == Dist.CLIENT) runnable.run();
    }

    private CoopNetwork() {}
}
