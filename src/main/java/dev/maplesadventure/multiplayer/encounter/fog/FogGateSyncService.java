package dev.maplesadventure.multiplayer.encounter.fog;

import dev.maplesadventure.multiplayer.encounter.EncounterSavedData;
import dev.maplesadventure.multiplayer.encounter.EncounterStatus;
import dev.maplesadventure.multiplayer.encounter.fog.network.FogGatePayloads;
import dev.maplesadventure.multiplayer.phase.PhaseId;
import dev.maplesadventure.multiplayer.phase.PhaseManager;
import dev.maplesadventure.multiplayer.phase.PhaseRole;
import dev.maplesadventure.multiplayer.coop.CoopSessionManager;
import dev.maplesadventure.multiplayer.encounter.boss.BossParticipantState;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.PacketDistributor;

public final class FogGateSyncService {
    private static final double RANGE = 96.0D;
    private static final Map<UUID, SyncState> STATES = new HashMap<>();

    public static void tick(ServerPlayer player) {
        if (player.tickCount % 20 != 0) return;
        ChunkPos chunk = player.chunkPosition();
        SyncState old = STATES.get(player.getUUID());
        if (old == null || !old.dimension.equals(player.level().dimension()) || old.chunkX != chunk.x || old.chunkZ != chunk.z
                || player.level().getGameTime() - old.tick >= 100L) syncNow(player);
    }
    public static void syncNow(ServerPlayer player) {
        EncounterSavedData data = EncounterSavedData.get(player.server);
        PhaseId phase = PhaseManager.state(player).phaseId();
        PhaseRole role = PhaseManager.state(player).role();
        List<FogGateClientView> views = data.fogGates().stream()
                .filter(gate -> gate.dimension().equals(player.level().dimension()) && near(player, gate))
                .map(gate -> {
                    var definition = data.definition(gate.bossEncounterId()).orElse(null);
                    var state = definition == null ? null : data.state(phase, definition);
                    EncounterStatus status = state == null ? EncounterStatus.READY : state.status();
                    boolean invader = role == PhaseRole.INVADER;
                    if (status == EncounterStatus.ACTIVE && definition != null)
                        BossParticipantState.migrateLegacyAttempt(player.server, phase, definition.encounterId());
                    boolean entered = status == EncounterStatus.ACTIVE && definition != null
                            && BossParticipantState.isEntered(player.server, phase, definition.encounterId(), player.getUUID());
                    boolean waiting = status == EncounterStatus.ACTIVE && role == PhaseRole.COOPERATOR
                            && CoopSessionManager.isFormalMember(player, phase) && !entered;
                    if (waiting) FogTraversalManager.ensureCooperatorPass(player, gate, phase);
                    boolean pass = !invader && (status == EncounterStatus.DEFEATED
                            || FogTraversalManager.hasPass(player.getUUID(), gate.gateId()));
                    boolean render = invader || status != EncounterStatus.DEFEATED && !waiting;
                    boolean interact = status == EncounterStatus.READY && definition != null
                            && FogGateDiagnostics.validPrimarySide(definition, gate)
                            && gate.side(player.getBoundingBox().getCenter()) == FogGateDefinition.GateSide.OUTSIDE
                            && (role == PhaseRole.SOLO || role == PhaseRole.HOST);
                    return new FogGateClientView(gate.gateId(), gate.bossEncounterId(), gate.fogBlocks(), gate.facing(),
                            render, pass, interact);
                }).toList();
        PacketDistributor.sendToPlayer(player, new FogGatePayloads.Snapshot(views));
        ChunkPos chunk = player.chunkPosition();
        STATES.put(player.getUUID(), new SyncState(player.level().dimension(), chunk.x, chunk.z, player.level().getGameTime()));
    }
    public static void syncPhase(MinecraftServer server, PhaseId phase) {
        for (ServerPlayer player : server.getPlayerList().getPlayers())
            if (PhaseManager.state(player).phaseId().equals(phase)) syncNow(player);
    }
    public static void forget(UUID player) { STATES.remove(player); }
    public static void clear() { STATES.clear(); }
    private static boolean near(ServerPlayer player, FogGateDefinition gate) {
        double max = RANGE * RANGE;
        for (long packed : gate.fogBlocks()) if (player.distanceToSqr(net.minecraft.core.BlockPos.of(packed).getCenter()) <= max) return true;
        return false;
    }
    private record SyncState(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
                             int chunkX, int chunkZ, long tick) {}
    private FogGateSyncService() {}
}
