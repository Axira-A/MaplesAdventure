package dev.maplesadventure.multiplayer.phase;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.config.PhaseConfig;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/** Server-thread-owned transient phase state. Debug joins intentionally never persist. */
public final class PhaseManager {
    private static final Map<UUID, PlayerPhaseState> STATES = new LinkedHashMap<>();

    public static PlayerPhaseState state(ServerPlayer player) {
        return state(player.getUUID());
    }

    public static PlayerPhaseState state(UUID playerUuid) {
        return STATES.computeIfAbsent(playerUuid, PlayerPhaseState::solo);
    }

    public static PlayerPhaseState assignSolo(ServerPlayer player) {
        return set(player, PlayerPhaseState.solo(player.getUUID()), "login/solo");
    }

    public static PlayerPhaseState join(ServerPlayer player, ServerPlayer target) {
        return set(player, new PlayerPhaseState(state(target).phaseId(), PhaseRole.SOLO), "debug join");
    }

    public static PlayerPhaseState set(ServerPlayer player, PlayerPhaseState state, String reason) {
        PlayerPhaseState previous = STATES.put(player.getUUID(), state);
        if (PhaseConfig.DEBUG.get() && !state.equals(previous)) {
            MaplesAdventure.LOGGER.info("[MaplesAdventure/Phase] state changed player={} phase={} role={} reason={}",
                    player.getGameProfile().getName(), state.phaseId(), state.role(), reason);
        }
        return state;
    }

    public static void forget(UUID playerUuid) { STATES.remove(playerUuid); }
    public static Map<UUID, PlayerPhaseState> snapshot() { return Map.copyOf(STATES); }
    public static void clear() { STATES.clear(); }

    private PhaseManager() {}
}
