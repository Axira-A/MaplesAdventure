package dev.maplesadventure.mixin;

import dev.maplesadventure.integration.epicfight.progression.EpicFightEncumbranceHooks;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.network.client.CPChangeSkill;

/** Server-side authority: the Epic Fight skill screen cannot replace the managed DODGE slot. */
@Pseudo
@Mixin(targets = "yesman.epicfight.network.EpicFightServerBoundPayloadHandler", remap = false)
interface EpicFightDodgeSkillChangeMixin {
    @Inject(method = "handleChangeSkill", at = @At("HEAD"), cancellable = true, require = 0)
    private static void maplesadventure$lockDodgeSlot(CPChangeSkill request, IPayloadContext context,
                                                       CallbackInfo callback) {
        if (EpicFightEncumbranceHooks.rejectManualDodge(request, context.player())) callback.cancel();
    }
}
