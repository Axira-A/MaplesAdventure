package dev.maplesadventure.mixin;

import dev.maplesadventure.integration.epicfight.progression.EpicFightEncumbranceHooks;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/** Scales only the root-motion vector of an already identified dodge; animation speed is untouched. */
@Pseudo
@Mixin(targets = "yesman.epicfight.api.animation.types.ActionAnimation", remap = false)
abstract class EpicFightDodgeMovementMixin {
    @ModifyVariable(
            method = "move(Lyesman/epicfight/world/capabilities/entitypatch/LivingEntityPatch;Lyesman/epicfight/api/asset/AssetAccessor;)V",
            at = @At(value = "STORE"), ordinal = 0, require = 0)
    private Vec3 maplesadventure$scaleHeavyDodge(Vec3 movement, LivingEntityPatch<?> patch,
                                                 AssetAccessor<? extends DynamicAnimation> animation) {
        return EpicFightEncumbranceHooks.scaleDodgeMovement(movement, patch,
                (yesman.epicfight.api.animation.types.ActionAnimation) (Object) this);
    }
}
