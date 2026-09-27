package dev.maplesadventure.mixin.client;

import dev.maplesadventure.integration.soulscombathud.SoulsFlaskClient;
import dev.maplesadventure.integration.soulscombathud.SoulsFlaskHooks;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.tablesouls.souls_combat_hud.event.ClientForgeEvents", remap = false)
abstract class SoulsFlaskUseMixin implements SoulsFlaskHooks.Use {
    @Inject(method = "jumpToSelectedConsumable", at = @At("HEAD"), cancellable = true, require = 0)
    private static void maples$singleFlaskIntent(Player player, CallbackInfo ci) {
        int slot = SoulsFlaskClient.requestSlot(player);
        if (slot == -1) return;
        ci.cancel();
        if (slot < 0) return;
        SoulsFlaskClient.use(player, slot);
    }
}
