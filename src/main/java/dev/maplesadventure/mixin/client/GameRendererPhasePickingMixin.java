package dev.maplesadventure.mixin.client;

import dev.maplesadventure.multiplayer.phase.PhaseRelations;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Adds one membership predicate term to vanilla picking; it does not replace the pick algorithm. */
@Mixin(GameRenderer.class)
abstract class GameRendererPhasePickingMixin {
    @ModifyArg(
            method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/ProjectileUtil;getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;"),
            index = 4
    )
    private Predicate<Entity> maplesadventure$excludeCrossPhaseEntities(Predicate<Entity> vanillaPredicate) {
        Entity viewer = Minecraft.getInstance().getCameraEntity();
        return viewer == null ? vanillaPredicate : vanillaPredicate.and(target -> PhaseRelations.canTarget(viewer, target));
    }
}
