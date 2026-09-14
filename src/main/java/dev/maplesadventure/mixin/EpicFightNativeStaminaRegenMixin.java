package dev.maplesadventure.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Replaces only the local staminaRegen value used by Epic Fight's native nonlinear regeneration pass.
 * PlayerPatch.preTickServer stores stamina, maxStamina, then staminaRegen as its first three float locals.
 * The real STAMINA_REGEN attribute remains untouched for MaplesAdventure's multiplier calculation.
 */
@Pseudo
@Mixin(targets = "yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch", remap = false)
abstract class EpicFightNativeStaminaRegenMixin {
    @ModifyVariable(method = "preTickServer", at = @At("STORE"), ordinal = 2, require = 1)
    private float maplesadventure$disableNativeStaminaRegen(float original) {
        return 0.0F;
    }
}
