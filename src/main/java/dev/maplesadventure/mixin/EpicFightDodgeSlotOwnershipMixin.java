package dev.maplesadventure.mixin;

import dev.maplesadventure.integration.epicfight.progression.EpicFightEncumbranceAdapter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;

/** Optional server-side guard for skill books/add-ons; other slots and initial deserialization are untouched. */
@Pseudo
@Mixin(targets = "yesman.epicfight.skill.SkillContainer", remap = false)
abstract class EpicFightDodgeSlotOwnershipMixin {
    @Inject(method = "setSkill(Lyesman/epicfight/skill/Skill;Z)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void maplesadventure$ownDodgeSlot(Skill skill, boolean initialize, CallbackInfoReturnable<Boolean> ci) {
        if (EpicFightEncumbranceAdapter.rejectSlotWrite((SkillContainer) (Object) this, skill))
            ci.setReturnValue(false);
    }
}
