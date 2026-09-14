package dev.maplesadventure.multiplayer.hub.client;

import dev.maplesadventure.multiplayer.hub.MultiplayerHubState;

public final class MultiplayerHubClientState {
    private static MultiplayerHubState state = MultiplayerHubState.empty();
    public static MultiplayerHubState get() { return state; }
    public static void update(MultiplayerHubState updated) { state = updated; }
    public static void clear() { state = MultiplayerHubState.empty(); }
    private MultiplayerHubClientState() {}
}
