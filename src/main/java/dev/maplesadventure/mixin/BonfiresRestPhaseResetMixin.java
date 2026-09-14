package dev.maplesadventure.mixin;

import dev.maplesadventure.multiplayer.encounter.EncounterBonfireIntegration;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Optional one-instruction hook at Bonfires' committed successful-rest point. */
@Pseudo
@Mixin(targets = "wehavecookies56.bonfires.blocks.AshBonePileBlock", remap = false)
abstract class BonfiresRestPhaseResetMixin {
    @Inject(method = "useItemOn", at = @At(value = "INVOKE",
            target = "Lwehavecookies56/bonfires/data/EstusHandler$EstusHandlerInstance;setLastRested(Ljava/util/UUID;)V",
            shift = At.Shift.AFTER), require = 0)
    private void maplesadventure$resetEncountersAfterCommittedRest(ItemStack stack, BlockState state,
            Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit,
            CallbackInfoReturnable<ItemInteractionResult> callback) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && EncounterBonfireIntegration.isLoaded()) {
            EncounterBonfireIntegration.onSuccessfulRest(serverPlayer, pos);
        }
    }
}
