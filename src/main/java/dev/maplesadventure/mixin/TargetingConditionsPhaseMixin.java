package dev.maplesadventure.mixin;

import dev.maplesadventure.multiplayer.phase.PhaseRelations;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Rejects incompatible candidates before NearestAttackableTargetGoal chooses one. */
@Mixin(TargetingConditions.class)
abstract class TargetingConditionsPhaseMixin {
    @Inject(method = "test", at = @At("HEAD"), cancellable = true)
    private void maplesadventure$filterCrossPhaseCandidate(
            @Nullable LivingEntity attacker, LivingEntity candidate,
            CallbackInfoReturnable<Boolean> callback) {
        if (attacker != null && !PhaseRelations.canTarget(attacker, candidate)) {
            callback.setReturnValue(false);
        }
    }
}
