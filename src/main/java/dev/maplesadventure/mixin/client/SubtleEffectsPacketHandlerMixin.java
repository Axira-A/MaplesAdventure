package dev.maplesadventure.mixin.client;

import dev.maplesadventure.integration.subtleeffects.SubtleEffectsIntegration;
import dev.maplesadventure.multiplayer.phase.client.PhaseSensoryPolicy;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Preserves the payload's entityId as the source identity and hides only effects whose resolved
 * entity is cross-phase. Position-only and environment payloads are untouched.
 */
@Pseudo
@Mixin(targets = "einstein.subtle_effects.networking.clientbound.ClientPacketHandlers", remap = false)
abstract class SubtleEffectsPacketHandlerMixin {
    @Redirect(
            method = {
                    "handle(Lnet/minecraft/client/multiplayer/ClientLevel;Leinstein/subtle_effects/networking/clientbound/ClientBoundEntityFellPayload;)V",
                    "handle(Lnet/minecraft/client/multiplayer/ClientLevel;Leinstein/subtle_effects/networking/clientbound/ClientBoundEntitySpawnSprintingDustCloudsPayload;)V",
                    "handle(Lnet/minecraft/client/multiplayer/ClientLevel;Leinstein/subtle_effects/networking/clientbound/ClientBoundDrankPotionPayload;)V",
                    "handle(Lnet/minecraft/client/multiplayer/ClientLevel;Leinstein/subtle_effects/networking/clientbound/ClientBoundEntityLandInFluidPayload;)V"
            },
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getEntity(I)Lnet/minecraft/world/entity/Entity;"),
            require = 0
    )
    private static Entity maplesadventure$filterHiddenPhasePayloadSource(ClientLevel level, int entityId) {
        Entity entity = level.getEntity(entityId);
        if (SubtleEffectsIntegration.isLoaded() && entity != null
                && !PhaseSensoryPolicy.shouldExposeEntitySource(entity)) {
            return null;
        }
        return entity;
    }
}
