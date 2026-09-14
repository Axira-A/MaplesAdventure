package dev.maplesadventure.progression.weapon;

import dev.maplesadventure.MaplesAdventure;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import java.util.*;
import java.util.function.Function;

/** Immutable Item lookup rebuilt alongside requirements, never in an attack callback. */
public final class WeaponScalingService {
    private static Map<Item,WeaponScalingProfile> compiled=Map.of();
    private static final Map<ResourceLocation,Function<ItemStack,Optional<WeaponScalingProfile>>> integrations=new TreeMap<>();
    public static void registerIntegration(ResourceLocation id,Function<ItemStack,Optional<WeaponScalingProfile>> rule) { integrations.put(id,rule); }
    public static void compile() {
        Map<Item,WeaponScalingProfile> next=new IdentityHashMap<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item==Items.AIR) continue;
            try {
                var stack=item.getDefaultInstance(); var id=BuiltInRegistries.ITEM.getKey(item);
                var exact=WeaponScalingRules.rules.stream().filter(r->id.equals(r.item())).findFirst();
                var tag=WeaponScalingRules.rules.stream().filter(r->r.tag()!=null && stack.is(TagKey.create(Registries.ITEM,r.tag()))).findFirst();
                WeaponScalingProfile profile=null;
                if (exact.isPresent()) profile=exact.get().profile();
                else if (tag.isPresent()) profile=tag.get().profile();
                else for (var integration : integrations.values()) {
                    var result=integration.apply(stack);
                    if(result.isPresent()) { profile=result.get(); break; }
                }
                if (profile==null) {
                    var facts=WeaponRequirementService.facts(item);
                    profile=facts==null?WeaponScalingProfile.NONE:WeaponScalingProfile.automatic(facts.archetype());
                }
                if (!profile.source().equals("NONE")) {
                    if (next.size()>=WeaponRequirementService.MAX_ITEMS || id.toString().length()>256) throw new IllegalArgumentException("Registry bounds");
                    next.put(item,profile);
                }
            } catch(RuntimeException e) { MaplesAdventure.LOGGER.error("Unable to compile weapon scaling {}",BuiltInRegistries.ITEM.getKey(item),e); }
        }
        compiled=Collections.unmodifiableMap(next);
        MaplesAdventure.LOGGER.info("Compiled {} weapon scaling profiles",next.size());
    }
    public static Map<Item,WeaponScalingProfile> compiled() { return compiled; }
    public static WeaponScalingProfile profile(ItemStack stack) { return stack.isEmpty()?WeaponScalingProfile.NONE:compiled.getOrDefault(stack.getItem(),WeaponScalingProfile.NONE); }
    public static boolean isWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var facts=WeaponRequirementService.facts(stack.getItem());
        return facts!=null && facts.archetype()!=WeaponRequirementArchetype.NONE || profile(stack).enabled();
    }
    public static void clear() { compiled=Map.of(); }
    private WeaponScalingService() {}
}
