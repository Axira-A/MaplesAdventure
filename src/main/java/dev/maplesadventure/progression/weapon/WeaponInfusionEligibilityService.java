package dev.maplesadventure.progression.weapon;

import dev.maplesadventure.MaplesAdventure;
import net.minecraft.core.registries.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import java.util.*;

public final class WeaponInfusionEligibilityService {
    private static Map<Item,WeaponInfusionEligibility> compiled=Map.of();

    public static void compile() {
        var next=new IdentityHashMap<Item,WeaponInfusionEligibility>();
        var all=WeaponInfusionRegistry.definitions().keySet().stream().filter(id->!WeaponInfusionRegistry.requiresExplicitEligibility(id)).collect(java.util.stream.Collectors.toUnmodifiableSet());
        for(Item item:BuiltInRegistries.ITEM) {
            if(item==Items.AIR) continue;
            try {
                var stack=item.getDefaultInstance(); var id=BuiltInRegistries.ITEM.getKey(item);
                var exact=WeaponInfusionEligibilityRules.rules.stream().filter(rule->id.equals(rule.item())).findFirst();
                var selected=exact.isPresent()?exact:WeaponInfusionEligibilityRules.rules.stream()
                        .filter(rule->rule.tag()!=null&&stack.is(TagKey.create(Registries.ITEM,rule.tag()))).findFirst();
                Set<net.minecraft.resources.ResourceLocation> allowed;
                if(selected.isPresent()) {
                    var rule=selected.get();
                    if(!rule.infusible()) allowed=Set.of(WeaponInfusionRegistry.NORMAL_ID);
                    else allowed=rule.allowed().isEmpty()?all:rule.allowed();
                } else {
                    var damage=WeaponDamageProfileService.profile(stack);
                    boolean weapon=WeaponScalingService.isWeapon(stack)||WeaponDamageProfileService.explicitWeapon(stack);
                    allowed=weapon&&WeaponInfusionEligibility.simplePhysical(damage)?all:Set.of(WeaponInfusionRegistry.NORMAL_ID);
                }
                var safe=new HashSet<net.minecraft.resources.ResourceLocation>(); safe.add(WeaponInfusionRegistry.NORMAL_ID);
                allowed.stream().filter(WeaponInfusionRegistry.definitions()::containsKey).limit(WeaponInfusionEligibility.MAX_ALLOWED).forEach(safe::add);
                next.put(item,new WeaponInfusionEligibility(safe));
            } catch(RuntimeException error) { MaplesAdventure.LOGGER.error("Unable to compile infusion eligibility for {}",item,error); }
        }
        compiled=Collections.unmodifiableMap(next);
        MaplesAdventure.LOGGER.info("Compiled {} weapon infusion eligibility entries",next.size());
    }
    public static WeaponInfusionEligibility eligibility(ItemStack stack) {
        return compiled.getOrDefault(stack.getItem(),new WeaponInfusionEligibility(Set.of(WeaponInfusionRegistry.NORMAL_ID)));
    }
    public static Map<Item,WeaponInfusionEligibility> compiled() { return compiled; }
    public static void clear() { compiled=Map.of(); }
    private WeaponInfusionEligibilityService() {}
}
