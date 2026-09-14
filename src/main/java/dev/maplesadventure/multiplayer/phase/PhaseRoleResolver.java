package dev.maplesadventure.multiplayer.phase;

import dev.maplesadventure.multiplayer.coop.CoopSession;
import dev.maplesadventure.multiplayer.coop.CoopSessionManager;
import dev.maplesadventure.multiplayer.coop.CoopSessionState;
import dev.maplesadventure.multiplayer.invasion.InvasionSession;
import dev.maplesadventure.multiplayer.invasion.InvasionSessionManager;
import dev.maplesadventure.multiplayer.invasion.InvasionSessionState;
import net.minecraft.server.level.ServerPlayer;

/** Resolves the one authoritative role after overlapping co-op and invasion lifecycle changes. */
public final class PhaseRoleResolver {
    public static PlayerPhaseState resolve(ServerPlayer player) {
        InvasionSession invasion = InvasionSessionManager.session(player.getUUID()).orElse(null);
        if (invasion != null && invasion.state() != InvasionSessionState.ENDED) {
            if (invasion.invaderUuid().equals(player.getUUID()))
                return new PlayerPhaseState(invasion.hostPhaseId(), PhaseRole.INVADER);
            if (invasion.hostUuid().equals(player.getUUID()))
                return new PlayerPhaseState(invasion.hostPhaseId(), PhaseRole.HOST);
        }
        CoopSession coop = CoopSessionManager.session(player.getUUID()).orElse(null);
        if (coop != null && coop.state() == CoopSessionState.ACTIVE) {
            if (coop.cooperatorUuid().equals(player.getUUID()))
                return new PlayerPhaseState(coop.phaseId(), PhaseRole.COOPERATOR);
            if (coop.hostUuid().equals(player.getUUID()))
                return new PlayerPhaseState(coop.phaseId(), PhaseRole.HOST);
        }
        return PlayerPhaseState.solo(player.getUUID());
    }

    public static PlayerPhaseState refresh(ServerPlayer player, String reason) {
        PlayerPhaseState result = PhaseManager.set(player, resolve(player), reason);
        PhaseSyncService.changed(player);
        return result;
    }

    private PhaseRoleResolver() {}
}
