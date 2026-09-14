package dev.maplesadventure.progression.weapon;

import dev.maplesadventure.MaplesAdventure;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import java.util.*;
import java.util.function.Function;

public final class WeaponDamageProfileService {
    private static Map<Item,WeaponDamageProfile> compiled=Map.of();
    private static final Map<ResourceLocation,Function<ItemStack,Optional<WeaponDamageProfile>>> integrations=new TreeMap<>();
    public static void registerIntegration(ResourceLocation id,Function<ItemStack,Optional<WeaponDamageProfile>> rule) { integrations.put(id,rule); }
    public static void compile() {
        Map<Item,WeaponDamageProfile> next=new IdentityHashMap<>();
        for(Item item:BuiltInRegistries.ITEM) {
            if(item==Items.AIR) continue;
            try {
                var stack=item.getDefaultInstance(); var id=BuiltInRegistries.ITEM.getKey(item);
                var facts=WeaponRequirementService.facts(item);
                var automatic=WeaponDamageProfile.automatic(facts==null?WeaponRequirementArchetype.GENERIC:facts.archetype());
                var exact=WeaponDamageProfileRules.rules.stream().filter(r->id.equals(r.item())).findFirst();
                var selected=exact.isPresent()?exact:WeaponDamageProfileRules.rules.stream().filter(r->r.tag()!=null&&stack.is(TagKey.create(Registries.ITEM,r.tag()))).findFirst();
                WeaponDamageProfile profile=null;
                if(selected.isPresent()) {
                    // disabled removes this layer's overrides, never zeros all pre-existing weapon damage.
                    profile=selected.get().disabled()?automatic:selected.get().profile();
                } else for(var integration:integrations.values()) { var value=integration.apply(stack); if(value.isPresent()) { profile=value.get(); break; } }
                if(profile==null && WeaponScalingService.isWeapon(stack)) profile=automatic;
                if(profile!=null) {
                    if(next.size()>=WeaponRequirementService.MAX_ITEMS||id.toString().length()>256) throw new IllegalArgumentException("Damage registry bounds");
                    next.put(item,profile);
                }
            } catch(RuntimeException e) { MaplesAdventure.LOGGER.error("Unable to compile weapon damage {}",item,e); }
        }
        compiled=Collections.unmodifiableMap(next);
        MaplesAdventure.LOGGER.info("Compiled {} weapon damage profiles",next.size());
    }
    public static Map<Item,WeaponDamageProfile> compiled() { return compiled; }
    public static WeaponDamageProfile profile(ItemStack stack) { return stack.isEmpty()?WeaponDamageProfile.STANDARD:compiled.getOrDefault(stack.getItem(),WeaponDamageProfile.STANDARD); }
    public static boolean explicitWeapon(ItemStack stack) { return !stack.isEmpty()&&compiled.containsKey(stack.getItem())&&!profile(stack).source().equals("ARCHETYPE"); }
    public static void clear() { compiled=Map.of(); }
    private WeaponDamageProfileService() {}
}
