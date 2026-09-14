package dev.maplesadventure.multiplayer.encounter.fog;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.multiplayer.coop.CoopSessionManager;
import dev.maplesadventure.multiplayer.encounter.EncounterDefinition;
import dev.maplesadventure.multiplayer.encounter.EncounterManager;
import dev.maplesadventure.multiplayer.encounter.EncounterSavedData;
import dev.maplesadventure.multiplayer.encounter.EncounterStatus;
import dev.maplesadventure.multiplayer.encounter.EncounterType;
import dev.maplesadventure.multiplayer.encounter.boss.BossParticipantEntryState;
import dev.maplesadventure.multiplayer.encounter.boss.BossParticipantState;
import dev.maplesadventure.multiplayer.phase.PhaseId;
import dev.maplesadventure.multiplayer.phase.PhaseManager;
import dev.maplesadventure.multiplayer.phase.PhaseRole;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import dev.maplesadventure.multiplayer.encounter.fog.network.FogGatePayloads;

public final class FogTraversalManager {
    private static final Map<UUID, Traversal> TRAVERSALS = new HashMap<>();
    private static final Map<UUID, CooperatorPass> COOPERATOR_PASSES = new HashMap<>();
    private static BossGateEntryCoordinator invasionCoordinator = BossGateEntryCoordinator.NONE;

    public static boolean request(ServerPlayer player, UUID gateId) {
        EncounterSavedData data = EncounterSavedData.get(player.server);
        FogGateDefinition gate = data.fogGates().stream().filter(value -> value.gateId().equals(gateId)).findFirst().orElse(null);
        if (gate == null || !gate.dimension().equals(player.level().dimension()) || !gate.isRuntimeValid(player.serverLevel())) return false;
        EncounterDefinition encounter = data.definition(gate.bossEncounterId()).orElse(null);
        if (encounter == null || encounter.type() != EncounterType.BOSS || !FogGateDiagnostics.validPrimarySide(encounter, gate)
                || gate.side(player.getBoundingBox().getCenter()) != FogGateDefinition.GateSide.OUTSIDE) return false;
        PhaseRole role = PhaseManager.state(player).role();
        if (role != PhaseRole.SOLO && role != PhaseRole.HOST) return false;
        PhaseId phase = PhaseManager.state(player).phaseId();
        var state = data.state(phase, encounter);
        if (state.status() != EncounterStatus.READY || TRAVERSALS.containsKey(player.getUUID())) return false;
        BlockPos nearest = nearestGateBlock(player, gate);
        if (nearest == null || !withinRange(player, nearest) || !lineOfSight(player, gate, nearest)) return false;
        if (!invasionCoordinator.endActiveInvasion(player.server, phase, BossGateEntryCoordinator.Reason.BOSS_GATE_ENTRY)) return false;
        long now = player.server.getTickCount();
        TRAVERSALS.put(player.getUUID(), new Traversal(gate.gateId(), phase, now, now + 30L,
                gate.signedDistanceToInside(player.getBoundingBox().getCenter())));
        PacketDistributor.sendToPlayer(player, new FogGatePayloads.StartTraversal(gate.insideDirection(), 30));
        FogGateSyncService.syncNow(player);
        return true;
    }

    public static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Traversal>> iterator = TRAVERSALS.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            Traversal traversal = entry.getValue();
            FogGateDefinition gate = find(server, traversal.gateId());
            if (player == null || gate == null || server.getTickCount() > traversal.deadline()
                    || !PhaseManager.state(player).phaseId().equals(traversal.phase())) {
                iterator.remove();
                if (player != null) {
                    PacketDistributor.sendToPlayer(player, new FogGatePayloads.StopTraversal());
                    FogGateSyncService.syncNow(player);
                }
                continue;
            }
            double signedDistance = gate.signedDistanceToInside(player.getBoundingBox().getCenter());
            if (traversal.lastSignedDistance <= 0.0D && signedDistance > 0.0D) {
                iterator.remove();
                finish(player, gate, traversal.phase());
                continue;
            }
            traversal.lastSignedDistance = signedDistance;
            Vec3 direction = Vec3.atLowerCornerOf(gate.insideDirection().getNormal()).scale(0.14D);
            player.setDeltaMovement(direction.x, player.getDeltaMovement().y, direction.z);
            player.hurtMarked = true;
        }

        Iterator<Map.Entry<UUID, CooperatorPass>> passIterator = COOPERATOR_PASSES.entrySet().iterator();
        while (passIterator.hasNext()) {
            var entry = passIterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            CooperatorPass pass = entry.getValue();
            FogGateDefinition gate = find(server, pass.gateId);
            if (player == null || gate == null || PhaseManager.state(player).role() != PhaseRole.COOPERATOR
                    || !PhaseManager.state(player).phaseId().equals(pass.phase)
                    || BossParticipantState.entryState(server, pass.phase, pass.encounterId, player.getUUID())
                    != BossParticipantEntryState.OUTSIDE) {
                passIterator.remove();
            } else {
                double signedDistance = gate.signedDistanceToInside(player.getBoundingBox().getCenter());
                if (pass.lastSignedDistance <= 0.0D && signedDistance > 0.0D) {
                    BossParticipantState.markEntered(server, pass.phase, pass.encounterId, player);
                    passIterator.remove();
                    FogGateSyncService.syncNow(player);
                    continue;
                }
                pass.lastSignedDistance = signedDistance;
            }
        }
        BossVictoryReturnService.tick(server);
    }

    private static void finish(ServerPlayer host, FogGateDefinition gate, PhaseId phase) {
        PacketDistributor.sendToPlayer(host, new FogGatePayloads.StopTraversal());
        EncounterSavedData data = EncounterSavedData.get(host.server);
        EncounterDefinition definition = data.definition(gate.bossEncounterId()).orElse(null);
        if (definition == null) return;
        var state = data.state(phase, definition);
        if (state.status() == EncounterStatus.READY && EncounterManager.activate(host.server, data, definition, phase, state)) {
            BossParticipantState.initialize(host.server, phase, definition.encounterId(), host.getUUID());
            CoopSessionManager.session(host.getUUID()).ifPresent(session -> {
                if (session.hostUuid().equals(host.getUUID())) {
                    ServerPlayer cooperator = host.server.getPlayerList().getPlayer(session.cooperatorUuid());
                    if (cooperator != null) {
                        ensureCooperatorPass(cooperator, gate, phase);
                        FogGateSyncService.syncNow(cooperator);
                    }
                }
            });
        }
        FogGateSyncService.syncNow(host);
    }

    public static boolean hasPass(UUID player, UUID gate) {
        Traversal traversal = TRAVERSALS.get(player);
        CooperatorPass pass = COOPERATOR_PASSES.get(player);
        return traversal != null && traversal.gateId.equals(gate) || pass != null && pass.gateId.equals(gate);
    }
    public static boolean isWaitingCooperator(UUID player, UUID gate) {
        CooperatorPass pass = COOPERATOR_PASSES.get(player);
        return pass != null && pass.gateId.equals(gate);
    }
    public static void ensureCooperatorPass(ServerPlayer player, FogGateDefinition gate, PhaseId phase) {
        COOPERATOR_PASSES.computeIfAbsent(player.getUUID(), ignored -> new CooperatorPass(gate.gateId(), phase,
                gate.bossEncounterId(), gate.signedDistanceToInside(player.getBoundingBox().getCenter())));
    }
    public static boolean hasActiveTraversal(PhaseId phase) {
        return TRAVERSALS.values().stream().anyMatch(traversal -> traversal.phase.equals(phase));
    }
    public static String debugState(UUID player, UUID gate) {
        Traversal traversal = TRAVERSALS.get(player);
        if (traversal != null && traversal.gateId.equals(gate)) return "HOST_TRAVERSAL";
        CooperatorPass pass = COOPERATOR_PASSES.get(player);
        return pass != null && pass.gateId.equals(gate) ? "COOPERATOR_PASS" : "NONE";
    }
    public static void clearPhase(MinecraftServer server, PhaseId phase) {
        TRAVERSALS.entrySet().removeIf(entry -> entry.getValue().phase.equals(phase));
        COOPERATOR_PASSES.entrySet().removeIf(entry -> entry.getValue().phase.equals(phase));
    }
    public static void forget(UUID player) { TRAVERSALS.remove(player); COOPERATOR_PASSES.remove(player); }
    public static void clear() { TRAVERSALS.clear(); COOPERATOR_PASSES.clear(); }
    public static void setInvasionCoordinator(BossGateEntryCoordinator coordinator) {
        invasionCoordinator = coordinator == null ? BossGateEntryCoordinator.NONE : coordinator;
    }

    private static FogGateDefinition find(MinecraftServer server, UUID gateId) {
        return EncounterSavedData.get(server).fogGates().stream().filter(gate -> gate.gateId().equals(gateId)).findFirst().orElse(null);
    }
    private static BlockPos nearestGateBlock(ServerPlayer player, FogGateDefinition gate) {
        BlockPos best = null; double distance = Double.MAX_VALUE;
        for (long packed : gate.fogBlocks()) {
            BlockPos pos = BlockPos.of(packed);
            double next = player.distanceToSqr(pos.getCenter());
            if (next < distance) { distance = next; best = pos; }
        }
        return best;
    }
    private static boolean withinRange(ServerPlayer player, BlockPos pos) {
        AABB playerBox = player.getBoundingBox();
        AABB block = new AABB(pos);
        double dx = Math.max(0, Math.max(block.minX - playerBox.maxX, playerBox.minX - block.maxX));
        double dy = Math.max(0, Math.max(block.minY - playerBox.maxY, playerBox.minY - block.maxY));
        double dz = Math.max(0, Math.max(block.minZ - playerBox.maxZ, playerBox.minZ - block.maxZ));
        double max = Math.min(1.25D, player.blockInteractionRange());
        return dx * dx + dy * dy + dz * dz <= max * max;
    }
    private static boolean lineOfSight(ServerPlayer player, FogGateDefinition gate, BlockPos pos) {
        Vec3 end = pos.getCenter();
        BlockHitResult hit = player.level().clip(new ClipContext(player.getEyePosition(), end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.MISS) return true;
        long hitPos = hit.getBlockPos().asLong();
        for (long block : gate.fogBlocks()) if (block == hitPos) return true;
        return false;
    }

    private static final class Traversal {
        private final UUID gateId;
        private final PhaseId phase;
        private final long started;
        private final long deadline;
        private double lastSignedDistance;
        private Traversal(UUID gateId, PhaseId phase, long started, long deadline, double lastSignedDistance) {
            this.gateId = gateId; this.phase = phase; this.started = started; this.deadline = deadline;
            this.lastSignedDistance = lastSignedDistance;
        }
        UUID gateId() { return gateId; }
        PhaseId phase() { return phase; }
        long deadline() { return deadline; }
    }
    private static final class CooperatorPass {
        private final UUID gateId;
        private final PhaseId phase;
        private final net.minecraft.resources.ResourceLocation encounterId;
        private double lastSignedDistance;
        private CooperatorPass(UUID gateId, PhaseId phase, net.minecraft.resources.ResourceLocation encounterId,
                               double lastSignedDistance) {
            this.gateId = gateId; this.phase = phase; this.encounterId = encounterId;
            this.lastSignedDistance = lastSignedDistance;
        }
    }
    private FogTraversalManager() {}
}
