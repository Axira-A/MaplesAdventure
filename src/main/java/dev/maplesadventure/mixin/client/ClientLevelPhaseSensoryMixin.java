package dev.maplesadventure.mixin.client;

import dev.maplesadventure.multiplayer.phase.client.PhaseSensoryPolicy;
import dev.maplesadventure.multiplayer.phase.client.PhaseSensorySourceContext;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Covers client-local sound paths and keeps exact entity source context around particle creation.
 * It never filters position-only sounds or particles outside a hidden player's own tick.
 */
@Mixin(ClientLevel.class)
abstract class ClientLevelPhaseSensoryMixin {
    @Inject(method = "playLocalSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V",
            at = @At("HEAD"), cancellable = true)
    private void maplesadventure$filterEntityBoundLocalSound(
            Entity source, SoundEvent sound, SoundSource category, float volume, float pitch, CallbackInfo callback) {
        if (!PhaseSensoryPolicy.shouldExposeEntitySource(source)) {
            callback.cancel();
        }
    }

    @Inject(method = "tickNonPassenger", at = @At("HEAD"))
    private void maplesadventure$pushNonPassengerSource(Entity entity, CallbackInfo callback) {
        PhaseSensorySourceContext.push(entity);
    }

    @Inject(method = "tickNonPassenger", at = @At("RETURN"))
    private void maplesadventure$popNonPassengerSource(Entity entity, CallbackInfo callback) {
        PhaseSensorySourceContext.pop();
    }

    @Inject(method = "tickPassenger", at = @At("HEAD"))
    private void maplesadventure$pushPassengerSource(Entity vehicle, Entity passenger, CallbackInfo callback) {
        PhaseSensorySourceContext.push(passenger);
    }

    @Inject(method = "tickPassenger", at = @At("RETURN"))
    private void maplesadventure$popPassengerSource(Entity vehicle, Entity passenger, CallbackInfo callback) {
        PhaseSensorySourceContext.pop();
    }

    @Inject(method = "addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V",
            at = @At("HEAD"), cancellable = true)
    private void maplesadventure$filterEntityTickParticle(
            ParticleOptions particle, double x, double y, double z,
            double velocityX, double velocityY, double velocityZ, CallbackInfo callback) {
        if (PhaseSensorySourceContext.suppressesEntityParticles()) {
            callback.cancel();
        }
    }

    @Inject(method = "addParticle(Lnet/minecraft/core/particles/ParticleOptions;ZDDDDDD)V",
            at = @At("HEAD"), cancellable = true)
    private void maplesadventure$filterForcedEntityTickParticle(
            ParticleOptions particle, boolean force, double x, double y, double z,
            double velocityX, double velocityY, double velocityZ, CallbackInfo callback) {
        if (PhaseSensorySourceContext.suppressesEntityParticles()) {
            callback.cancel();
        }
    }
}
