package dev.maplesadventure.message;

import dev.maplesadventure.config.MessageConfig;
import dev.maplesadventure.interaction.InteractionGeometry;
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
import net.minecraft.world.phys.shapes.CollisionContext;

/** Server-side placement and reading rules. Client-selected coordinates are never trusted. */
public final class MessageValidator {
    private static final double RUNE_HALF_WIDTH = 0.42D;
    private static final double RUNE_HEIGHT = 0.08D;
    private static final double EPSILON = 1.0E-6D;

    public static PlacementResult resolvePlacement(ServerPlayer player) {
        if (!MessageConfig.ENABLED.get() || !player.isAlive() || player.isSpectator()) {
            return PlacementResult.failure(Failure.DISABLED);
        }
        ServerLevel level = player.serverLevel();
        double reach = player.blockInteractionRange();
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(reach));
        BlockHitResult hit = level.clip(new ClipContext(
                eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player
        ));
        if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != Direction.UP) {
            return PlacementResult.failure(Failure.INVALID_SURFACE);
        }
        BlockPos support = hit.getBlockPos();
        if (!level.isLoaded(support) || !level.getWorldBorder().isWithinBounds(support)) {
            return PlacementResult.failure(Failure.UNLOADED);
        }
        if (!player.canInteractWithBlock(support, 0.0D)) {
            return PlacementResult.failure(Failure.OUT_OF_RANGE);
        }
        if (!level.mayInteract(player, support)) {
            return PlacementResult.failure(Failure.NOT_ALLOWED);
        }
        BlockState state = level.getBlockState(support);
        if (state.isAir() || state.getShape(level, support, CollisionContext.of(player)).isEmpty()) {
            return PlacementResult.failure(Failure.INVALID_SURFACE);
        }
        Vec3 position = hit.getLocation().add(0.0D, 0.012D, 0.0D);
        return new PlacementResult(true, Failure.NONE, position, support, hit);
    }

    public static ReadResult canRead(ServerPlayer player, AdventureMessage message) {
        if (!MessageConfig.ENABLED.get() || !player.isAlive() || player.isSpectator()) {
            return ReadResult.failure(Failure.DISABLED);
        }
        ServerLevel level = player.serverLevel();
        if (!level.dimension().equals(message.dimension())) {
            return ReadResult.failure(Failure.WRONG_DIMENSION);
        }
        if (!level.isLoaded(message.supportPos())) {
            return ReadResult.failure(Failure.UNLOADED);
        }
        if (!supportIsValid(level, message)) {
            return ReadResult.failure(Failure.INVALID_SURFACE);
        }

        AABB rune = interactionBounds(message.position());
        AABB playerBounds = player.getBoundingBox();
        double distance = InteractionGeometry.distanceBetweenBoxes(
                playerBounds.minX, playerBounds.minY, playerBounds.minZ,
                playerBounds.maxX, playerBounds.maxY, playerBounds.maxZ,
                rune.minX, rune.minY, rune.minZ, rune.maxX, rune.maxY, rune.maxZ
        );
        double configured = MessageConfig.READ_DISTANCE.get();
        if (distance > configured + EPSILON) {
            return new ReadResult(false, Failure.OUT_OF_RANGE, distance, configured, null);
        }
        double vanillaReach = player.blockInteractionRange();
        if (distance > vanillaReach + EPSILON || !player.canInteractWithBlock(message.supportPos(), 0.0D)) {
            return new ReadResult(false, Failure.OUT_OF_VANILLA_RANGE, distance, configured, null);
        }

        Vec3 target = message.position().add(0.0D, 0.025D, 0.0D);
        Vec3 eye = player.getEyePosition();
        BlockHitResult firstHit = level.clip(new ClipContext(
                eye, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player
        ));
        boolean visible = firstHit.getType() == HitResult.Type.MISS;
        return new ReadResult(visible, visible ? Failure.NONE : Failure.BLOCKED,
                distance, configured, firstHit);
    }

    public static boolean supportIsValid(ServerLevel level, AdventureMessage message) {
        if (!level.isLoaded(message.supportPos())) {
            return true;
        }
        BlockState state = level.getBlockState(message.supportPos());
        return !state.isAir() && !state.getShape(level, message.supportPos()).isEmpty();
    }

    public static AABB interactionBounds(Vec3 position) {
        return new AABB(
                position.x - RUNE_HALF_WIDTH, position.y - 0.02D, position.z - RUNE_HALF_WIDTH,
                position.x + RUNE_HALF_WIDTH, position.y + RUNE_HEIGHT, position.z + RUNE_HALF_WIDTH
        );
    }

    public enum Failure {
        NONE,
        DISABLED,
        INVALID_TEMPLATE,
        COOLDOWN,
        TOO_CLOSE_TO_ANOTHER,
        INVALID_SURFACE,
        OUT_OF_RANGE,
        OUT_OF_VANILLA_RANGE,
        BLOCKED,
        WRONG_DIMENSION,
        UNLOADED,
        NOT_FOUND,
        NOT_ALLOWED
    }

    public record PlacementResult(
            boolean valid,
            Failure failure,
            Vec3 position,
            BlockPos supportPos,
            BlockHitResult hitResult
    ) {
        static PlacementResult failure(Failure failure) {
            return new PlacementResult(false, failure, Vec3.ZERO, BlockPos.ZERO, null);
        }
    }

    public record ReadResult(
            boolean valid,
            Failure failure,
            double distance,
            double maximum,
            BlockHitResult firstHit
    ) {
        static ReadResult failure(Failure failure) {
            return new ReadResult(false, failure, Double.NaN, MessageConfig.READ_DISTANCE.get(), null);
        }
    }

    private MessageValidator() {
    }
}
