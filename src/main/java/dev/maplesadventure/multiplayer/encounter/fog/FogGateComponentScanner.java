package dev.maplesadventure.multiplayer.encounter.fog;

import dev.maplesadventure.multiplayer.encounter.EncounterDefinition;
import dev.maplesadventure.multiplayer.encounter.EncounterSpawnPoint;
import dev.maplesadventure.multiplayer.encounter.EncounterSpawnRole;
import dev.maplesadventure.multiplayer.encounter.EncounterType;
import dev.maplesadventure.registry.ModBlocks;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayDeque;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** Discovers one face-connected, coplanar fog component and derives inside from its primary spawn. */
public final class FogGateComponentScanner {
    public static Result scan(ServerLevel level, EncounterDefinition encounter, BlockPos selectedGate) {
        if (encounter.type() != EncounterType.BOSS || !encounter.dimension().equals(level.dimension()))
            return Result.failure("Encounter must be a BOSS in this dimension");
        BlockState selected = level.getBlockState(selectedGate);
        if (!selected.is(ModBlocks.BOSS_FOG_GATE.get())) return Result.failure("Look at a boss_fog_gate block");
        Direction facing = selected.getValue(BossFogGateBlock.FACING);
        LongOpenHashSet gate = collectGate(level, selectedGate, facing);
        if (gate.isEmpty()) return Result.failure("Fog gate component is empty");

        EncounterSpawnPoint primary = encounter.spawnPoints().stream()
                .filter(point -> point.role() == EncounterSpawnRole.BOSS_PRIMARY).findFirst().orElse(null);
        if (primary == null || encounter.primaryCount() != 1L)
            return Result.failure("Boss must have exactly one BOSS_PRIMARY");
        double plane = facing.getAxis() == Direction.Axis.X ? selectedGate.getX() + 0.5D : selectedGate.getZ() + 0.5D;
        double primaryAxis = facing.getAxis() == Direction.Axis.X ? primary.position().x : primary.position().z;
        if (Math.abs(primaryAxis - plane) < 0.25D)
            return Result.failure("Boss primary spawn must be clearly on one side of the fog gate.");
        Direction inside = facing.getAxis() == Direction.Axis.X
                ? (primaryAxis > plane ? Direction.EAST : Direction.WEST)
                : (primaryAxis > plane ? Direction.SOUTH : Direction.NORTH);
        return new Result(true, "OK", new FogGateDefinition(UUID.randomUUID(), encounter.encounterId(),
                level.dimension(), gate.toLongArray(), facing, inside, BossRoomVolume.empty()));
    }

    private static LongOpenHashSet collectGate(ServerLevel level, BlockPos start, Direction facing) {
        LongOpenHashSet result = new LongOpenHashSet();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        int plane = facing.getAxis() == Direction.Axis.X ? start.getX() : start.getZ();
        queue.add(start);
        result.add(start.asLong());
        while (!queue.isEmpty() && result.size() <= 4096) {
            BlockPos current = queue.removeFirst();
            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);
                if ((facing.getAxis() == Direction.Axis.X ? next.getX() : next.getZ()) != plane) continue;
                BlockState state = level.getBlockState(next);
                if (!state.is(ModBlocks.BOSS_FOG_GATE.get()) || state.getValue(BossFogGateBlock.FACING) != facing) continue;
                if (result.add(next.asLong())) queue.addLast(next);
            }
        }
        return result;
    }

    public record Result(boolean valid, String reason, FogGateDefinition definition) {
        static Result failure(String reason) { return new Result(false, reason, null); }
    }
    private FogGateComponentScanner() {}
}
