package dev.maplesadventure.multiplayer.encounter.fog;

import dev.maplesadventure.multiplayer.encounter.EncounterDefinition;
import dev.maplesadventure.multiplayer.encounter.EncounterSavedData;
import dev.maplesadventure.multiplayer.encounter.EncounterSpawnRole;
import dev.maplesadventure.multiplayer.encounter.EncounterStatus;
import dev.maplesadventure.multiplayer.encounter.EncounterType;
import dev.maplesadventure.multiplayer.phase.PhaseManager;
import dev.maplesadventure.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

/** Short, server-derived map-author diagnostics; never shown to ordinary players. */
public final class FogGateDiagnostics {
    public static Result evaluate(ServerPlayer player, BlockPos looked) {
        EncounterSavedData data = EncounterSavedData.get(player.server);
        if (!player.level().getBlockState(looked).is(ModBlocks.BOSS_FOG_GATE.get())) return new Result(Reason.NOT_A_GATE, null);
        FogGateDefinition gate = data.fogGate(player.level().dimension(), looked).orElse(null);
        if (gate == null || !gate.isIntact(player.serverLevel())) return new Result(Reason.UNBOUND, gate);
        EncounterDefinition encounter = data.definition(gate.bossEncounterId()).orElse(null);
        if (!validPrimarySide(encounter, gate)) return new Result(Reason.INVALID_PRIMARY_SIDE, gate);
        if (data.state(PhaseManager.state(player).phaseId(), encounter).status() != EncounterStatus.READY)
            return new Result(Reason.ENCOUNTER_NOT_READY, gate);
        if (gate.side(player.getBoundingBox().getCenter()) != FogGateDefinition.GateSide.OUTSIDE)
            return new Result(Reason.WRONG_SIDE, gate);
        if (!withinRange(player, gate)) return new Result(Reason.TOO_FAR, gate);
        return new Result(Reason.READY, gate);
    }

    public static boolean validPrimarySide(EncounterDefinition encounter, FogGateDefinition gate) {
        if (encounter == null || encounter.type() != EncounterType.BOSS || encounter.primaryCount() != 1L
                || !encounter.dimension().equals(gate.dimension())) return false;
        var primary = encounter.spawnPoints().stream()
                .filter(point -> point.role() == EncounterSpawnRole.BOSS_PRIMARY).findFirst().orElse(null);
        if (primary == null) return false;
        double axis = gate.facing().getAxis() == Direction.Axis.X ? primary.position().x : primary.position().z;
        double difference = axis - gate.planeCoordinate();
        if (Math.abs(difference) < 0.25D) return false;
        Direction expected = gate.facing().getAxis() == Direction.Axis.X
                ? (difference > 0.0D ? Direction.EAST : Direction.WEST)
                : (difference > 0.0D ? Direction.SOUTH : Direction.NORTH);
        return expected == gate.insideDirection();
    }

    private static boolean withinRange(ServerPlayer player, FogGateDefinition gate) {
        double maximum = Math.min(1.25D, player.blockInteractionRange());
        double maximumSquared = maximum * maximum;
        for (long packed : gate.fogBlocks()) {
            AABB target = new AABB(BlockPos.of(packed));
            AABB source = player.getBoundingBox();
            double dx = Math.max(0.0D, Math.max(target.minX - source.maxX, source.minX - target.maxX));
            double dy = Math.max(0.0D, Math.max(target.minY - source.maxY, source.minY - target.maxY));
            double dz = Math.max(0.0D, Math.max(target.minZ - source.maxZ, source.minZ - target.maxZ));
            if (dx * dx + dy * dy + dz * dz <= maximumSquared) return true;
        }
        return false;
    }

    public enum Reason { READY, UNBOUND, INVALID_PRIMARY_SIDE, ENCOUNTER_NOT_READY, WRONG_SIDE, TOO_FAR, NOT_A_GATE }
    public record Result(Reason reason, FogGateDefinition gate) {}
    private FogGateDiagnostics() {}
}
