package dev.maplesadventure.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.maplesadventure.multiplayer.phase.PhaseRelations;
import dev.maplesadventure.multiplayer.phase.loot.PhaseExperienceSpawnContext;
import dev.maplesadventure.multiplayer.phase.mob.ModPhaseAttachments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps Vanilla XP splitting while adding provenance before spawn and filtering both merge paths. */
@Mixin(ExperienceOrb.class)
abstract class ExperienceOrbPhaseMixin {
    @Shadow @Nullable private Player followingPlayer;

    @WrapMethod(method = "award")
    private static void maplesadventure$scopeEveryAward(ServerLevel level, Vec3 position, int amount,
                                                         Operation<Void> original) {
        try (PhaseExperienceSpawnContext.Scope ignored = PhaseExperienceSpawnContext.enterSharedIfAbsent()) {
            original.call(level, position, amount);
        }
    }

    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;DDDI)V", at = @At("RETURN"))
    private void maplesadventure$attachAwardProvenance(Level level, double x, double y, double z, int value,
                                                        CallbackInfo callback) {
        PhaseExperienceSpawnContext.current().ifPresent(state ->
                ((ExperienceOrb) (Object) this).setData(ModPhaseAttachments.PHASE_OBJECT, state.copy()));
    }

    /** This overload is used before award decides whether it needs to construct a new orb. */
    @Inject(method = "canMerge(Lnet/minecraft/world/entity/ExperienceOrb;II)Z", at = @At("HEAD"), cancellable = true)
    private static void maplesadventure$filterAwardMerge(ExperienceOrb existing, int id, int value,
                                                          CallbackInfoReturnable<Boolean> callback) {
        if (PhaseExperienceSpawnContext.isScoped() && !PhaseExperienceSpawnContext.canMergeWith(existing)) {
            callback.setReturnValue(false);
        }
    }

    /** This overload is used by the periodic nearby-orb merge scan. */
    @Inject(method = "canMerge(Lnet/minecraft/world/entity/ExperienceOrb;)Z", at = @At("HEAD"), cancellable = true)
    private void maplesadventure$filterPeriodicMerge(ExperienceOrb other,
                                                      CallbackInfoReturnable<Boolean> callback) {
        if (!PhaseRelations.canMerge((ExperienceOrb) (Object) this, other)) callback.setReturnValue(false);
    }

    /** A player whose role/phase changed stops attracting this orb immediately. */
    @Inject(method = "tick", at = @At("HEAD"))
    private void maplesadventure$dropIncompatibleFollower(CallbackInfo callback) {
        if (followingPlayer != null
                && !PhaseRelations.canCollect(followingPlayer, (ExperienceOrb) (Object) this)) {
            followingPlayer = null;
        }
    }
}
