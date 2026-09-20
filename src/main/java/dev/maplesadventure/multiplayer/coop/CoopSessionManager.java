package dev.maplesadventure.multiplayer.coop;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.multiplayer.phase.PhaseId;
import dev.maplesadventure.multiplayer.phase.PhaseManager;
import dev.maplesadventure.multiplayer.phase.PhaseRole;
import dev.maplesadventure.multiplayer.phase.PhaseRoleResolver;
import dev.maplesadventure.multiplayer.phase.PhaseSyncService;
import dev.maplesadventure.multiplayer.phase.PlayerPhaseState;
import dev.maplesadventure.multiplayer.encounter.EncounterManager;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Authoritative one-host/one-cooperator transaction and idempotent teardown boundary. */
public final class CoopSessionManager {
    private static final Map<UUID, CoopSession> SESSIONS = new LinkedHashMap<>();
    private static final Map<UUID, UUID> SESSION_BY_PLAYER = new LinkedHashMap<>();

    public static synchronized SummonResult summon(ServerPlayer host, UUID signId) {
        SummonSignRecord sign = SummonSignManager.find(signId).orElse(null);
        if (sign == null || sign.type() != SummonSignType.COOP || sign.state() != SummonSignState.AVAILABLE)
            return reject(host, SummonResult.SIGN_UNAVAILABLE);
        if (sign.ownerUuid().equals(host.getUUID())) return reject(host, SummonResult.OWN_SIGN);
        if (!isTrueSolo(host) || hasSession(host.getUUID())) return reject(host, SummonResult.HOST_NOT_SOLO);
        if (EncounterManager.hasActiveBossAttempt(host.server, PhaseId.solo(host.getUUID()))) {
            return reject(host, SummonResult.BOSS_ATTEMPT_ACTIVE);
        }

        ServerPlayer cooperator = host.server.getPlayerList().getPlayer(sign.ownerUuid());
        if (cooperator == null || !cooperator.isAlive()) return reject(host, SummonResult.COOPERATOR_UNAVAILABLE);
        if (!isTrueSolo(cooperator) || hasSession(cooperator.getUUID())) {
            return reject(host, SummonResult.COOPERATOR_NOT_SOLO);
        }
        if (!host.level().dimension().equals(cooperator.level().dimension())
                || !host.level().dimension().equals(sign.dimension())) return reject(host, SummonResult.WRONG_DIMENSION);
        if (!SummonSignValidator.validateForSummon(host, sign)) return reject(host, SummonResult.OUT_OF_RANGE_OR_BLOCKED);

        Vec3 summonPosition = SummonSignValidator.findSafePlayerPosition(host.serverLevel(), cooperator, sign.position());
        if (summonPosition == null) return reject(host, SummonResult.NO_SAFE_POSITION);
        SummonSignRecord claimed = SummonSignManager.claim(host.server, signId).orElse(null);
        if (claimed == null) return reject(host, SummonResult.SIGN_UNAVAILABLE);

        PlayerPhaseState previousCooperatorState = PhaseManager.state(cooperator);
        ReturnContext returnContext = ReturnContext.capture(cooperator, previousCooperatorState);
        PendingReturnSavedData returns = PendingReturnSavedData.get(host.server);
        returns.put(cooperator.getUUID(), returnContext);

        PhaseId hostPhase = PhaseId.solo(host.getUUID());
        CoopSession session = new CoopSession(UUID.randomUUID(), host.getUUID(), cooperator.getUUID(),
                hostPhase, System.currentTimeMillis());
        SESSIONS.put(session.sessionId(), session);
        SESSION_BY_PLAYER.put(host.getUUID(), session.sessionId());
        SESSION_BY_PLAYER.put(cooperator.getUUID(), session.sessionId());

        try {
            SummonSignManager.consumeClaimed(claimed.signId());
            SummonSignManager.removeOwner(host.server, host.getUUID());
            PhaseRoleResolver.refresh(host, "co-op host");
            PhaseRoleResolver.refresh(cooperator, "co-op summon");
            cooperator.teleportTo(host.serverLevel(), summonPosition.x, summonPosition.y, summonPosition.z,
                    claimed.yaw(), cooperator.getXRot());
            host.displayClientMessage(Component.translatable("summon.maplesadventure.session.host_started"), true);
            cooperator.displayClientMessage(Component.translatable("summon.maplesadventure.session.cooperator_started"), true);
            return SummonResult.SUCCESS;
        } catch (RuntimeException exception) {
            MaplesAdventure.LOGGER.error("Failed to create co-op session host={} cooperator={}",
                    host.getGameProfile().getName(), cooperator.getGameProfile().getName(), exception);
            removeSessionIndexes(session);
            session.setState(CoopSessionState.ENDED);
            PhaseRoleResolver.refresh(host, "co-op summon rollback");
            restorePendingReturn(cooperator, "summon rollback");
            PhaseSyncService.changed(host);
            return reject(host, SummonResult.INTERNAL_ERROR);
        }
    }

    public static synchronized boolean endForPlayer(UUID playerUuid, EndReason reason) {
        CoopSession session = session(playerUuid).orElse(null);
        if (session == null) return false;
        return end(session, reason, reason != EndReason.COOPERATOR_LOGOUT);
    }

    public static synchronized boolean dismiss(ServerPlayer host) {
        CoopSession session = session(host.getUUID()).orElse(null);
        if (session == null || !session.hostUuid().equals(host.getUUID())) return false;
        return end(session, EndReason.HOST_DISMISSED, true);
    }

    public static synchronized boolean leave(ServerPlayer cooperator) {
        CoopSession session = session(cooperator.getUUID()).orElse(null);
        if (session == null || !session.cooperatorUuid().equals(cooperator.getUUID())) return false;
        return end(session, EndReason.COOPERATOR_LEFT, true);
    }

    public static synchronized boolean hasSession(UUID playerUuid) {
        return SESSION_BY_PLAYER.containsKey(playerUuid);
    }

    public static synchronized Optional<CoopSession> session(UUID playerUuid) {
        UUID sessionId = SESSION_BY_PLAYER.get(playerUuid);
        return Optional.ofNullable(sessionId == null ? null : SESSIONS.get(sessionId));
    }

    public static synchronized Optional<CoopSession> sessionForPhase(PhaseId phase) {
        return SESSIONS.values().stream().filter(session -> session.state() == CoopSessionState.ACTIVE
                && session.phaseId().equals(phase)).findFirst();
    }

    public static synchronized java.util.List<CoopSession> activeSessions() {
        return SESSIONS.values().stream().filter(session -> session.state() == CoopSessionState.ACTIVE).toList();
    }

    public static synchronized boolean arePartners(UUID first, UUID second) {
        UUID sessionId = SESSION_BY_PLAYER.get(first);
        return sessionId != null && sessionId.equals(SESSION_BY_PLAYER.get(second));
    }

    /** Counts only an authoritative active host/cooperator session; debug phase joins never count. */
    public static synchronized int formalPartySize(MinecraftServer server, PhaseId phase) {
        for (CoopSession session : SESSIONS.values()) {
            if (session.state() != CoopSessionState.ACTIVE || !session.phaseId().equals(phase)) continue;
            ServerPlayer host = server.getPlayerList().getPlayer(session.hostUuid());
            ServerPlayer cooperator = server.getPlayerList().getPlayer(session.cooperatorUuid());
            if (host != null && cooperator != null && isFormalMember(host, phase) && isFormalMember(cooperator, phase)) return 2;
        }
        return 1;
    }

    /** BossBar membership uses formal role/session state, never mere PhaseId equality. */
    public static synchronized boolean isFormalMember(ServerPlayer player, PhaseId phase) {
        PlayerPhaseState state = PhaseManager.state(player);
        if (!state.phaseId().equals(phase)) return false;
        if (state.role() == PhaseRole.SOLO) return phase.equals(PhaseId.solo(player.getUUID()));
        if (state.role() != PhaseRole.HOST && state.role() != PhaseRole.COOPERATOR) return false;
        CoopSession session = session(player.getUUID()).orElse(null);
        return session != null && session.state() == CoopSessionState.ACTIVE && session.phaseId().equals(phase);
    }

    public static synchronized void recoverPendingReturn(ServerPlayer player) {
        if (hasSession(player.getUUID())) return;
        PendingReturnSavedData data = PendingReturnSavedData.get(player.server);
        if (data.get(player.getUUID()).isPresent()) {
            MaplesAdventure.LOGGER.info("Recovering pending co-op return for {} ({} pending record(s))",
                    player.getGameProfile().getName(), data.size());
            restorePendingReturn(player, "login crash recovery");
            player.displayClientMessage(Component.translatable("summon.maplesadventure.session.recovered"), true);
        }
    }

    public static synchronized void clearTransient() {
        SESSIONS.clear();
        SESSION_BY_PLAYER.clear();
    }

    private static boolean end(CoopSession session, EndReason reason, boolean returnOnlineCooperator) {
        if (session.state() != CoopSessionState.ACTIVE) return false;
        session.setState(CoopSessionState.ENDING);
        removeSessionIndexes(session);
        MinecraftServer server = serverFor(session);
        if (server == null) {
            session.setState(CoopSessionState.ENDED);
            return true;
        }

        ServerPlayer host = server.getPlayerList().getPlayer(session.hostUuid());
        ServerPlayer cooperator = server.getPlayerList().getPlayer(session.cooperatorUuid());
        SummonSignManager.removeOwner(server, session.hostUuid());
        SummonSignManager.removeOwner(server, session.cooperatorUuid());

        if (host != null) {
            PhaseRoleResolver.refresh(host, "co-op ended: " + reason.name());
            host.displayClientMessage(Component.translatable("summon.maplesadventure.session.ended"), true);
        }
        if (cooperator != null && returnOnlineCooperator) {
            restorePendingReturn(cooperator, "session end: " + reason.name());
            cooperator.displayClientMessage(Component.translatable("summon.maplesadventure.session.returned"), true);
        }
        session.setState(CoopSessionState.ENDED);
        MaplesAdventure.LOGGER.debug("Ended co-op session {} reason={}", session.sessionId(), reason);
        return true;
    }

    private static void restorePendingReturn(ServerPlayer player, String reason) {
        // Combat state from the foreign world must not survive return or pending-return recovery.
        dev.maplesadventure.progression.status.StatusRuntimeService.clearAll(player,
                dev.maplesadventure.progression.status.StatusRuntimeService.ClearReason.SESSION_RETURN);
        PendingReturnSavedData data = PendingReturnSavedData.get(player.server);
        ReturnContext context = data.get(player.getUUID()).orElse(null);
        if (context == null) {
            PhaseManager.assignSolo(player);
            PhaseSyncService.changed(player);
            return;
        }
        ServerLevel targetLevel = player.server.getLevel(context.dimension());
        if (targetLevel == null) {
            MaplesAdventure.LOGGER.error("Cannot restore co-op return for {}: missing dimension {}",
                    player.getGameProfile().getName(), context.dimension().location());
            PhaseManager.assignSolo(player);
            PhaseSyncService.changed(player);
            return;
        }
        Vec3 safe = SummonSignValidator.findSafePlayerPosition(targetLevel, player, context.position());
        if (safe == null) safe = context.position();
        player.teleportTo(targetLevel, safe.x, safe.y, safe.z, context.yaw(), context.pitch());
        PhaseManager.assignSolo(player);
        PhaseSyncService.changed(player);
        data.remove(player.getUUID());
        MaplesAdventure.LOGGER.debug("Restored co-op return for {} reason={}", player.getGameProfile().getName(), reason);
    }

    private static boolean isTrueSolo(ServerPlayer player) {
        PlayerPhaseState state = PhaseManager.state(player);
        return state.role() == PhaseRole.SOLO && state.phaseId().equals(PhaseId.solo(player.getUUID()));
    }

    private static SummonResult reject(ServerPlayer player, SummonResult result) {
        player.displayClientMessage(Component.translatable("summon.maplesadventure.failure."
                + result.name().toLowerCase(java.util.Locale.ROOT)), true);
        return result;
    }

    private static MinecraftServer serverFor(CoopSession session) {
        return net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
    }

    private static void removeSessionIndexes(CoopSession session) {
        SESSIONS.remove(session.sessionId());
        SESSION_BY_PLAYER.remove(session.hostUuid(), session.sessionId());
        SESSION_BY_PLAYER.remove(session.cooperatorUuid(), session.sessionId());
    }

    public enum SummonResult {
        SUCCESS,
        SIGN_UNAVAILABLE,
        OWN_SIGN,
        HOST_NOT_SOLO,
        COOPERATOR_UNAVAILABLE,
        COOPERATOR_NOT_SOLO,
        WRONG_DIMENSION,
        OUT_OF_RANGE_OR_BLOCKED,
        NO_SAFE_POSITION,
        BOSS_ATTEMPT_ACTIVE,
        INTERNAL_ERROR
    }

    public enum EndReason {
        COOPERATOR_LEFT,
        HOST_DISMISSED,
        COOPERATOR_DEATH,
        HOST_DEATH,
        HOST_LOGOUT,
        COOPERATOR_LOGOUT,
        DIMENSION_CHANGE,
        ADMIN_FORCED,
        BOSS_DEFEATED
    }

    private CoopSessionManager() {}
}
