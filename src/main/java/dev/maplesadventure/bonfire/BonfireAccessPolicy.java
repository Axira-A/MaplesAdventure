package dev.maplesadventure.bonfire;

import dev.maplesadventure.multiplayer.encounter.EncounterManager;
import dev.maplesadventure.multiplayer.invasion.InvasionSessionManager;
import dev.maplesadventure.multiplayer.phase.PhaseManager;
import dev.maplesadventure.multiplayer.phase.PhaseRole;
import net.minecraft.server.level.ServerPlayer;

/** One server-side gate for activation, rest and level-up entry. */
public final class BonfireAccessPolicy {
    public static boolean allows(PhaseRole role, boolean alive, boolean spectator,
                                 boolean bossActive, boolean hostileSession) {
        return (role == PhaseRole.SOLO || role == PhaseRole.HOST)
                && alive && !spectator && !bossActive && !hostileSession;
    }
    public static boolean allows(ServerPlayer player) {
        var phase = PhaseManager.state(player);
        return allows(phase.role(), player.isAlive() && !player.isRemoved(), player.isSpectator(),
                EncounterManager.hasActiveBossAttempt(player.server, phase.phaseId()),
                InvasionSessionManager.sessionForPhase(phase.phaseId()).isPresent());
    }
    private BonfireAccessPolicy() {}
}
