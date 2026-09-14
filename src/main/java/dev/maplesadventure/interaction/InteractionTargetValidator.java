package dev.maplesadventure.interaction;

import dev.maplesadventure.interaction.provider.InteractionTargetProvider;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.Nullable;

/** One validation policy shared by acquisition, target retention, and F execution. */
public final class InteractionTargetValidator {
    private final InteractionRegistry registry;

    public InteractionTargetValidator(InteractionRegistry registry) {
        this.registry = registry;
    }

    public ValidationResult validate(
            ClientLevel level,
            LocalPlayer player,
            InteractionTarget target,
            @Nullable InteractionTargetProvider provider,
            ValidationMode mode
    ) {
        if (provider == null) {
            return ValidationResult.failure(FailureReason.NO_PROVIDER);
        }
        if (target instanceof BlockInteractionTarget blockTarget) {
            return validateBlock(level, player, blockTarget, provider, mode);
        }
        if (target instanceof EntityInteractionTarget entityTarget) {
            return validateEntity(level, player, entityTarget, provider, mode);
        }
        if (target instanceof MessageInteractionTarget messageTarget) {
            return validateMessage(level, player, messageTarget, provider, mode);
        }
        if (target instanceof SummonSignInteractionTarget signTarget) {
            return validateSummonSign(level, player, signTarget, provider, mode);
        }
        if (target instanceof FogGateInteractionTarget fogTarget) {
            return validateFogGate(level, player, fogTarget, provider, mode);
        }
        return ValidationResult.failure(FailureReason.NO_PROVIDER);
    }

    private ValidationResult validateFogGate(ClientLevel level, LocalPlayer player, FogGateInteractionTarget target,
                                               InteractionTargetProvider provider, ValidationMode mode) {
        var gate = dev.maplesadventure.multiplayer.encounter.fog.client.FogGateClientCache.get(target.gateId()).orElse(null);
        if (gate == null || !gate.interactable() || !providerValid(level, player, target, provider))
            return ValidationResult.failure(FailureReason.TARGET_REMOVED);
        net.minecraft.core.BlockPos closest = target.blockPos();
        if (closest == null || !level.isLoaded(closest)) return ValidationResult.failure(FailureReason.UNLOADED);
        InteractionDistanceCalculator.DistanceResult distance = InteractionDistanceCalculator.forVirtual(
                player, closest.getCenter(), mode == ValidationMode.RELEASE);
        if (!distance.withinMaplesRange()) return ValidationResult.failure(FailureReason.OUT_OF_MAPLES_RANGE, distance);
        if (!distance.withinVanillaRange() || !player.canInteractWithBlock(closest, 0.0D))
            return ValidationResult.failure(FailureReason.OUT_OF_VANILLA_RANGE, distance);
        // A 3x4 gate is one logical target. A ray aimed at the nearest cell may first hit an adjacent cell in
        // that same component, which is valid; only blocks outside this gate component are occluders.
        InteractionVisibilityPolicy.TraceResult trace = InteractionVisibilityPolicy.traceBlockGroup(
                level, player, gate.blocks(), closest.getCenter());
        if (!trace.visible()) return ValidationResult.failure(FailureReason.BLOCKED_BY_OTHER_BLOCK, distance, trace.firstHit());
        return ValidationResult.success(distance, null, null, trace.firstHit());
    }

    private ValidationResult validateSummonSign(
            ClientLevel level,
            LocalPlayer player,
            SummonSignInteractionTarget target,
            InteractionTargetProvider provider,
            ValidationMode mode
    ) {
        var summary = dev.maplesadventure.multiplayer.coop.client.SummonSignRenderCache.get(target.signId()).orElse(null);
        if (summary == null) return ValidationResult.failure(FailureReason.TARGET_REMOVED);
        if (!level.isLoaded(summary.supportPos())) return ValidationResult.failure(FailureReason.UNLOADED);
        if (level.getBlockState(summary.supportPos()).isAir()
                || level.getBlockState(summary.supportPos()).getShape(level, summary.supportPos()).isEmpty()) {
            return ValidationResult.failure(FailureReason.TARGET_CHANGED);
        }
        if (!providerValid(level, player, target, provider)) return ValidationResult.failure(FailureReason.TARGET_REMOVED);
        InteractionDistanceCalculator.DistanceResult distance = InteractionDistanceCalculator.forVirtual(
                player, summary.position(), mode == ValidationMode.RELEASE
        );
        if (!distance.withinMaplesRange()) return ValidationResult.failure(FailureReason.OUT_OF_MAPLES_RANGE, distance);
        if (!distance.withinVanillaRange() || !player.canInteractWithBlock(summary.supportPos(), 0.0D)) {
            return ValidationResult.failure(FailureReason.OUT_OF_VANILLA_RANGE, distance);
        }
        InteractionVisibilityPolicy.TraceResult trace = InteractionVisibilityPolicy.traceVirtual(
                level, player, summary.supportPos(), summary.position().add(0.0D, 0.025D, 0.0D)
        );
        if (!trace.visible()) return ValidationResult.failure(FailureReason.BLOCKED_BY_OTHER_BLOCK, distance, trace.firstHit());
        return ValidationResult.success(distance, null, null, trace.firstHit());
    }

    private ValidationResult validateMessage(
            ClientLevel level,
            LocalPlayer player,
            MessageInteractionTarget target,
            InteractionTargetProvider provider,
            ValidationMode mode
    ) {
        var summary = dev.maplesadventure.client.message.MessageRenderCache.get(target.messageId()).orElse(null);
        if (summary == null) return ValidationResult.failure(FailureReason.TARGET_REMOVED);
        if (!level.isLoaded(summary.supportPos())) return ValidationResult.failure(FailureReason.UNLOADED);
        if (level.getBlockState(summary.supportPos()).isAir()
                || level.getBlockState(summary.supportPos()).getShape(level, summary.supportPos()).isEmpty()) {
            return ValidationResult.failure(FailureReason.TARGET_CHANGED);
        }
        if (!providerValid(level, player, target, provider)) return ValidationResult.failure(FailureReason.TARGET_REMOVED);
        InteractionDistanceCalculator.DistanceResult distance = InteractionDistanceCalculator.forVirtual(
                player, summary.position(), mode == ValidationMode.RELEASE
        );
        if (!distance.withinMaplesRange()) return ValidationResult.failure(FailureReason.OUT_OF_MAPLES_RANGE, distance);
        if (!distance.withinVanillaRange() || !player.canInteractWithBlock(summary.supportPos(), 0.0D)) {
            return ValidationResult.failure(FailureReason.OUT_OF_VANILLA_RANGE, distance);
        }
        InteractionVisibilityPolicy.TraceResult trace = InteractionVisibilityPolicy.traceVirtual(
                level, player, summary.supportPos(), summary.position().add(0.0D, 0.025D, 0.0D)
        );
        if (!trace.visible()) return ValidationResult.failure(FailureReason.BLOCKED_BY_OTHER_BLOCK, distance, trace.firstHit());
        return ValidationResult.success(distance, null, null, trace.firstHit());
    }

    private ValidationResult validateBlock(
            ClientLevel level,
            LocalPlayer player,
            BlockInteractionTarget target,
            InteractionTargetProvider provider,
            ValidationMode mode
    ) {
        if (!level.isLoaded(target.pos())) {
            return ValidationResult.failure(FailureReason.UNLOADED);
        }
        if (level.getBlockState(target.pos()).getBlock() != target.expectedBlock()) {
            return ValidationResult.failure(FailureReason.TARGET_CHANGED);
        }
        if (!level.getWorldBorder().isWithinBounds(target.pos())) {
            return ValidationResult.failure(FailureReason.OUTSIDE_WORLD_BORDER);
        }
        if (!providerValid(level, player, target, provider)) {
            return ValidationResult.failure(FailureReason.TARGET_CHANGED);
        }

        InteractionDistanceCalculator.DistanceResult distance = InteractionDistanceCalculator.forBlock(
                level,
                player,
                target,
                mode == ValidationMode.RELEASE
        );
        if (!distance.withinMaplesRange()) {
            return ValidationResult.failure(FailureReason.OUT_OF_MAPLES_RANGE, distance);
        }
        if (!distance.withinVanillaRange()) {
            return ValidationResult.failure(FailureReason.OUT_OF_VANILLA_RANGE, distance);
        }

        try {
            InteractionHitResolver.BlockHitResolution resolution = provider.resolveHitResult(level, player, target);
            if (!resolution.valid()) {
                FailureReason reason = resolution.failure() == InteractionHitResolver.ResolutionFailure.NO_VALID_HIT_POINT
                        ? FailureReason.NO_VALID_HIT_POINT
                        : FailureReason.BLOCKED_BY_OTHER_BLOCK;
                return ValidationResult.failure(reason, distance, resolution.raycastFirstHit());
            }
            BlockHitResult hitResult = resolution.hitResult();
            if (hitResult == null || !hitResult.getBlockPos().equals(target.pos())) {
                return ValidationResult.failure(FailureReason.NO_VALID_HIT_POINT, distance);
            }
            InteractionVisibilityPolicy.TraceResult trace = InteractionVisibilityPolicy.traceBlock(
                    level,
                    player,
                    target.pos(),
                    hitResult.getLocation()
            );
            if (!trace.visible()) {
                return ValidationResult.failure(FailureReason.BLOCKED_BY_OTHER_BLOCK, distance, trace.firstHit());
            }
            return ValidationResult.success(distance, hitResult, null, trace.firstHit());
        } catch (RuntimeException exception) {
            registry.reportProviderError(provider, target, level, exception);
        } catch (LinkageError error) {
            registry.reportProviderError(provider, target, level, error);
        }
        return ValidationResult.failure(FailureReason.NO_VALID_HIT_POINT, distance);
    }

    private ValidationResult validateEntity(
            ClientLevel level,
            LocalPlayer player,
            EntityInteractionTarget target,
            InteractionTargetProvider provider,
            ValidationMode mode
    ) {
        Entity entity = target.resolve(level);
        if (entity == null || entity.isRemoved() || !entity.isAlive()) {
            return ValidationResult.failure(FailureReason.TARGET_REMOVED);
        }
        if (!level.getWorldBorder().isWithinBounds(entity.blockPosition())) {
            return ValidationResult.failure(FailureReason.OUTSIDE_WORLD_BORDER);
        }
        if (!providerValid(level, player, target, provider)) {
            return ValidationResult.failure(FailureReason.TARGET_REMOVED);
        }

        InteractionDistanceCalculator.DistanceResult distance = InteractionDistanceCalculator.forEntity(
                player,
                entity,
                provider.getMaximumInteractionDistance(level, player, target),
                mode == ValidationMode.RELEASE
        );
        if (!distance.withinMaplesRange()) {
            return ValidationResult.failure(FailureReason.OUT_OF_MAPLES_RANGE, distance);
        }
        if (!distance.withinVanillaRange()) {
            return ValidationResult.failure(FailureReason.OUT_OF_VANILLA_RANGE, distance);
        }

        InteractionHitResolver.EntityHitResolution resolution = InteractionHitResolver.resolveEntityHit(level, player, entity);
        if (!resolution.valid()) {
            return ValidationResult.failure(FailureReason.NO_LINE_OF_SIGHT, distance, resolution.raycastFirstHit());
        }
        return ValidationResult.success(distance, null, resolution.hitResult(), resolution.raycastFirstHit());
    }

    private boolean providerValid(
            ClientLevel level,
            LocalPlayer player,
            InteractionTarget target,
            InteractionTargetProvider provider
    ) {
        try {
            return provider.isValidTarget(level, player, target);
        } catch (RuntimeException exception) {
            registry.reportProviderError(provider, target, level, exception);
        } catch (LinkageError error) {
            registry.reportProviderError(provider, target, level, error);
        }
        return false;
    }

    public enum ValidationMode {
        ACQUIRE,
        RELEASE,
        EXECUTE
    }

    public enum FailureReason {
        NONE,
        OUT_OF_MAPLES_RANGE,
        OUT_OF_VANILLA_RANGE,
        NO_LINE_OF_SIGHT,
        BLOCKED_BY_OTHER_BLOCK,
        NO_VALID_HIT_POINT,
        TARGET_REMOVED,
        TARGET_CHANGED,
        NO_PROVIDER,
        UNLOADED,
        OUTSIDE_WORLD_BORDER
    }

    public record ValidationResult(
            boolean valid,
            FailureReason failureReason,
            double preciseDistance,
            double configuredMaximum,
            double effectiveMaximum,
            double vanillaReach,
            @Nullable InteractionDistanceCalculator.ShapeSource shapeSource,
            @Nullable BlockHitResult blockHitResult,
            @Nullable EntityHitResult entityHitResult,
            @Nullable BlockHitResult raycastFirstHit
    ) {
        static ValidationResult success(
                InteractionDistanceCalculator.DistanceResult distance,
                @Nullable BlockHitResult blockHitResult,
                @Nullable EntityHitResult entityHitResult,
                @Nullable BlockHitResult raycastFirstHit
        ) {
            return from(true, FailureReason.NONE, distance, blockHitResult, entityHitResult, raycastFirstHit);
        }

        static ValidationResult failure(FailureReason reason) {
            return new ValidationResult(false, reason, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                    null, null, null, null);
        }

        static ValidationResult failure(FailureReason reason, InteractionDistanceCalculator.DistanceResult distance) {
            return from(false, reason, distance, null, null, null);
        }

        static ValidationResult failure(
                FailureReason reason,
                InteractionDistanceCalculator.DistanceResult distance,
                @Nullable BlockHitResult raycastFirstHit
        ) {
            return from(false, reason, distance, null, null, raycastFirstHit);
        }

        private static ValidationResult from(
                boolean valid,
                FailureReason reason,
                InteractionDistanceCalculator.DistanceResult distance,
                @Nullable BlockHitResult blockHitResult,
                @Nullable EntityHitResult entityHitResult,
                @Nullable BlockHitResult raycastFirstHit
        ) {
            return new ValidationResult(
                    valid,
                    reason,
                    distance.preciseDistance(),
                    distance.configuredMaximum(),
                    distance.effectiveMaximum(),
                    distance.vanillaReach(),
                    distance.shapeSource(),
                    blockHitResult,
                    entityHitResult,
                    raycastFirstHit
            );
        }
    }
}
