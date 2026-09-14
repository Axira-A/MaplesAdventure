package dev.maplesadventure.progression.weapon;
import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.config.WeaponRequirementConfig;
import dev.maplesadventure.progression.*;
import dev.maplesadventure.progression.spell.SpellSchoolScalingRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import java.util.*;
import java.util.function.Function;
public final class WeaponRequirementService {
    public static final int MAX_ITEMS=32768;
    private static Map<Item,WeaponRequirementProfile> compiled=Map.of();
    private static Map<Item,WeaponFacts> facts=Map.of();
    private static final Map<ResourceLocation,Function<ItemStack,Optional<WeaponRequirementProfile>>> integrations=new TreeMap<>();
    public static UUID revision=UUID.randomUUID();
    public static void registerIntegration(ResourceLocation id,Function<ItemStack,Optional<WeaponRequirementProfile>> rule) { integrations.put(id,rule); }
    public static void compile() {
        Map<Item,WeaponRequirementProfile> next=new IdentityHashMap<>(); Map<Item,WeaponFacts> inspected=new IdentityHashMap<>();
        for(Item item:BuiltInRegistries.ITEM) {
            if(item==Items.AIR) continue;
            try {
                var stack=item.getDefaultInstance();
                // Inspect independently of requirement overrides: disabled requirements do not disable AR.
                WeaponFacts f=null;
                try { f=WeaponClassifier.inspect(stack); inspected.put(item,f); }
                catch(RuntimeException failure) { MaplesAdventure.LOGGER.warn("Unable to inspect weapon {}",item,failure); }
                var id=BuiltInRegistries.ITEM.getKey(item);
                var exact=WeaponRequirementRules.rules.stream().filter(r->id.equals(r.item())).findFirst();
                var tag=WeaponRequirementRules.rules.stream().filter(r->r.tag()!=null && stack.is(TagKey.create(Registries.ITEM,r.tag()))).findFirst();
                WeaponRequirementProfile p;
                if(exact.isPresent()) p=exact.get().profile();
                else if(tag.isPresent()) p=tag.get().profile();
                else {
                    p=null;
                    for(var rule:integrations.values()) { var result=rule.apply(stack); if(result.isPresent()) { p=result.get(); break; } }
                    if(p==null) {
                        p=f==null?WeaponRequirementProfile.NONE:WeaponRequirementHeuristic.generate(f,SpellSchoolScalingRegistry.profiles(),WeaponRequirementConfig.AUTO_CAP.get());
                    }
                }
                if(!p.source().equals("NONE")) {
                    if(next.size()>=MAX_ITEMS || id.toString().length()>256) throw new IllegalArgumentException("Registry wire limit");
                    next.put(item,p);
                }
            } catch(RuntimeException e) { MaplesAdventure.LOGGER.error("Unable to compile weapon requirement {}",BuiltInRegistries.ITEM.getKey(item),e); }
        }
        compiled=Collections.unmodifiableMap(next); facts=Collections.unmodifiableMap(inspected); revision=UUID.randomUUID();
        MaplesAdventure.LOGGER.info("Compiled {} weapon requirement profiles ({} items inspected)",compiled.size(),facts.size());
        WeaponScalingService.compile();
        WeaponDamageProfileService.compile();
        WeaponInfusionEligibilityService.compile();
    }
    public static Map<Item,WeaponRequirementProfile> compiled() { return compiled; }
    public static WeaponFacts facts(Item item) { return facts.get(item); }
    public static WeaponRequirementProfile profile(ItemStack stack) { return stack.isEmpty()?WeaponRequirementProfile.NONE:compiled.getOrDefault(stack.getItem(),WeaponRequirementProfile.NONE); }
    public static WeaponRequirementResult evaluate(ServerPlayer player,ItemStack stack) {
        return evaluate(PlayerAttributeService.state(player),profile(stack),WeaponRequirementConfig.UNMET_MULTIPLIER.get());
    }
    public static WeaponRequirementResult evaluate(PlayerAttributeState state,WeaponRequirementProfile profile,double penalty) {
        if(!Double.isFinite(penalty)||penalty<.1||penalty>1) throw new IllegalArgumentException("Invalid requirement penalty");
        Map<Attribute,Integer> missing=new EnumMap<>(Attribute.class);
        if(profile.enabled()) for(Attribute a:WeaponRequirementProfile.ATTRIBUTES) if(state.get(a)<profile.get(a)) missing.put(a,profile.get(a)-state.get(a));
        return new WeaponRequirementResult(missing.isEmpty(),missing,missing.isEmpty()?1:penalty,missing.isEmpty());
    }
    public static boolean meetsRequirements(ServerPlayer player,ItemStack stack) { return evaluate(player,stack).satisfied(); }
    public static Map<Attribute,Integer> missingRequirements(ServerPlayer player,ItemStack stack) { return evaluate(player,stack).missingAttributes(); }
    public static void clear() { compiled=Map.of(); facts=Map.of(); WeaponScalingService.clear(); WeaponDamageProfileService.clear(); WeaponInfusionEligibilityService.clear(); }
    private WeaponRequirementService() {}
}
