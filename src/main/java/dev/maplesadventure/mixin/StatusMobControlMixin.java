package dev.maplesadventure.mixin;

import dev.maplesadventure.progression.status.StatusControlLockService;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No cancellable AI-step event exists. Skips the step, never persists or overwrites noAI. */
@Mixin(Mob.class)
abstract class StatusMobControlMixin {
    @Inject(method="serverAiStep",at=@At("HEAD"),cancellable=true)
    private void maplesadventure$statusControl(CallbackInfo ci) {
        if(StatusControlLockService.locked((Mob)(Object)this)) ci.cancel();
    }
}
