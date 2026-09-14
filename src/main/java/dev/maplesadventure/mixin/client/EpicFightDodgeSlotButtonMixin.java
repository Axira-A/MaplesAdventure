package dev.maplesadventure.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlots;

/** This private Epic Fight widget rebuilds active/tooltip every render; no public slot-edit event exists. */
@Pseudo
@Mixin(targets = "yesman.epicfight.client.gui.screen.SkillEditScreen$SlotButton", remap = false)
abstract class EpicFightDodgeSlotButtonMixin {
    @Shadow @Final private SkillContainer skillContainer;
    @Inject(method = "renderWidget", at = @At("TAIL"), require = 0)
    private void maplesadventure$explainAutomaticDodge(GuiGraphics graphics, int x, int y, float partial, CallbackInfo ci) {
        if (skillContainer.getSlot() != SkillSlots.DODGE) return;
        Button self = (Button) (Object) this;
        self.active = false;
        self.setTooltip(Tooltip.create(Component.translatable("message.maplesadventure.encumbrance.dodge_locked")));
    }
}
