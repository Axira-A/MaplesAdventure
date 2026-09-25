package dev.maplesadventure.mixin;

import dev.maplesadventure.bonfire.BonfirePoseLock;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No public event covers every entity push/move (including piston and direct velocity paths). */
@Mixin(Entity.class)
abstract class BonfirePoseLockMixin {
    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void maplesadventure$holdBonfirePose(MoverType type, Vec3 movement, CallbackInfo ci) {
        if (BonfirePoseLock.locked((Entity) (Object) this)) ci.cancel();
    }

    @Inject(method = "push(DDD)V", at = @At("HEAD"), cancellable = true)
    private void maplesadventure$ignoreBonfireImpulse(double x, double y, double z, CallbackInfo ci) {
        if (BonfirePoseLock.locked((Entity) (Object) this)) ci.cancel();
    }
}
