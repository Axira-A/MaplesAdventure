package dev.maplesadventure.mixin.client;

import dev.maplesadventure.integration.epicfight.EpicFightIntegration;
import dev.maplesadventure.integration.epicfight.EpicFightSensoryBridge;
import net.minecraft.sounds.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Epic Fight deliberately uses the position-only Level.playLocalSound overload on clients.
 * Filter at its entity-patch boundary, before the source identity is discarded.
 */
@Pseudo
@Mixin(targets = "yesman.epicfight.world.capabilities.entitypatch.HurtableEntityPatch", remap = false)
abstract class EpicFightPatchSoundMixin {
    @Inject(
            method = "playSound(Lnet/minecraft/sounds/SoundEvent;FFF)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0)
    private void maplesadventure$filterCrossPhaseEpicFightSound(
            SoundEvent sound, float volume, float pitchMin, float pitchMax, CallbackInfo callback) {
        if (EpicFightIntegration.isLoaded() && !EpicFightSensoryBridge.shouldExposePatch(this)) {
            callback.cancel();
        }
    }
}
