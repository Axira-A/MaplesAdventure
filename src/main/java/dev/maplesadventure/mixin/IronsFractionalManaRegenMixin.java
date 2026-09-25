package dev.maplesadventure.mixin;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Iron's 3.16.3 truncates MAX_MANA in regeneration, reducing an already full fractional pool. */
@Pseudo
@Mixin(targets = "io.redspace.ironsspellbooks.capabilities.magic.MagicManager", remap = false)
abstract class IronsFractionalManaRegenMixin {
    @Inject(method = "regenPlayerMana", at = @At("HEAD"), cancellable = true, require = 0)
    private void maplesadventure$preserveFullFractionalMana(ServerPlayer player, MagicData data,
            CallbackInfoReturnable<Boolean> callback) {
        float maximum = (float) player.getAttributeValue(AttributeRegistry.MAX_MANA);
        // Do not intercept expenditure, partial regeneration, integer pools or over-cap correction.
        if (Float.isFinite(maximum) && maximum > 0 && maximum != Math.floor(maximum)
                && data.getMana() == maximum) callback.setReturnValue(false);
    }
}
