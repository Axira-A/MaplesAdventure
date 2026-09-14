package dev.maplesadventure.multiplayer.invasion;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.config.InvasionConfig;
import dev.maplesadventure.multiplayer.coop.CoopSession;
import dev.maplesadventure.multiplayer.coop.CoopSessionManager;
import dev.maplesadventure.multiplayer.coop.ReturnContext;
import dev.maplesadventure.multiplayer.coop.SummonSignManager;
import dev.maplesadventure.multiplayer.coop.SummonSignValidator;
import dev.maplesadventure.multiplayer.encounter.fog.BossGateEntryCoordinator;
import dev.maplesadventure.multiplayer.phase.PhaseId;
import dev.maplesadventure.multiplayer.phase.PhaseManager;
import dev.maplesadventure.multiplayer.phase.PhaseRoleResolver;
import dev.maplesadventure.multiplayer.phase.PhaseSyncService;
import dev.maplesadventure.multiplayer.phase.PlayerPhaseState;
import dev.maplesadventure.multiplayer.invasion.network.InvasionPayloads;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server-thread session authority. All claims and teardown operations are synchronized and idempotent. */
public final class InvasionSessionManager implements BossGateEntryCoordinator {
    public static final InvasionSessionManager INSTANCE = new InvasionSessionManager();
    private static final int MATERIALIZE_TICKS = 30;
    private static final Map<UUID, InvasionSession> SESSIONS = new LinkedHashMap<>();
    private static final Map<UUID, UUID> BY_PLAYER = new LinkedHashMap<>();
    private static final Map<UUID, UUID> BY_HOST = new LinkedHashMap<>();
    private static final Map<UUID, UUID> BY_COOPERATOR = new LinkedHashMap<>();
    private static final Map<UUID, Long> HOST_COOLDOWN_UNTIL = new LinkedHashMap<>();

    public static synchronized StartResult start(ServerPlayer invader, CoopSession coop) {
        InvasionEligibilityPolicy.Failure invaderFailure = InvasionEligibilityPolicy.invader(invader);
        if (invaderFailure != InvasionEligibilityPolicy.Failure.NONE) return StartResult.INVADER_INELIGIBLE;
        if (InvasionEligibilityPolicy.host(invader.server, coop) != InvasionEligibilityPolicy.Failure.NONE)
            return StartResult.HOST_INELIGIBLE;
        ServerPlayer host = invader.server.getPlayerList().getPlayer(coop.hostUuid());
        ServerPlayer cooperator = invader.server.getPlayerList().getPlayer(coop.cooperatorUuid());
        if (host == null || cooperator == null || BY_HOST.containsKey(host.getUUID())) return StartResult.CLAIMED;
        Vec3 spawn = InvasionSpawnResolver.resolve(host, invader);
        if (spawn == null) return StartResult.NO_SAFE_POSITION;

        ReturnContext context = ReturnContext.capture(invader, PhaseManager.state(invader));
        PendingInvasionReturnSavedData.get(invader.server).put(invader.getUUID(), context);
        InvasionSession session = new InvasionSession(UUID.randomUUID(), host.getUUID(), cooperator.getUUID(),
                invader.getUUID(), coop.phaseId(), HostileSessionType.INVASION, System.currentTimeMillis());
        SESSIONS.put(session.sessionId(), session);
        BY_PLAYER.put(host.getUUID(), session.sessionId());
        BY_PLAYER.put(invader.getUUID(), session.sessionId());
        BY_HOST.put(host.getUUID(), session.sessionId());
        BY_COOPERATOR.put(cooperator.getUUID(), session.sessionId());
        try {
            InvasionQueueManager.remove(invader.getUUID());
            SummonSignManager.removeOwner(invader.server, invader.getUUID());
            invader.teleportTo(host.serverLevel(), spawn.x, spawn.y, spawn.z, invader.getYRot(), invader.getXRot());
            PhaseManager.set(invader, new PlayerPhaseState(coop.phaseId(), dev.maplesadventure.multiplayer.phase.PhaseRole.INVADER),
                    "invasion materializing");
            PhaseSyncService.changed(invader);
            PhaseRoleResolver.refresh(host, "hosting invasion");
            session.materialize(invader.server.getTickCount(), MATERIALIZE_TICKS);
            sendMaterialize(session, spawn, invader.getYRot());
            host.displayClientMessage(Component.translatable("invasion.maplesadventure.host_invaded"), true);
            cooperator.displayClientMessage(Component.translatable("invasion.maplesadventure.host_invaded"), true);
            invader.displayClientMessage(Component.translatable("invasion.maplesadventure.materializing"), true);
            return StartResult.SUCCESS;
        } catch (RuntimeException exception) {
            MaplesAdventure.LOGGER.error("Failed to start invasion host={} invader={}", host.getUUID(), invader.getUUID(), exception);
            removeIndexes(session);
            session.setState(InvasionSessionState.ENDED);
            restore(invader, "invasion rollback");
            PhaseRoleResolver.refresh(host, "invasion rollback");
            return StartResult.INTERNAL_ERROR;
        }
    }

    /** Explicit 1v1 consent through a red sign. The hostile runtime is shared with random invasions. */
    public static synchronized StartResult startDuel(ServerPlayer host, UUID signId) {
        dev.maplesadventure.multiplayer.coop.SummonSignRecord sign = SummonSignManager.find(signId).orElse(null);
        if (sign == null || sign.type() != dev.maplesadventure.multiplayer.coop.SummonSignType.DUEL
                || sign.state() != dev.maplesadventure.multiplayer.coop.SummonSignState.AVAILABLE)
            return rejectDuel(host, StartResult.CLAIMED);
        if (sign.ownerUuid().equals(host.getUUID())) return rejectDuel(host, StartResult.INVADER_INELIGIBLE);
        if (!duelEligible(host) || hasSession(host.getUUID()) || CoopSessionManager.hasSession(host.getUUID())
                || dev.maplesadventure.multiplayer.encounter.EncounterManager.hasActiveBossAttempt(
                host.server, PhaseManager.state(host).phaseId())) return rejectDuel(host, StartResult.HOST_INELIGIBLE);
        ServerPlayer duelist = host.server.getPlayerList().getPlayer(sign.ownerUuid());
        if (duelist == null || !duelEligible(duelist) || hasSession(duelist.getUUID())
                || CoopSessionManager.hasSession(duelist.getUUID())
                || dev.maplesadventure.multiplayer.encounter.EncounterManager.hasActiveBossAttempt(
                host.server, PhaseManager.state(duelist).phaseId()))
            return rejectDuel(host, StartResult.INVADER_INELIGIBLE);
        if (!host.level().dimension().equals(sign.dimension()) || !duelist.level().dimension().equals(sign.dimension())
                || !SummonSignValidator.validateForSummon(host, sign))
            return rejectDuel(host, StartResult.HOST_INELIGIBLE);
        Vec3 spawn = SummonSignValidator.findSafePlayerPosition(host.serverLevel(), duelist, sign.position());
        if (spawn == null) return rejectDuel(host, StartResult.NO_SAFE_POSITION);
        dev.maplesadventure.multiplayer.coop.SummonSignRecord claimed = SummonSignManager.claim(host.server, signId).orElse(null);
        if (claimed == null) return rejectDuel(host, StartResult.CLAIMED);

        ReturnContext context = ReturnContext.capture(duelist, PhaseManager.state(duelist));
        PendingInvasionReturnSavedData.get(host.server).put(duelist.getUUID(), context);
        PhaseId hostPhase = PhaseId.solo(host.getUUID());
        InvasionSession session = new InvasionSession(UUID.randomUUID(), host.getUUID(), InvasionSession.NO_COOPERATOR,
                duelist.getUUID(), hostPhase, HostileSessionType.DUEL, System.currentTimeMillis());
        index(session);
        try {
            InvasionQueueManager.remove(host.getUUID());
            InvasionQueueManager.remove(duelist.getUUID());
            SummonSignManager.removeOwner(host.server, host.getUUID());
            SummonSignManager.removeOwner(host.server, duelist.getUUID());
            SummonSignManager.consumeClaimed(claimed.signId());
            duelist.teleportTo(host.serverLevel(), spawn.x, spawn.y, spawn.z, claimed.yaw(), duelist.getXRot());
            PhaseManager.set(duelist, new PlayerPhaseState(hostPhase, dev.maplesadventure.multiplayer.phase.PhaseRole.INVADER),
                    "duel materializing");
            PhaseSyncService.changed(duelist);
            PhaseRoleResolver.refresh(host, "hosting duel");
            session.materialize(host.server.getTickCount(), MATERIALIZE_TICKS);
            sendMaterialize(session, spawn, claimed.yaw());
            host.displayClientMessage(Component.translatable("duel.maplesadventure.session.host_started"), true);
            duelist.displayClientMessage(Component.translatable("duel.maplesadventure.session.duelist_started"), true);
            return StartResult.SUCCESS;
        } catch (RuntimeException exception) {
            MaplesAdventure.LOGGER.error("Failed to start duel host={} duelist={}", host.getUUID(), duelist.getUUID(), exception);
            removeIndexes(session); session.setState(InvasionSessionState.ENDED);
            restore(duelist, "duel rollback"); PhaseRoleResolver.refresh(host, "duel rollback");
            return rejectDuel(host, StartResult.INTERNAL_ERROR);
        }
    }

    public static synchronized void tick(MinecraftServer server) {
        long nowTick = server.getTickCount();
        long timeoutMillis = InvasionConfig.TIMEOUT_SECONDS.get() * 1000L;
        for (InvasionSession session : List.copyOf(SESSIONS.values())) {
            if (session.state() == InvasionSessionState.MATERIALIZING && nowTick >= session.materializeUntilTick()) {
                session.activate();
                ServerPlayer invader = server.getPlayerList().getPlayer(session.invaderUuid());
                if (invader != null) invader.displayClientMessage(Component.translatable(
                        session.type() == HostileSessionType.DUEL
                                ? "duel.maplesadventure.active" : "invasion.maplesadventure.active"), true);
            }
            if ((session.state() == InvasionSessionState.ACTIVE || session.state() == InvasionSessionState.MATERIALIZING)
                    && System.currentTimeMillis() - session.createdAt() >= timeoutMillis) {
                end(session, EndReason.TIMEOUT, true);
            }
        }
        HOST_COOLDOWN_UNTIL.entrySet().removeIf(entry -> entry.getValue() <= nowTick);
    }

    public static synchronized Optional<InvasionSession> session(UUID player) {
        UUID id = BY_PLAYER.get(player);
        return Optional.ofNullable(id == null ? null : SESSIONS.get(id));
    }
    public static synchronized Optional<InvasionSession> sessionForHost(UUID host) {
        UUID id = BY_HOST.get(host);
        return Optional.ofNullable(id == null ? null : SESSIONS.get(id));
    }
    public static synchronized Optional<InvasionSession> sessionForPhase(PhaseId phase) {
        return SESSIONS.values().stream().filter(session -> session.hostPhaseId().equals(phase)
                && session.state() != InvasionSessionState.ENDED).findFirst();
    }
    public static synchronized List<InvasionSession> activeSessions() { return List.copyOf(SESSIONS.values()); }
    public static synchronized boolean hasSession(UUID player) { return BY_PLAYER.containsKey(player); }
    public static synchronized boolean isInvader(UUID player) {
        InvasionSession session = session(player).orElse(null);
        return session != null && session.invaderUuid().equals(player);
    }
    public static synchronized boolean areOpponents(UUID first, UUID second) {
        InvasionSession session = participantSession(first).orElseGet(() -> participantSession(second).orElse(null));
        if (session == null || !isParticipant(session, first) || !isParticipant(session, second)) return false;
        boolean firstInvader = session.invaderUuid().equals(first);
        boolean secondInvader = session.invaderUuid().equals(second);
        return firstInvader != secondInvader && (session.hostUuid().equals(first) || session.cooperatorUuid().equals(first)
                || session.hostUuid().equals(second) || session.cooperatorUuid().equals(second));
    }
    public static synchronized boolean isCombatActive(UUID player) {
        InvasionSession session = participantSession(player).orElse(null);
        return session != null && session.state() == InvasionSessionState.ACTIVE;
    }
    public static synchronized boolean isMaterializing(UUID player) {
        InvasionSession session = session(player).orElse(null);
        return session != null && session.state() == InvasionSessionState.MATERIALIZING;
    }
    public static synchronized boolean isCoolingDown(MinecraftServer server, UUID host) {
        return HOST_COOLDOWN_UNTIL.getOrDefault(host, 0L) > server.getTickCount();
    }

    public static synchronized boolean endForPlayer(UUID player, EndReason reason) {
        InvasionSession session = session(player).orElse(null);
        return session != null && end(session, reason, reason != EndReason.INVADER_LOGOUT);
    }

    private static boolean end(InvasionSession session, EndReason reason, boolean returnOnlineInvader) {
        if (session.state() == InvasionSessionState.ENDING || session.state() == InvasionSessionState.ENDED) return true;
        session.setState(InvasionSessionState.ENDING);
        MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        ServerPlayer host = server == null ? null : server.getPlayerList().getPlayer(session.hostUuid());
        ServerPlayer invader = server == null ? null : server.getPlayerList().getPlayer(session.invaderUuid());
        if (invader != null && returnOnlineInvader) restore(invader, "invasion end: " + reason.name());
        removeIndexes(session);
        session.setState(InvasionSessionState.ENDED);
        if (server != null) {
            if (session.type() == HostileSessionType.INVASION)
                HOST_COOLDOWN_UNTIL.put(session.hostUuid(), (long) server.getTickCount()
                        + InvasionConfig.COOLDOWN_SECONDS.get() * 20L);
            if (host != null) PhaseRoleResolver.refresh(host, "invasion ended: " + reason.name());
            sendClear(server, session.invaderUuid());
        }
        if (invader != null && returnOnlineInvader) {
            invader.displayClientMessage(Component.translatable("invasion.maplesadventure.returned"), true);
        }
        MaplesAdventure.LOGGER.debug("Ended invasion {} reason={}", session.sessionId(), reason);
        return true;
    }

    public static synchronized void recoverPendingReturn(ServerPlayer player) {
        if (hasSession(player.getUUID())) return;
        if (PendingInvasionReturnSavedData.get(player.server).contains(player.getUUID())) {
            restore(player, "login crash recovery");
            player.displayClientMessage(Component.translatable("invasion.maplesadventure.recovered"), true);
        }
    }

    private static void restore(ServerPlayer player, String reason) {
        PendingInvasionReturnSavedData data = PendingInvasionReturnSavedData.get(player.server);
        ReturnContext context = data.get(player.getUUID()).orElse(null);
        if (context == null) {
            PhaseManager.assignSolo(player);
            PhaseSyncService.changed(player);
            return;
        }
        ServerLevel level = player.server.getLevel(context.dimension());
        if (level != null) {
            Vec3 safe = SummonSignValidator.findSafePlayerPosition(level, player, context.position());
            if (safe == null) safe = context.position();
            player.teleportTo(level, safe.x, safe.y, safe.z, context.yaw(), context.pitch());
        }
        PhaseManager.set(player, new PlayerPhaseState(context.originalPhase(), context.originalRole()), "invasion return");
        PhaseSyncService.changed(player);
        data.remove(player.getUUID());
        MaplesAdventure.LOGGER.debug("Restored invasion return player={} reason={}", player.getUUID(), reason);
    }

    private static void sendMaterialize(InvasionSession session, Vec3 position, float yaw) {
        MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        InvasionPayloads.Materialize payload = new InvasionPayloads.Materialize(session.invaderUuid(), position, yaw, MATERIALIZE_TICKS);
        send(server, session.invaderUuid(), new InvasionPayloads.Clear(session.hostUuid()));
        if (session.hasCooperator()) send(server, session.invaderUuid(), new InvasionPayloads.Clear(session.cooperatorUuid()));
        send(server, session.hostUuid(), payload);
        if (session.hasCooperator()) send(server, session.cooperatorUuid(), payload);
        send(server, session.invaderUuid(), payload);
    }
    private static void sendClear(MinecraftServer server, UUID source) {
        InvasionPayloads.Clear payload = new InvasionPayloads.Clear(source);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) PacketDistributor.sendToPlayer(player, payload);
    }
    private static void send(MinecraftServer server, UUID player, net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        ServerPlayer target = server.getPlayerList().getPlayer(player);
        if (target != null) PacketDistributor.sendToPlayer(target, payload);
    }
    private static void removeIndexes(InvasionSession session) {
        SESSIONS.remove(session.sessionId());
        BY_PLAYER.remove(session.hostUuid(), session.sessionId());
        BY_PLAYER.remove(session.invaderUuid(), session.sessionId());
        BY_HOST.remove(session.hostUuid(), session.sessionId());
        if (session.hasCooperator()) BY_COOPERATOR.remove(session.cooperatorUuid(), session.sessionId());
    }

    private static Optional<InvasionSession> participantSession(UUID player) {
        UUID id = BY_PLAYER.get(player);
        if (id == null) id = BY_COOPERATOR.get(player);
        return Optional.ofNullable(id == null ? null : SESSIONS.get(id));
    }
    private static boolean isParticipant(InvasionSession session, UUID player) {
        return session.hostUuid().equals(player) || session.hasCooperator() && session.cooperatorUuid().equals(player)
                || session.invaderUuid().equals(player);
    }

    private static void index(InvasionSession session) {
        SESSIONS.put(session.sessionId(), session);
        BY_PLAYER.put(session.hostUuid(), session.sessionId());
        BY_PLAYER.put(session.invaderUuid(), session.sessionId());
        BY_HOST.put(session.hostUuid(), session.sessionId());
        if (session.hasCooperator()) BY_COOPERATOR.put(session.cooperatorUuid(), session.sessionId());
    }

    private static boolean trueSolo(ServerPlayer player) {
        PlayerPhaseState state = PhaseManager.state(player);
        return state.role() == dev.maplesadventure.multiplayer.phase.PhaseRole.SOLO
                && state.phaseId().equals(PhaseId.solo(player.getUUID()));
    }

    private static boolean duelEligible(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && !player.isCreative() && trueSolo(player)
                && !PendingInvasionReturnSavedData.get(player.server).contains(player.getUUID())
                && !dev.maplesadventure.multiplayer.coop.PendingReturnSavedData.get(player.server).contains(player.getUUID());
    }

    private static StartResult rejectDuel(ServerPlayer host, StartResult result) {
        host.displayClientMessage(Component.translatable("duel.maplesadventure.failure." + result.name().toLowerCase(java.util.Locale.ROOT)), true);
        return result;
    }

    public static synchronized void clearTransient() {
        SESSIONS.clear(); BY_PLAYER.clear(); BY_HOST.clear(); BY_COOPERATOR.clear(); HOST_COOLDOWN_UNTIL.clear();
    }

    @Override public boolean endActiveInvasion(MinecraftServer server, PhaseId phase, Reason reason) {
        synchronized (InvasionSessionManager.class) {
            InvasionSession session = sessionForPhase(phase).orElse(null);
            if (session == null) return true;
            end(session, EndReason.BOSS_GATE_ENTRY, true);
            ServerPlayer invader = server.getPlayerList().getPlayer(session.invaderUuid());
            return invader == null || !PhaseManager.state(invader).phaseId().equals(phase);
        }
    }

    public enum StartResult { SUCCESS, INVADER_INELIGIBLE, HOST_INELIGIBLE, CLAIMED, NO_SAFE_POSITION, INTERNAL_ERROR }
    public enum EndReason { INVADER_DEATH, HOST_DEATH, INVADER_LOGOUT, HOST_LOGOUT, DIMENSION_CHANGE,
        TIMEOUT, ADMIN_FORCED, BOSS_GATE_ENTRY, SERVER_STOPPING }
    private InvasionSessionManager() {}
}
