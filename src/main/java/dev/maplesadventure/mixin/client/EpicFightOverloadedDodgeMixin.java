package dev.maplesadventure.mixin.client;

import dev.maplesadventure.progression.client.ClientAttributeState;
import dev.maplesadventure.progression.encumbrance.EquipLoadTier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.event.types.player.SkillCastEvent;
import yesman.epicfight.client.events.engine.ControlEngine;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlots;

/** Client-side feedback/packet suppression only; the CAST listener remains server authority. */
@Pseudo
@Mixin(targets = "yesman.epicfight.skill.SkillContainer", remap = false)
abstract class EpicFightOverloadedDodgeMixin {
    @Inject(method = "sendCastRequest", at = @At("HEAD"), cancellable = true, require = 0)
    private void maplesadventure$rejectOverloadedDodge(LocalPlayerPatch executor, ControlEngine controls,
                                                        CallbackInfoReturnable<SkillCastEvent> callback) {
        SkillContainer self = (SkillContainer) (Object) this;
        if (self.getSlot() != SkillSlots.DODGE
                || ClientAttributeState.snapshot().equipLoad().tier() != EquipLoadTier.OVERLOADED) return;
        SkillCastEvent rejected = new SkillCastEvent(executor, self, new CompoundTag());
        rejected.cancel();
        executor.getOriginal().displayClientMessage(
                Component.translatable("message.maplesadventure.encumbrance.cannot_dodge"), true);
        callback.setReturnValue(rejected);
    }
}
