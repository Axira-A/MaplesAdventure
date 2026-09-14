package dev.maplesadventure.interaction;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Uses Minecraft collision-shape clipping; it contains no transparency whitelist. */
public final class InteractionVisibilityPolicy {
    private static final double ENDPOINT_EXTENSION = 0.01D;

    public static TraceResult traceBlock(
            ClientLevel level,
            LocalPlayer player,
            BlockPos targetPos,
            Vec3 intendedHitPoint
    ) {
        BlockHitResult firstHit = clip(level, player, intendedHitPoint);
        boolean visible = firstHit.getType() == HitResult.Type.MISS || firstHit.getBlockPos().equals(targetPos);
        return new TraceResult(visible, firstHit);
    }

    public static TraceResult traceEntity(ClientLevel level, LocalPlayer player, Vec3 intendedHitPoint) {
        BlockHitResult firstHit = clip(level, player, intendedHitPoint);
        return new TraceResult(firstHit.getType() == HitResult.Type.MISS, firstHit);
    }

    /** A multi-block virtual target may legitimately hit any block belonging to the same logical object. */
    public static TraceResult traceBlockGroup(
            ClientLevel level,
            LocalPlayer player,
            long[] targetBlocks,
            Vec3 intendedHitPoint
    ) {
        BlockHitResult firstHit = clip(level, player, intendedHitPoint);
        if (firstHit.getType() == HitResult.Type.MISS) return new TraceResult(true, firstHit);
        long hit = firstHit.getBlockPos().asLong();
        for (long target : targetBlocks) {
            if (target == hit) return new TraceResult(true, firstHit);
        }
        return new TraceResult(false, firstHit);
    }

    public static TraceResult traceVirtual(
            ClientLevel level,
            LocalPlayer player,
            BlockPos supportPos,
            Vec3 intendedHitPoint
    ) {
        Vec3 eye = player.getEyePosition();
        BlockHitResult firstHit = level.clip(new ClipContext(
                eye,
                intendedHitPoint,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));
        // The rune is above the support surface, not part of it. Hitting the support means a floor/ceiling blocks the read.
        return new TraceResult(firstHit.getType() == HitResult.Type.MISS, firstHit);
    }

    private static BlockHitResult clip(ClientLevel level, LocalPlayer player, Vec3 intendedHitPoint) {
        Vec3 eye = player.getEyePosition();
        Vec3 direction = intendedHitPoint.subtract(eye);
        Vec3 end = direction.lengthSqr() < 1.0E-10D
                ? intendedHitPoint
                : intendedHitPoint.add(direction.normalize().scale(ENDPOINT_EXTENSION));
        return level.clip(new ClipContext(
                eye,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));
    }

    public record TraceResult(boolean visible, BlockHitResult firstHit) {
    }

    private InteractionVisibilityPolicy() {
    }
}
