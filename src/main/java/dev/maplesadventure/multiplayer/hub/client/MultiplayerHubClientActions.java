package dev.maplesadventure.multiplayer.hub.client;

import dev.maplesadventure.multiplayer.hub.MultiplayerAction;
import dev.maplesadventure.multiplayer.hub.network.MultiplayerHubPayloads;
import net.neoforged.neoforge.network.PacketDistributor;

public final class MultiplayerHubClientActions {
    public static void requestState() { PacketDistributor.sendToServer(new MultiplayerHubPayloads.RequestState()); }
    public static void send(MultiplayerAction action) { PacketDistributor.sendToServer(new MultiplayerHubPayloads.Action(action)); }
    private MultiplayerHubClientActions() {}
}
