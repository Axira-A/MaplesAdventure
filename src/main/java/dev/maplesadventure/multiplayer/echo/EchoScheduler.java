package dev.maplesadventure.multiplayer.echo;

import dev.maplesadventure.config.EchoServerConfig;
import dev.maplesadventure.multiplayer.phase.PhaseId;
import dev.maplesadventure.multiplayer.phase.PhaseRelations;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Low-frequency, spatially bounded selection. It never requests trajectories from clients. */
public final class EchoScheduler {
    private static final int SCHEDULE_INTERVAL = 20;
    private static final int RELATIONSHIP_COOLDOWN_TICKS = 200;
    private static final Map<UUID, Long> NEXT_ELIGIBLE = new HashMap<>();
    private static final Map<UUID, UUID> LAST_SOURCE = new HashMap<>();
    private static final Map<UUID, PhaseId> LAST_PHASE = new HashMap<>();
    private static final Map<UUID, Long> RELATIONSHIP_COOLDOWN_UNTIL = new HashMap<>();

    public static void tick(MinecraftServer server) {
        long now = server.getTickCount();
        observePhaseChanges(server, now);
        if (!EchoServerConfig.ENABLED.get() || now % SCHEDULE_INTERVAL != 0) return;
        for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
            if (now < NEXT_ELIGIBLE.computeIfAbsent(viewer.getUUID(), ignored -> now + randomInterval(viewer))) continue;
            scheduleNext(viewer, now);
            if (now < RELATIONSHIP_COOLDOWN_UNTIL.getOrDefault(viewer.getUUID(), 0L)) continue;
            ServerPlayer source = chooseSource(viewer, now);
            if (source != null && EchoSyncService.sendPlayback(viewer, source, false)) {
                LAST_SOURCE.put(viewer.getUUID(), source.getUUID());
            }
        }
    }

    private static ServerPlayer chooseSource(ServerPlayer viewer, long now) {
        double range = EchoServerConfig.CANDIDATE_RANGE.get();
        ServerLevel level = viewer.serverLevel();
        List<ServerPlayer> candidates = level.getEntitiesOfClass(ServerPlayer.class,
                viewer.getBoundingBox().inflate(range), source -> source != viewer
                        && source.distanceToSqr(viewer) <= range * range
                        && !PhaseRelations.canSee(viewer, source)
                        && now >= RELATIONSHIP_COOLDOWN_UNTIL.getOrDefault(source.getUUID(), 0L)
                        && PlayerEchoRecorder.frameCount(source.getUUID()) >= 4);
        if (candidates.isEmpty()) return null;
        UUID last = LAST_SOURCE.get(viewer.getUUID());
        candidates.sort((left, right) -> {
            boolean leftRepeated = left.getUUID().equals(last);
            boolean rightRepeated = right.getUUID().equals(last);
            if (leftRepeated != rightRepeated) return leftRepeated ? 1 : -1;
            return Double.compare(left.distanceToSqr(viewer), right.distanceToSqr(viewer));
        });
        int selectionWindow = Math.min(3, candidates.size());
        return candidates.get(viewer.getRandom().nextInt(selectionWindow));
    }

    private static void observePhaseChanges(MinecraftServer server, long now) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PhaseId phase = PhaseRelations.state(player).phaseId();
            PhaseId previous = LAST_PHASE.put(player.getUUID(), phase);
            if (previous != null && !previous.equals(phase)) {
                RELATIONSHIP_COOLDOWN_UNTIL.put(player.getUUID(), now + RELATIONSHIP_COOLDOWN_TICKS);
            }
        }
    }

    private static void scheduleNext(ServerPlayer viewer, long now) {
        NEXT_ELIGIBLE.put(viewer.getUUID(), now + randomInterval(viewer));
    }
    private static int randomInterval(ServerPlayer viewer) {
        int minimum = EchoServerConfig.MIN_INTERVAL_SECONDS.get();
        int maximum = Math.max(minimum, EchoServerConfig.MAX_INTERVAL_SECONDS.get());
        return (minimum + viewer.getRandom().nextInt(maximum - minimum + 1)) * 20;
    }
    public static long nextEligible(UUID playerId) { return NEXT_ELIGIBLE.getOrDefault(playerId, 0L); }
    public static void forget(UUID playerId) {
        NEXT_ELIGIBLE.remove(playerId); LAST_SOURCE.remove(playerId); LAST_PHASE.remove(playerId);
        RELATIONSHIP_COOLDOWN_UNTIL.remove(playerId);
    }
    public static void clear() {
        NEXT_ELIGIBLE.clear(); LAST_SOURCE.clear(); LAST_PHASE.clear(); RELATIONSHIP_COOLDOWN_UNTIL.clear();
    }
    private EchoScheduler() {}
}
