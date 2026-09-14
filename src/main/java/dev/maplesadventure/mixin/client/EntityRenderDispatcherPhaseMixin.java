package dev.maplesadventure.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.maplesadventure.multiplayer.phase.PhaseRelations;
import dev.maplesadventure.multiplayer.phase.PhaseRole;
import dev.maplesadventure.client.render.PhantomTint;
import dev.maplesadventure.client.render.PhantomTintingMultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cancels the entire dispatcher path so model, layers, name, fire, debug hitbox and shadow cannot leak. */
@Mixin(EntityRenderDispatcher.class)
abstract class EntityRenderDispatcherPhaseMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void maplesadventure$hideCrossPhaseEntity(
            E entity, double x, double y, double z, float yaw, float partialTick,
            PoseStack poseStack, MultiBufferSource buffers, int packedLight, CallbackInfo callback) {
        Player viewer = Minecraft.getInstance().player;
        if (viewer != null && entity != viewer
                && !PhaseRelations.canSee(viewer, entity)) {
            callback.cancel();
        }
    }

    @Redirect(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void maplesadventure$renderSessionPhantom(
            EntityRenderer renderer, Entity entity, float yaw, float partialTick,
            PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        Player viewer = Minecraft.getInstance().player;
        PhaseRole role = entity instanceof Player player ? PhaseRelations.state(player).role() : null;
        boolean visiblePhantom = entity instanceof Player player
                && (role == PhaseRole.COOPERATOR || role == PhaseRole.INVADER)
                && (player == viewer || viewer != null && PhaseRelations.canSee(viewer, player));
        PhantomTint tint = role == PhaseRole.INVADER ? PhantomTint.RED_INVADER : PhantomTint.GOLD_COOPERATOR;
        int dynamicAlpha = role == PhaseRole.INVADER
                ? dev.maplesadventure.multiplayer.invasion.client.InvasionMaterializationCache.playerAlpha(entity.getUUID()) : 255;
        renderer.render(entity, yaw, partialTick, poseStack,
                visiblePhantom ? new PhantomTintingMultiBufferSource(buffers, tint, dynamicAlpha) : buffers,
                visiblePhantom ? PhantomTint.FULL_BRIGHT : packedLight);
    }
}
