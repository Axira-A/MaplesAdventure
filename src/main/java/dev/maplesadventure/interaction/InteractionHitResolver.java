package dev.maplesadventure.interaction;

import java.util.Collections;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Resolves an actual target-surface hit and proves that surface is visible. */
public final class InteractionHitResolver {
    private static final int MAX_SHAPE_BOXES = 16;
    private static final double INTERIOR_EPSILON = 1.0E-4D;

    public static BlockHitResolution resolveBlockHit(ClientLevel level, LocalPlayer player, BlockPos pos) {
        VoxelShape shape = InteractionDistanceCalculator.resolveTargetShape(level, player, pos).shape();
        List<AABB> boxes = shape.toAabbs();
        BlockHitResult firstObstruction = null;
        int boxCount = Math.min(boxes.size(), MAX_SHAPE_BOXES);
        prioritizeNearestBoxes(boxes, player.getBoundingBox(), pos, boxCount);
        for (int index = 0; index < boxCount; index++) {
            AABB box = boxes.get(index);
            double centerX = midpoint(box.minX, box.maxX);
            double centerY = midpoint(box.minY, box.maxY);
            double centerZ = midpoint(box.minZ, box.maxZ);
            double minX = insetMinimum(box.minX, box.maxX);
            double maxX = insetMaximum(box.minX, box.maxX);
            double minY = insetMinimum(box.minY, box.maxY);
            double maxY = insetMaximum(box.minY, box.maxY);
            double minZ = insetMinimum(box.minZ, box.maxZ);
            double maxZ = insetMaximum(box.minZ, box.maxZ);

            BlockHitResolution resolution = tryPoint(level, player, pos, shape, centerX, centerY, centerZ);
            if (resolution.valid()) {
                return resolution;
            }
            firstObstruction = firstObstruction(firstObstruction, resolution.raycastFirstHit());

            for (int pointIndex = 0; pointIndex < 6; pointIndex++) {
                double pointX = centerX;
                double pointY = centerY;
                double pointZ = centerZ;
                switch (pointIndex) {
                    case 0 -> pointX = minX;
                    case 1 -> pointX = maxX;
                    case 2 -> pointY = minY;
                    case 3 -> pointY = maxY;
                    case 4 -> pointZ = minZ;
                    case 5 -> pointZ = maxZ;
                    default -> throw new IllegalStateException("Unexpected surface point index " + pointIndex);
                }
                resolution = tryPoint(level, player, pos, shape, pointX, pointY, pointZ);
                if (resolution.valid()) {
                    return resolution;
                }
                firstObstruction = firstObstruction(firstObstruction, resolution.raycastFirstHit());
            }
        }
        return new BlockHitResolution(
                null,
                firstObstruction,
                firstObstruction == null ? ResolutionFailure.NO_VALID_HIT_POINT : ResolutionFailure.BLOCKED_BY_OTHER_BLOCK
        );
    }

    public static EntityHitResolution resolveEntityHit(ClientLevel level, LocalPlayer player, Entity entity) {
        AABB bounds = entity.getBoundingBox();
        Vec3 eye = player.getEyePosition();
        double x = clamp(eye.x, bounds.minX, bounds.maxX);
        double z = clamp(eye.z, bounds.minZ, bounds.maxZ);
        double[] heights = {0.72D, 0.50D, 0.90D, 0.25D};
        BlockHitResult firstObstruction = null;
        for (int index = 0; index < heights.length; index++) {
            double y = bounds.minY + (bounds.maxY - bounds.minY) * heights[index];
            Vec3 point = new Vec3(x, y, z);
            InteractionVisibilityPolicy.TraceResult trace = InteractionVisibilityPolicy.traceEntity(level, player, point);
            if (trace.visible()) {
                return new EntityHitResolution(new EntityHitResult(entity, point), trace.firstHit(), ResolutionFailure.NONE);
            }
            firstObstruction = firstObstruction(firstObstruction, trace.firstHit());
        }
        return new EntityHitResolution(null, firstObstruction, ResolutionFailure.BLOCKED_BY_OTHER_BLOCK);
    }

    private static BlockHitResolution tryPoint(
            ClientLevel level,
            LocalPlayer player,
            BlockPos pos,
            VoxelShape shape,
            double localX,
            double localY,
            double localZ
    ) {
        Vec3 insidePoint = new Vec3(pos.getX() + localX, pos.getY() + localY, pos.getZ() + localZ);
        BlockHitResult targetHit = shape.clip(player.getEyePosition(), insidePoint, pos);
        if (targetHit == null) {
            return new BlockHitResolution(null, null, ResolutionFailure.NO_VALID_HIT_POINT);
        }
        InteractionVisibilityPolicy.TraceResult trace = InteractionVisibilityPolicy.traceBlock(
                level,
                player,
                pos,
                targetHit.getLocation()
        );
        if (!trace.visible()) {
            return new BlockHitResolution(null, trace.firstHit(), ResolutionFailure.BLOCKED_BY_OTHER_BLOCK);
        }
        return new BlockHitResolution(targetHit, trace.firstHit(), ResolutionFailure.NONE);
    }

    private static @Nullable BlockHitResult firstObstruction(
            @Nullable BlockHitResult current,
            @Nullable BlockHitResult candidate
    ) {
        return current == null ? candidate : current;
    }

    private static double midpoint(double minimum, double maximum) {
        return (minimum + maximum) * 0.5D;
    }

    private static double insetMinimum(double minimum, double maximum) {
        return minimum + Math.min(INTERIOR_EPSILON, (maximum - minimum) * 0.25D);
    }

    private static double insetMaximum(double minimum, double maximum) {
        return maximum - Math.min(INTERIOR_EPSILON, (maximum - minimum) * 0.25D);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static void prioritizeNearestBoxes(
            List<AABB> boxes,
            AABB playerBounds,
            BlockPos pos,
            int selectedCount
    ) {
        for (int index = 0; index < selectedCount; index++) {
            int nearestIndex = index;
            double nearestDistance = InteractionDistanceCalculator.distanceBetween(
                    playerBounds,
                    boxes.get(index).move(pos)
            );
            for (int candidateIndex = index + 1; candidateIndex < boxes.size(); candidateIndex++) {
                double candidateDistance = InteractionDistanceCalculator.distanceBetween(
                        playerBounds,
                        boxes.get(candidateIndex).move(pos)
                );
                if (candidateDistance < nearestDistance) {
                    nearestDistance = candidateDistance;
                    nearestIndex = candidateIndex;
                }
            }
            if (nearestIndex != index) {
                Collections.swap(boxes, index, nearestIndex);
            }
        }
    }

    public record BlockHitResolution(
            @Nullable BlockHitResult hitResult,
            @Nullable BlockHitResult raycastFirstHit,
            ResolutionFailure failure
    ) {
        public boolean valid() {
            return hitResult != null && failure == ResolutionFailure.NONE;
        }
    }

    public record EntityHitResolution(
            @Nullable EntityHitResult hitResult,
            @Nullable BlockHitResult raycastFirstHit,
            ResolutionFailure failure
    ) {
        public boolean valid() {
            return hitResult != null && failure == ResolutionFailure.NONE;
        }
    }

    public enum ResolutionFailure {
        NONE,
        NO_VALID_HIT_POINT,
        BLOCKED_BY_OTHER_BLOCK
    }

    private InteractionHitResolver() {
    }
}
