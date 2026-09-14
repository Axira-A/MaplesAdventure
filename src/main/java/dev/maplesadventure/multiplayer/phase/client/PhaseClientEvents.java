package dev.maplesadventure.multiplayer.phase.client;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class PhaseClientEvents {
    public static void register() { NeoForge.EVENT_BUS.register(new PhaseClientEvents()); }

    @SubscribeEvent
    public void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientPhaseState.clear();
        dev.maplesadventure.multiplayer.encounter.fog.client.FogGateClientCache.clear();
        dev.maplesadventure.multiplayer.encounter.fog.client.FogTraversalClientState.stop();
        dev.maplesadventure.multiplayer.invasion.client.InvasionMaterializationCache.clear();
        dev.maplesadventure.multiplayer.hub.client.MultiplayerHubClientState.clear();
        dev.maplesadventure.progression.client.ClientAttributeState.clear();
    }

    private PhaseClientEvents() {}
}
