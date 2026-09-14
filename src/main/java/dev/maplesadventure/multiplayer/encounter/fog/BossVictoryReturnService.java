package dev.maplesadventure.multiplayer.encounter.fog;

import dev.maplesadventure.multiplayer.coop.CoopSessionManager;
import dev.maplesadventure.multiplayer.phase.PhaseId;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.server.MinecraftServer;

/** Delays cooperator return so the victory animation remains visible; host is never moved. */
public final class BossVictoryReturnService {
    private static final Map<PhaseId, Long> PENDING = new HashMap<>();
    public static void schedule(MinecraftServer server, PhaseId phase) {
        if (CoopSessionManager.sessionForPhase(phase).isPresent()) PENDING.put(phase, server.getTickCount() + 60L);
    }
    public static void tick(MinecraftServer server) {
        Iterator<Map.Entry<PhaseId, Long>> iterator = PENDING.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (server.getTickCount() < entry.getValue()) continue;
            CoopSessionManager.sessionForPhase(entry.getKey()).ifPresent(session ->
                    CoopSessionManager.endForPlayer(session.hostUuid(), CoopSessionManager.EndReason.BOSS_DEFEATED));
            iterator.remove();
        }
    }
    public static boolean isPending(PhaseId phase) { return PENDING.containsKey(phase); }
    public static void clear() { PENDING.clear(); }
    private BossVictoryReturnService() {}
}
