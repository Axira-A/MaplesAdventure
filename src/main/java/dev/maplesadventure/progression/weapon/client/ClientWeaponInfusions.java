package dev.maplesadventure.progression.weapon.client;

import dev.maplesadventure.progression.weapon.*;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Server-synchronized definition cache used only for client presentation. */
public final class ClientWeaponInfusions {
    private static Map<net.minecraft.resources.ResourceLocation,WeaponInfusionDefinition> definitions=WeaponInfusionRegistry.definitions();
    public static void replace(Collection<WeaponInfusionDefinition> values) {
        var next=new HashMap<net.minecraft.resources.ResourceLocation,WeaponInfusionDefinition>();
        for(var value:values) if(next.put(value.id(),value)!=null||next.size()>WeaponInfusionDefinition.MAX_DEFINITIONS)
            throw new IllegalArgumentException("Invalid client infusion registry");
        definitions=Map.copyOf(next);
    }
    public static WeaponCombatProfileResolver.Resolved resolve(ItemStack stack,WeaponRequirementNetwork.Entry entry) {
        var base=new WeaponCombatProfileResolver.Resolved(entry.profile(),entry.scaling(),entry.damage(),entry.weapon(),WeaponInfusionView.normal(),entry.statuses());
        return WeaponCombatProfileResolver.resolveInfusion(base,WeaponInfusionService.state(stack).orElse(null),definitions,entry.infusionEligibility());
    }
    public static void clear() { definitions=WeaponInfusionRegistry.definitions(); }
    private ClientWeaponInfusions() {}
}
