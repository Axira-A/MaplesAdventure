package dev.maplesadventure.interaction;

import dev.maplesadventure.config.InteractionConfig;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Shared geometry and range policy for scanning, retention, and execution. */
public final class InteractionDistanceCalculator {
    private static final double EPSILON = 1.0E-6D;
    public static final double RELEASE_HYSTERESIS = 0.05D;

    public static DistanceResult forBlock(
            ClientLevel level,
            LocalPlayer player,
            BlockInteractionTarget target,
            boolean releaseHysteresis
    ) {
        TargetShape targetShape = resolveTargetShape(level, player, target.pos());
        AABB playerBounds = player.getBoundingBox();
        List<AABB> boxes = targetShape.shape().toAabbs();
        double distance = Double.POSITIVE_INFINITY;
        for (int index = 0; index < boxes.size(); index++) {
            double candidate = distanceBetween(playerBounds, boxes.get(index).move(target.pos()));
            if (candidate < distance) {
                distance = candidate;
            }
        }
        return result(
                distance,
                InteractionConfig.MAX_INTERACTION_DISTANCE.get(),
                player.blockInteractionRange(),
                releaseHysteresis,
                player.canInteractWithBlock(target.pos(), 0.0D),
                targetShape.source()
        );
    }

    public static DistanceResult forEntity(
            LocalPlayer player,
            Entity entity,
            boolean releaseHysteresis
    ) {
        return forEntity(player, entity, InteractionConfig.MAX_INTERACTION_DISTANCE.get(), releaseHysteresis);
    }

    public static DistanceResult forEntity(
            LocalPlayer player,
            Entity entity,
            double configuredMaximum,
            boolean releaseHysteresis
    ) {
        double distance = distanceBetween(player.getBoundingBox(), entity.getBoundingBox());
        return result(
                distance,
                Math.min(InteractionConfig.MAX_INTERACTION_DISTANCE.get(), configuredMaximum),
                player.entityInteractionRange(),
                releaseHysteresis,
                player.canInteractWithEntity(entity, 0.0D),
                ShapeSource.ENTITY_BOUNDS
        );
    }

    public static double blockBroadPhaseRange(LocalPlayer player) {
        double margin = InteractionConfig.CANDIDATE_TOLERANCE.get();
        return Math.min(InteractionConfig.MAX_INTERACTION_DISTANCE.get(), player.blockInteractionRange()) + margin;
    }

    public static double entityBroadPhaseRange(LocalPlayer player) {
        double margin = InteractionConfig.CANDIDATE_TOLERANCE.get();
        return Math.min(InteractionConfig.MAX_INTERACTION_DISTANCE.get(), player.entityInteractionRange()) + margin;
    }

    public static DistanceResult forVirtual(LocalPlayer player, Vec3 position, boolean releaseHysteresis) {
        AABB bounds = dev.maplesadventure.message.MessageValidator.interactionBounds(position);
        double distance = distanceBetween(player.getBoundingBox(), bounds);
        double vanillaReach = player.blockInteractionRange();
        return result(
                distance,
                InteractionConfig.MAX_INTERACTION_DISTANCE.get(),
                vanillaReach,
                releaseHysteresis,
                distance <= vanillaReach + EPSILON,
                ShapeSource.VIRTUAL_BOUNDS
        );
    }

    public static TargetShape resolveTargetShape(ClientLevel level, LocalPlayer player, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        CollisionContext context = CollisionContext.of(player);
        VoxelShape shape = state.getShape(level, pos, context);
        if (!shape.isEmpty()) {
            return new TargetShape(shape, ShapeSource.OUTLINE);
        }
        shape = state.getInteractionShape(level, pos);
        if (!shape.isEmpty()) {
            return new TargetShape(shape, ShapeSource.INTERACTION);
        }
        shape = state.getCollisionShape(level, pos, context);
        if (!shape.isEmpty()) {
            return new TargetShape(shape, ShapeSource.COLLISION);
        }
        return new TargetShape(Shapes.block(), ShapeSource.FULL_BLOCK_FALLBACK);
    }

    static double distanceBetween(AABB first, AABB second) {
        return distanceBetween(
                first.minX, first.minY, first.minZ, first.maxX, first.maxY, first.maxZ,
                second.minX, second.minY, second.minZ, second.maxX, second.maxY, second.maxZ
        );
    }

    static double distanceBetween(
            double firstMinX, double firstMinY, double firstMinZ,
            double firstMaxX, double firstMaxY, double firstMaxZ,
            double secondMinX, double secondMinY, double secondMinZ,
            double secondMaxX, double secondMaxY, double secondMaxZ
    ) {
        return InteractionGeometry.distanceBetweenBoxes(
                firstMinX, firstMinY, firstMinZ, firstMaxX, firstMaxY, firstMaxZ,
                secondMinX, secondMinY, secondMinZ, secondMaxX, secondMaxY, secondMaxZ
        );
    }

    private static DistanceResult result(
            double distance,
            double configuredMaximum,
            double vanillaReach,
            boolean releaseHysteresis,
            boolean vanillaReachCheck,
            ShapeSource shapeSource
    ) {
        double maplesLimit = configuredMaximum + (releaseHysteresis ? RELEASE_HYSTERESIS : 0.0D);
        double effectiveMaximum = Math.min(maplesLimit, vanillaReach);
        boolean withinMaplesRange = distance <= maplesLimit + EPSILON;
        boolean withinVanillaRange = distance <= vanillaReach + EPSILON && vanillaReachCheck;
        return new DistanceResult(
                distance,
                configuredMaximum,
                effectiveMaximum,
                vanillaReach,
                withinMaplesRange,
                withinVanillaRange,
                shapeSource
        );
    }

    public record DistanceResult(
            double preciseDistance,
            double configuredMaximum,
            double effectiveMaximum,
            double vanillaReach,
            boolean withinMaplesRange,
            boolean withinVanillaRange,
            ShapeSource shapeSource
    ) {
    }

    public record TargetShape(VoxelShape shape, ShapeSource source) {
    }

    public enum ShapeSource {
        OUTLINE,
        INTERACTION,
        COLLISION,
        FULL_BLOCK_FALLBACK,
        ENTITY_BOUNDS,
        VIRTUAL_BOUNDS
    }

    private InteractionDistanceCalculator() {
    }
}
