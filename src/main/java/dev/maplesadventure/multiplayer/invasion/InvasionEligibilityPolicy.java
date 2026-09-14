package dev.maplesadventure.multiplayer.invasion;

import dev.maplesadventure.config.InvasionConfig;
import dev.maplesadventure.multiplayer.coop.CoopSession;
import dev.maplesadventure.multiplayer.coop.CoopSessionManager;
import dev.maplesadventure.multiplayer.coop.CoopSessionState;
import dev.maplesadventure.multiplayer.coop.PendingReturnSavedData;
import dev.maplesadventure.multiplayer.encounter.EncounterManager;
import dev.maplesadventure.multiplayer.encounter.fog.FogTraversalManager;
import dev.maplesadventure.multiplayer.encounter.fog.BossVictoryReturnService;
import dev.maplesadventure.multiplayer.phase.PhaseId;
import dev.maplesadventure.multiplayer.phase.PhaseManager;
import dev.maplesadventure.multiplayer.phase.PhaseRole;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class InvasionEligibilityPolicy {
    public static Failure invader(ServerPlayer player) {
        if (!InvasionConfig.ENABLED.get()) return Failure.DISABLED;
        if (!player.isAlive() || player.isSpectator() || player.isCreative()) return Failure.PLAYER_STATE;
        var state = PhaseManager.state(player);
        if (state.role() != PhaseRole.SOLO || !state.phaseId().equals(PhaseId.solo(player.getUUID()))) return Failure.NOT_SOLO;
        if (CoopSessionManager.hasSession(player.getUUID()) || InvasionSessionManager.hasSession(player.getUUID())) return Failure.IN_SESSION;
        if (PendingReturnSavedData.get(player.server).contains(player.getUUID())
                || PendingInvasionReturnSavedData.get(player.server).contains(player.getUUID())) return Failure.PENDING_RETURN;
        if (EncounterManager.hasActiveBossAttempt(player.server, state.phaseId())) return Failure.BOSS_ACTIVE;
        return Failure.NONE;
    }

    public static Failure host(MinecraftServer server, CoopSession coop) {
        if (!InvasionConfig.ENABLED.get() || coop == null || coop.state() != CoopSessionState.ACTIVE) return Failure.NO_ACTIVE_COOP;
        ServerPlayer host = server.getPlayerList().getPlayer(coop.hostUuid());
        ServerPlayer cooperator = server.getPlayerList().getPlayer(coop.cooperatorUuid());
        if (host == null || cooperator == null || !host.isAlive() || !cooperator.isAlive()) return Failure.PARTY_UNAVAILABLE;
        if (!PhaseManager.state(host).phaseId().equals(coop.phaseId())
                || !PhaseManager.state(cooperator).phaseId().equals(coop.phaseId())) return Failure.PHASE_MISMATCH;
        if (InvasionSessionManager.sessionForHost(coop.hostUuid()).isPresent()) return Failure.IN_SESSION;
        if (EncounterManager.hasActiveBossAttempt(server, coop.phaseId())) return Failure.BOSS_ACTIVE;
        if (BossVictoryReturnService.isPending(coop.phaseId())) return Failure.BOSS_ACTIVE;
        if (FogTraversalManager.hasActiveTraversal(coop.phaseId())) return Failure.FOG_TRAVERSAL;
        if (InvasionSessionManager.isCoolingDown(server, coop.hostUuid())) return Failure.COOLDOWN;
        if (System.currentTimeMillis() - coop.createdAt() < InvasionConfig.GRACE_SECONDS.get() * 1000L) return Failure.GRACE;
        return Failure.NONE;
    }

    public enum Failure {
        NONE, DISABLED, PLAYER_STATE, NOT_SOLO, IN_SESSION, PENDING_RETURN, BOSS_ACTIVE,
        NO_ACTIVE_COOP, PARTY_UNAVAILABLE, PHASE_MISMATCH, FOG_TRAVERSAL, BOSS_ROOM, COOLDOWN, GRACE
    }
    private InvasionEligibilityPolicy() {}
}
