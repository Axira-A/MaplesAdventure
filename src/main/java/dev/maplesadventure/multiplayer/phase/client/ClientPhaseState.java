package dev.maplesadventure.multiplayer.phase.client;

import dev.maplesadventure.multiplayer.phase.PlayerPhaseState;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Read-only client mirror. Missing entries fail closed to each player's stable solo phase. */
public final class ClientPhaseState {
    private static final Map<UUID, PlayerPhaseState> STATES = new LinkedHashMap<>();

    public static PlayerPhaseState state(UUID playerUuid) {
        return STATES.getOrDefault(playerUuid, PlayerPhaseState.solo(playerUuid));
    }

    public static void replace(Map<UUID, PlayerPhaseState> states) {
        STATES.clear();
        STATES.putAll(states);
    }

    public static void update(UUID playerUuid, PlayerPhaseState state) { STATES.put(playerUuid, state); }
    public static void remove(UUID playerUuid) { STATES.remove(playerUuid); }
    public static void clear() { STATES.clear(); }
    public static int size() { return STATES.size(); }

    private ClientPhaseState() {}
}
