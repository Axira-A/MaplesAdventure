package dev.maplesadventure.network;

import dev.maplesadventure.client.message.ClientMessagePayloadHandler;
import dev.maplesadventure.message.MessageManager;
import dev.maplesadventure.multiplayer.phase.network.PhaseNetwork;
import dev.maplesadventure.multiplayer.echo.network.EchoNetwork;
import dev.maplesadventure.multiplayer.coop.network.CoopNetwork;
import dev.maplesadventure.multiplayer.encounter.fog.network.FogGateNetwork;
import dev.maplesadventure.multiplayer.invasion.network.InvasionNetwork;
import dev.maplesadventure.multiplayer.hub.network.MultiplayerHubNetwork;
import dev.maplesadventure.progression.network.AttributeNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class MessageNetwork {
    private static final String PROTOCOL_VERSION = "24";

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(MessagePayloads.Create.TYPE, MessagePayloads.Create.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) MessageManager.create(player, payload.phrases(), payload.connectors());
        });
        registrar.playToServer(MessagePayloads.Read.TYPE, MessagePayloads.Read.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) MessageManager.openReader(player, payload.messageId());
        });
        registrar.playToServer(MessagePayloads.Rate.TYPE, MessagePayloads.Rate.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) MessageManager.rate(player, payload.messageId(), payload.rating());
        });
        registrar.playToServer(MessagePayloads.Delete.TYPE, MessagePayloads.Delete.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) MessageManager.delete(player, payload.messageId());
        });

        registrar.playToClient(MessagePayloads.NearbySnapshot.TYPE, MessagePayloads.NearbySnapshot.STREAM_CODEC,
                (payload, context) -> runClient(() -> ClientMessagePayloadHandler.handle(payload)));
        registrar.playToClient(MessagePayloads.Upsert.TYPE, MessagePayloads.Upsert.STREAM_CODEC,
                (payload, context) -> runClient(() -> ClientMessagePayloadHandler.handle(payload)));
        registrar.playToClient(MessagePayloads.Remove.TYPE, MessagePayloads.Remove.STREAM_CODEC,
                (payload, context) -> runClient(() -> ClientMessagePayloadHandler.handle(payload)));
        registrar.playToClient(MessagePayloads.OpenReader.TYPE, MessagePayloads.OpenReader.STREAM_CODEC,
                (payload, context) -> runClient(() -> ClientMessagePayloadHandler.handle(payload)));
        PhaseNetwork.register(registrar);
        EchoNetwork.register(registrar);
        CoopNetwork.register(registrar);
        FogGateNetwork.register(registrar);
        InvasionNetwork.register(registrar);
        MultiplayerHubNetwork.register(registrar);
        AttributeNetwork.register(registrar);
        dev.maplesadventure.progression.status.StatusNetwork.register(registrar);
        dev.maplesadventure.progression.weapon.WeaponRequirementNetwork.register(registrar);
        dev.maplesadventure.progression.network.UpgradeNetwork.register(registrar);
        dev.maplesadventure.bonfire.network.BonfireNetwork.register(registrar);
    }

    private static void runClient(Runnable action) {
        if (FMLEnvironment.dist == Dist.CLIENT) action.run();
    }

    private MessageNetwork() {
    }
}
