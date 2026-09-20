package dev.maplesadventure.mixin;

import dev.maplesadventure.progression.status.StatusDamageSources;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** NO_IMPACT/NO_KNOCKBACK do not skip handleDamageEvent's hurtTime and walk-animation write. */
@Mixin(LivingEntity.class)
abstract class StatusQuietDamageMixin {
    @Inject(method="handleDamageEvent",at=@At("HEAD"),cancellable=true)
    private void maplesadventure$quietDot(DamageSource source,CallbackInfo ci) {
        if(StatusDamageSources.isQuietDot(source)) ci.cancel();
    }
}
