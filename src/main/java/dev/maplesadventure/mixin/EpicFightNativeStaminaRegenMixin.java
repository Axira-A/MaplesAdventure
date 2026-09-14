package dev.maplesadventure.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import yesman.epicfight.registry.entries.EpicFightAttributes;

/**
 * Prevents only Epic Fight's native nonlinear regeneration pass from seeing a positive regen value.
 * The real STAMINA_REGEN attribute remains untouched and is consumed by MaplesAdventure as a multiplier.
 */
@Pseudo
@Mixin(targets = "yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch", remap = false)
abstract class EpicFightNativeStaminaRegenMixin {
    @Redirect(
            method = "preTickServer",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getAttributeValue(Lnet/minecraft/core/Holder;)D"),
            require = 0)
    private double maplesadventure$disableNativeStaminaRegen(LivingEntity entity, Holder<Attribute> attribute) {
        if (attribute.value() == EpicFightAttributes.STAMINA_REGEN.value()) return 0.0D;
        return entity.getAttributeValue(attribute);
    }
}
