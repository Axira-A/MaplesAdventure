package dev.maplesadventure.multiplayer.invasion;

import dev.maplesadventure.multiplayer.encounter.EncounterSavedData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Loaded-chunk-only safe spawn search. It never asks the chunk source to load terrain. */
public final class InvasionSpawnResolver {
    private static final int MIN_RADIUS = 12;
    private static final int MAX_RADIUS = 32;

    public static Vec3 resolve(ServerPlayer host, ServerPlayer invader) {
        ServerLevel level = host.serverLevel();
        Random random = new Random(host.getUUID().getLeastSignificantBits() ^ level.getGameTime());
        List<Candidate> candidates = new ArrayList<>();
        for (int radius = MIN_RADIUS; radius <= MAX_RADIUS; radius += 2) {
            double offset = random.nextDouble() * Math.PI * 2.0D;
            for (int sample = 0; sample < 12; sample++) {
                double angle = offset + sample * Math.PI * 2.0D / 12.0D;
                int x = net.minecraft.util.Mth.floor(host.getX() + Math.cos(angle) * radius);
                int z = net.minecraft.util.Mth.floor(host.getZ() + Math.sin(angle) * radius);
                for (int dy = 5; dy >= -5; dy--) {
                    BlockPos feet = new BlockPos(x, net.minecraft.util.Mth.floor(host.getY()) + dy, z);
                    Vec3 position = safePosition(level, invader, feet);
                    if (position == null || nearFogGate(level, position)) continue;
                    boolean visible = lineOfSight(host, position);
                    double distance = position.distanceToSqr(host.position());
                    candidates.add(new Candidate(position, visible, Math.abs(distance - 24.0D * 24.0D)));
                    break;
                }
            }
        }
        return candidates.stream().min(Comparator.comparing(Candidate::visible)
                .thenComparingDouble(Candidate::distancePenalty)).map(Candidate::position).orElse(null);
    }

    private static Vec3 safePosition(ServerLevel level, ServerPlayer player, BlockPos feet) {
        BlockPos support = feet.below();
        if (!level.isLoaded(feet) || !level.isLoaded(support)
                || !level.getWorldBorder().isWithinBounds(feet)
                || !level.getBlockState(support).isFaceSturdy(level, support, Direction.UP)
                || !level.getFluidState(feet).isEmpty()
                || level.getFluidState(support).is(FluidTags.LAVA)) return null;
        Vec3 position = Vec3.atBottomCenterOf(feet);
        AABB box = player.getDimensions(player.getPose()).makeBoundingBox(position);
        return level.noCollision(player, box) ? position : null;
    }

    private static boolean lineOfSight(ServerPlayer host, Vec3 position) {
        return host.level().clip(new ClipContext(host.getEyePosition(), position.add(0.0D, 1.0D, 0.0D),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, host)).getType() == HitResult.Type.MISS;
    }

    private static boolean nearFogGate(ServerLevel level, Vec3 position) {
        return EncounterSavedData.get(level.getServer()).fogGates().stream()
                .filter(gate -> gate.dimension().equals(level.dimension()))
                .anyMatch(gate -> java.util.Arrays.stream(gate.fogBlocks())
                        .mapToObj(BlockPos::of).anyMatch(pos -> pos.getCenter().distanceToSqr(position) < 36.0D));
    }

    private record Candidate(Vec3 position, boolean visible, double distancePenalty) {}
    private InvasionSpawnResolver() {}
}
