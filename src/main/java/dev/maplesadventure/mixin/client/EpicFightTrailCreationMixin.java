package dev.maplesadventure.mixin.client;

import dev.maplesadventure.integration.epicfight.EpicFightIntegration;
import dev.maplesadventure.integration.epicfight.EpicFightSensoryBridge;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents Epic Fight from creating AnimationTrailParticle instances for hidden phase sources. */
@Pseudo
@Mixin(targets = "yesman.epicfight.api.animation.types.StaticAnimation", remap = false)
abstract class EpicFightTrailCreationMixin {
    @Inject(
            method = "lambda$begin$6(Lyesman/epicfight/world/capabilities/entitypatch/LivingEntityPatch;Ljava/util/List;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0)
    private void maplesadventure$filterCrossPhaseWeaponTrails(
            @Coerce Object patch, List<?> trails, CallbackInfo callback) {
        if (EpicFightIntegration.isLoaded() && !EpicFightSensoryBridge.shouldExposePatch(patch)) {
            callback.cancel();
        }
    }
}
