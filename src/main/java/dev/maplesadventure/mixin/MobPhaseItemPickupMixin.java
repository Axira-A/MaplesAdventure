package dev.maplesadventure.mixin;

import dev.maplesadventure.multiplayer.phase.PhaseRelations;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents a shared or wrong-phase mob from converting phase loot into shared equipment. */
@Mixin(Mob.class)
abstract class MobPhaseItemPickupMixin {
    @Inject(method = "pickUpItem", at = @At("HEAD"), cancellable = true)
    private void maplesadventure$filterPhaseItemPickup(ItemEntity item, CallbackInfo callback) {
        if (!PhaseRelations.canCollect((Mob) (Object) this, item)) callback.cancel();
    }
}
