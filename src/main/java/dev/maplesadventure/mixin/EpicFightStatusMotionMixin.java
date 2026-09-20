package dev.maplesadventure.mixin;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;

/** Source creation is the last reliable phase context; no private field access or animation-time guessing. */
@Pseudo @Mixin(targets="yesman.epicfight.api.animation.types.AttackAnimation",remap=false)
abstract class EpicFightStatusMotionMixin {
    @Inject(method="getEpicFightDamageSource(Lnet/minecraft/world/damagesource/DamageSource;Lyesman/epicfight/world/capabilities/entitypatch/LivingEntityPatch;Lnet/minecraft/world/entity/Entity;Lyesman/epicfight/api/animation/types/AttackAnimation$Phase;)Lyesman/epicfight/world/damagesource/EpicFightDamageSource;",
            at=@At("RETURN"),require=0)
    private void maplesadventure$capturePhase(DamageSource base,LivingEntityPatch<?> patch,Entity target,AttackAnimation.Phase phase,CallbackInfoReturnable<EpicFightDamageSource> cir) {
        dev.maplesadventure.integration.epicfight.progression.EpicFightStatusMotionAdapter.capture((AttackAnimation)(Object)this,phase,cir.getReturnValue());
    }
}
