package dev.maplesadventure.mixin;

import dev.maplesadventure.multiplayer.phase.PhaseRelations;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Guards Vanilla's single ItemEntity-to-ItemEntity merge entry point. */
@Mixin(ItemEntity.class)
abstract class ItemEntityPhaseMergeMixin {
    @Inject(method = "tryToMerge", at = @At("HEAD"), cancellable = true)
    private void maplesadventure$filterPhaseMerge(ItemEntity other, CallbackInfo callback) {
        if (!PhaseRelations.canMerge((ItemEntity) (Object) this, other)) callback.cancel();
    }
}
