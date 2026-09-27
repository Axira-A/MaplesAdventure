package dev.maplesadventure.mixin;

import dev.maplesadventure.api.flask.FlaskApi;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mouse release must not turn a press-started action back into hold-to-drink. Controller uses stopUsingItem. */
@Mixin(LivingEntity.class)
abstract class FlaskReleaseMixin {
    @Inject(method = "releaseUsingItem", at = @At("HEAD"), cancellable = true)
    private void maplesadventure$independentFlaskAction(CallbackInfo ci) {
        if (FlaskApi.isUsingFlask((LivingEntity)(Object)this)) ci.cancel();
    }
}
