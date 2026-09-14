package dev.maplesadventure.integration.irons.progression;
import dev.maplesadventure.progression.weapon.WeaponIntegrations;
import dev.maplesadventure.mixin.IronsSchoolPowerAccessor;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.resources.ResourceLocation;
import java.util.*;
public final class IronsWeaponAffinity implements WeaponIntegrations.Affinity {
    public Map<ResourceLocation,Double> schools(ItemStack stack) {
        Map<ResourceLocation,Double> result=new HashMap<>();
        stack.forEachModifier(EquipmentSlot.MAINHAND,(attribute,modifier)-> {
            if(modifier.amount()<=0) return;
            for(var school:SchoolRegistry.REGISTRY)
                if(((IronsSchoolPowerAccessor)school).maplesadventure$powerAttribute().equals(attribute))
                    result.merge(school.getId(),modifier.amount(),Math::max);
        });
        return result;
    }
}
