package dev.maplesadventure.multiplayer.coop;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Server-side placement, distance, LOS, support and teleport-space validation. */
public final class SummonSignValidator {
    public static final double INTERACTION_DISTANCE = 1.25D;
    private static final List<Vec3> SAFE_OFFSETS = List.of(
            Vec3.ZERO,
            new Vec3(0.75D, 0.0D, 0.0D), new Vec3(-0.75D, 0.0D, 0.0D),
            new Vec3(0.0D, 0.0D, 0.75D), new Vec3(0.0D, 0.0D, -0.75D),
            new Vec3(0.75D, 0.0D, 0.75D), new Vec3(-0.75D, 0.0D, 0.75D),
            new Vec3(0.75D, 0.0D, -0.75D), new Vec3(-0.75D, 0.0D, -0.75D),
            new Vec3(0.0D, 1.0D, 0.0D)
    );

    public static PlacementResult resolvePlacement(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        double yaw = Math.toRadians(player.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        Vec3 feet = new Vec3(player.getX(), player.getBoundingBox().minY, player.getZ());
        for (Vec3 probe : List.of(feet.add(forward.scale(0.8D)), feet)) {
            BlockPos support = BlockPos.containing(probe.x, feet.y - 0.05D, probe.z);
            Vec3 position = resolveSurface(level, support, probe.x, probe.z);
            if (position == null || !level.getWorldBorder().isWithinBounds(support)) continue;
            if (pointDistance(player.getBoundingBox(), position) > INTERACTION_DISTANCE
                    || !player.canInteractWithBlock(support, 0.0D)) continue;
            if (!hasLineOfSight(level, player, position)) continue;
            return new PlacementResult(true, position, support);
        }
        return PlacementResult.invalid();
    }

    public static boolean validateForSummon(ServerPlayer host, SummonSignRecord sign) {
        if (sign.state() != SummonSignState.AVAILABLE
                || !host.level().dimension().equals(sign.dimension())
                || !host.serverLevel().isLoaded(sign.supportPos())
                || !supportIsValid(host.serverLevel(), sign.supportPos(), sign.position())) return false;
        double effectiveRange = Math.min(INTERACTION_DISTANCE, host.blockInteractionRange());
        return pointDistance(host.getBoundingBox(), sign.position()) <= effectiveRange
                && host.canInteractWithBlock(sign.supportPos(), 0.0D)
                && hasLineOfSight(host.serverLevel(), host, sign.position());
    }

    public static boolean supportIsValid(ServerLevel level, BlockPos support, Vec3 position) {
        Vec3 surface = resolveSurface(level, support, position.x, position.z);
        return surface != null && Math.abs(surface.y - position.y) <= 0.08D;
    }

    public static Vec3 findSafePlayerPosition(ServerLevel level, ServerPlayer player, Vec3 intended) {
        for (Vec3 offset : SAFE_OFFSETS) {
            Vec3 candidate = intended.add(offset);
            AABB box = player.getDimensions(player.getPose()).makeBoundingBox(candidate);
            BlockPos feet = BlockPos.containing(candidate);
            if (level.getWorldBorder().isWithinBounds(feet)
                    && level.getFluidState(feet).isEmpty()
                    && level.noCollision(player, box)) return candidate;
        }
        return null;
    }

    private static Vec3 resolveSurface(ServerLevel level, BlockPos support, double x, double z) {
        if (!level.isLoaded(support)) return null;
        BlockState state = level.getBlockState(support);
        if (state.isAir()) return null;
        VoxelShape shape = state.getCollisionShape(level, support);
        if (shape.isEmpty()) shape = state.getShape(level, support);
        if (shape.isEmpty()) return null;
        double top = shape.max(Direction.Axis.Y);
        double localX = Math.max(0.08D, Math.min(0.92D, x - support.getX()));
        double localZ = Math.max(0.08D, Math.min(0.92D, z - support.getZ()));
        return new Vec3(support.getX() + localX, support.getY() + top + 0.012D, support.getZ() + localZ);
    }

    private static boolean hasLineOfSight(ServerLevel level, ServerPlayer player, Vec3 position) {
        BlockHitResult hit = level.clip(new ClipContext(
                player.getEyePosition(), position.add(0.0D, 0.02D, 0.0D),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player
        ));
        return hit.getType() == HitResult.Type.MISS;
    }

    private static double pointDistance(AABB box, Vec3 point) {
        double dx = Math.max(Math.max(box.minX - point.x, 0.0D), point.x - box.maxX);
        double dy = Math.max(Math.max(box.minY - point.y, 0.0D), point.y - box.maxY);
        double dz = Math.max(Math.max(box.minZ - point.z, 0.0D), point.z - box.maxZ);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public record PlacementResult(boolean valid, Vec3 position, BlockPos supportPos) {
        static PlacementResult invalid() { return new PlacementResult(false, Vec3.ZERO, BlockPos.ZERO); }
    }

    private SummonSignValidator() {}
}
