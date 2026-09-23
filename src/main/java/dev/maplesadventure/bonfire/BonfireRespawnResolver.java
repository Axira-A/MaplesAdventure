package dev.maplesadventure.bonfire;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.portal.DimensionTransition;

/** Validates the exact placement generation before replacing Vanilla's respawn transition. */
public final class BonfireRespawnResolver {
    public static Optional<DimensionTransition> resolve(MinecraftServer server, ServerPlayer player, BonfireRestPoint point) {
        BonfireRef ref = point.ref();
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, ref.dimension()));
        if (level == null || !level.getWorldBorder().isWithinBounds(ref.pos())) return Optional.empty();
        // A single short destination-chunk access is allowed for respawn. No persistent ticket/force-load is created.
        level.getChunkAt(ref.pos());
        if (!BonfireStateService.matches(level, ref)) return Optional.empty();
        for (int radius = 1; radius <= 3; radius++) {
            for (int dy : new int[]{0, 1, -1, 2, -2}) {
                for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    BlockPos feet = ref.pos().offset(dx, dy, dz);
                    if (!level.getWorldBorder().isWithinBounds(feet) || !safe(level, player, feet)) continue;
                    Vec3 destination = Vec3.atBottomCenterOf(feet);
                    return Optional.of(new DimensionTransition(level, destination, Vec3.ZERO,
                            point.yaw(), 0.0F, DimensionTransition.DO_NOTHING));
                }
            }
        }
        return Optional.empty();
    }
    private static boolean safe(ServerLevel level, ServerPlayer player, BlockPos feet) {
        BlockPos floor = feet.below();
        return level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
                && level.getFluidState(feet).isEmpty() && level.getFluidState(feet.above()).isEmpty()
                && level.noCollision(player.getDimensions(Pose.STANDING).makeBoundingBox(Vec3.atBottomCenterOf(feet)));
    }
    private BonfireRespawnResolver() {}
}
