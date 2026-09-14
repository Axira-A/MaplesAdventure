package dev.maplesadventure.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** canUse's charging early return skips CAST_SKILL; guard before that path and before resources. */
@Pseudo @Mixin(targets="yesman.epicfight.skill.SkillContainer",remap=false)
public abstract class EpicFightWeaponRequirementMixin {
    @Inject(method="canUse",at=@At("HEAD"),cancellable=true,require=0)
    private void maplesadventure$requirements(CallbackInfoReturnable<Boolean> cir) {
        if(dev.maplesadventure.integration.epicfight.progression.EpicFightWeaponRequirements.reject(
                (yesman.epicfight.skill.SkillContainer)(Object)this)) cir.setReturnValue(false);
    }
}
