package dev.maplesadventure.mixin;

import dev.maplesadventure.multiplayer.phase.PhaseRelations;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Filters only incompatible entity-to-entity pushes; block physics remains entirely vanilla. */
@Mixin(LivingEntity.class)
abstract class LivingEntityPhaseCollisionMixin {
    @Inject(method = "doPush", at = @At("HEAD"), cancellable = true)
    private void maplesadventure$filterCrossPhaseEntityPush(Entity other, CallbackInfo callback) {
        if (!PhaseRelations.canCollide((Entity) (Object) this, other)) {
            callback.cancel();
        }
    }
}
