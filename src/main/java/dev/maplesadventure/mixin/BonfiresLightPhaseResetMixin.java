package dev.maplesadventure.mixin;

import dev.maplesadventure.multiplayer.encounter.EncounterBonfireIntegration;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lighting a new bonfire commits the same rest state and therefore performs one reset. */
@Pseudo
@Mixin(targets = "wehavecookies56.bonfires.packets.server.LightBonfire", remap = false)
abstract class BonfiresLightPhaseResetMixin {
    @org.spongepowered.asm.mixin.Shadow public abstract net.minecraft.core.BlockPos bonfireTE();
    @Inject(method = "handle", at = @At(value = "INVOKE",
            target = "Lwehavecookies56/bonfires/data/EstusHandler$EstusHandlerInstance;setLastRested(Ljava/util/UUID;)V",
            shift = At.Shift.AFTER), require = 0)
    private void maplesadventure$resetEncountersAfterBonfireLit(IPayloadContext context, CallbackInfo callback) {
        if (context.player() instanceof ServerPlayer player && EncounterBonfireIntegration.isLoaded()) {
            EncounterBonfireIntegration.onSuccessfulRest(player, bonfireTE());
        }
    }
}
