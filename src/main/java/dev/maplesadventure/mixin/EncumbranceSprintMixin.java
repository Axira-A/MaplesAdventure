package dev.maplesadventure.mixin;

import dev.maplesadventure.progression.encumbrance.EncumbranceRuntimeService;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No cancellable vanilla sprint-start event exists; keep client prediction and server flags in agreement. */
@Mixin(LivingEntity.class)
abstract class EncumbranceSprintMixin {
    @Inject(method = "setSprinting", at = @At("HEAD"), cancellable = true)
    private void maplesadventure$preventOverloadedSprint(boolean sprinting, CallbackInfo ci) {
        if (sprinting && (!EncumbranceRuntimeService.canSprint((LivingEntity) (Object) this)
                || dev.maplesadventure.progression.status.StatusControlLockService.locked((LivingEntity)(Object)this))) ci.cancel();
    }
}
