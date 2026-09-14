package dev.maplesadventure.mixin;

import dev.maplesadventure.multiplayer.phase.PhaseRelations;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevents unowned/shared Vanilla hoppers from converting phase-bound entities into shared inventory stacks. */
@Mixin(HopperBlockEntity.class)
abstract class HopperBlockEntityPhaseMixin {
    @Inject(method = "addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/entity/item/ItemEntity;)Z",
            at = @At("HEAD"), cancellable = true)
    private static void maplesadventure$rejectPhaseItem(Container destination, ItemEntity item,
                                                         CallbackInfoReturnable<Boolean> callback) {
        if (!PhaseRelations.canSharedAutomationCollect(item)) callback.setReturnValue(false);
    }
}
