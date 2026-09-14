package dev.maplesadventure.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.maplesadventure.multiplayer.phase.loot.PhaseExperienceSpawnContext;
import dev.maplesadventure.multiplayer.phase.loot.PhaseLootService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Propagates encounter provenance around the exact final Vanilla XP award call. */
@Mixin(LivingEntity.class)
abstract class LivingEntityPhaseExperienceMixin {
    @WrapOperation(method = "dropExperience", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ExperienceOrb;award(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;I)V"))
    private void maplesadventure$phaseExperienceAward(ServerLevel level, Vec3 position, int amount,
                                                       Operation<Void> original) {
        var provenance = PhaseLootService.stateForEncounterDeath((LivingEntity) (Object) this);
        if (provenance.isEmpty()) {
            original.call(level, position, amount);
            return;
        }
        try (PhaseExperienceSpawnContext.Scope ignored = PhaseExperienceSpawnContext.enter(provenance.get())) {
            original.call(level, position, amount);
        }
    }
}
