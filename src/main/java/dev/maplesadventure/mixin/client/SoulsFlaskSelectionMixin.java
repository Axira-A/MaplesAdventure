package dev.maplesadventure.mixin.client;

import dev.maplesadventure.flask.FlaskItem;
import dev.maplesadventure.integration.soulscombathud.SoulsFlaskClient;
import dev.maplesadventure.integration.soulscombathud.SoulsFlaskHooks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.tablesouls.souls_combat_hud.client.util.slots.ConsumableSlotManager", remap = false)
abstract class SoulsFlaskSelectionMixin implements SoulsFlaskHooks.Selection {
    @Inject(method = "isConsumable", at = @At("HEAD"), cancellable = true, require = 0)
    private static void maples$flask(ItemStack stack, Player player, CallbackInfoReturnable<Boolean> ci) {
        if (SoulsFlaskClient.available() && stack.getItem() instanceof FlaskItem)
            ci.setReturnValue(SoulsFlaskClient.recognized(stack));
    }
}
