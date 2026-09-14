package dev.maplesadventure.mixin.client;

import dev.maplesadventure.integration.subtleeffects.SubtleEffectsIntegration;
import dev.maplesadventure.multiplayer.phase.client.PhaseSensoryPolicy;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Stops SubtleEffects entity tickers whose entity is hidden by phase membership. */
@Pseudo
@Mixin(targets = "einstein.subtle_effects.ticking.tickers.entity.EntityTicker", remap = false)
abstract class SubtleEffectsEntityTickerMixin {
    @Shadow @Final protected Entity entity;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 0)
    private void maplesadventure$filterHiddenPhaseEntityTicker(CallbackInfo callback) {
        if (SubtleEffectsIntegration.isLoaded() && !PhaseSensoryPolicy.shouldExposeEntitySource(entity)) {
            callback.cancel();
        }
    }
}
