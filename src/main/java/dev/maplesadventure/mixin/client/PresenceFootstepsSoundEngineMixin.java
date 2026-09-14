package dev.maplesadventure.mixin.client;

import dev.maplesadventure.integration.presencefootsteps.PresenceFootstepsIntegration;
import dev.maplesadventure.multiplayer.phase.client.PhaseSensoryPolicy;
import java.util.stream.Stream;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Presence Footsteps plays directly through SoundManager. Filter its source-entity stream before
 * generators run, while retaining shared and phase-compatible entities.
 */
@Pseudo
@Mixin(targets = "eu.ha3.presencefootsteps.sound.SoundEngine", remap = false)
abstract class PresenceFootstepsSoundEngineMixin {
    @Inject(method = "getTargets(Lnet/minecraft/world/entity/Entity;)Ljava/util/stream/Stream;",
            at = @At("RETURN"), cancellable = true, require = 0)
    private void maplesadventure$filterCrossPhaseFootstepSources(
            Entity listener, CallbackInfoReturnable<Stream<? extends Entity>> callback) {
        if (!PresenceFootstepsIntegration.isLoaded()) {
            return;
        }
        Stream<? extends Entity> targets = callback.getReturnValue();
        if (targets != null) {
            callback.setReturnValue(targets.filter(PhaseSensoryPolicy::shouldExposeEntitySource));
        }
    }
}
