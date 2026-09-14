package dev.maplesadventure.multiplayer.phase.network;

import dev.maplesadventure.multiplayer.phase.PlayerPhaseState;
import dev.maplesadventure.multiplayer.phase.client.ClientPhaseState;
import dev.maplesadventure.multiplayer.echo.client.EchoPlaybackManager;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class PhaseNetwork {
    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(PhasePayloads.Snapshot.TYPE, PhasePayloads.Snapshot.STREAM_CODEC,
                (payload, context) -> runClient(() -> {
                    Map<UUID, PlayerPhaseState> states = new LinkedHashMap<>();
                    payload.entries().forEach(entry -> states.put(entry.playerId(), entry.state()));
                    ClientPhaseState.replace(states);
                }));
        registrar.playToClient(PhasePayloads.Update.TYPE, PhasePayloads.Update.STREAM_CODEC,
                (payload, context) -> runClient(() -> {
                    ClientPhaseState.update(payload.entry().playerId(), payload.entry().state());
                    EchoPlaybackManager.removeSourceIfVisible(payload.entry().playerId());
                }));
        registrar.playToClient(PhasePayloads.Remove.TYPE, PhasePayloads.Remove.STREAM_CODEC,
                (payload, context) -> runClient(() -> ClientPhaseState.remove(payload.playerId())));
    }

    private static void runClient(Runnable action) {
        if (FMLEnvironment.dist == Dist.CLIENT) action.run();
    }

    private PhaseNetwork() {}
}
