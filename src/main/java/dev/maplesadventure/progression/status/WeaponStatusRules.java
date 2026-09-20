package dev.maplesadventure.progression.status;

import com.google.gson.*;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;

/** Same exact/tag/priority/file-order convention as the existing weapon balance rules. */
public final class WeaponStatusRules extends SimpleJsonResourceReloadListener {
    public record Rule(ResourceLocation file,ResourceLocation item,ResourceLocation tag,int priority,WeaponStatusProfile profile) {}
    private static List<Rule> rules=List.of();
    private static volatile Map<Item,WeaponStatusProfile> compiled=Map.of();
    public WeaponStatusRules() { super(new Gson(),"maplesadventure/weapon_status_buildup"); }
    public static Map<Item,WeaponStatusProfile> compiled() { return compiled; }
    public static WeaponStatusProfile profile(ItemStack stack) { return compiled.getOrDefault(stack.getItem(),WeaponStatusProfile.EMPTY); }
    public static void clear() { rules=List.of(); compiled=Map.of(); }
    public static void compile() {
        var next=new IdentityHashMap<Item,WeaponStatusProfile>();
        for(var item:BuiltInRegistries.ITEM) {
            var id=BuiltInRegistries.ITEM.getKey(item);
            var exact=rules.stream().filter(r->id.equals(r.item())).findFirst();
            var selected=exact.isPresent()?exact:rules.stream().filter(r->r.tag()!=null&&item.builtInRegistryHolder().is(TagKey.create(Registries.ITEM,r.tag()))).findFirst();
            selected.ifPresent(r->next.put(item,r.profile()));
        }
        compiled=Collections.unmodifiableMap(next);
    }
    @Override protected void apply(Map<ResourceLocation,JsonElement> input,ResourceManager manager,ProfilerFiller profiler) {
        var next=new ArrayList<Rule>();
        input.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry->{try {
            if(next.size()>=4096) throw new IllegalArgumentException("Status rule count");
            var v=entry.getValue().getAsJsonObject(); StatusDefinitions.fields(v,Set.of("item","tag","priority","statuses","weight_class"));
            if(v.has("item")==v.has("tag")) throw new IllegalArgumentException("One item or tag required");
            double p=StatusDefinitions.number(v,"priority",0); if(p!=Math.rint(p)||Math.abs(p)>1000000) throw new IllegalArgumentException("Priority bounds");
            String text=v.get(v.has("item")?"item":"tag").getAsString(); if(text.length()>256) throw new IllegalArgumentException("Selector bounds");
            var selector=ResourceLocation.parse(text);
            var profile=v.has("statuses")?WeaponStatusProfile.parse(v.getAsJsonObject("statuses")):WeaponStatusProfile.EMPTY;
            if(v.has("weight_class")) profile=new WeaponStatusProfile(profile.components(),StatusWeaponWeightClass.valueOf(v.get("weight_class").getAsString().toUpperCase(Locale.ROOT)));
            next.add(new Rule(entry.getKey(),v.has("item")?selector:null,v.has("tag")?selector:null,(int)p,profile));
        } catch(RuntimeException e) { dev.maplesadventure.MaplesAdventure.LOGGER.error("Rejected weapon status rule {}: {}",entry.getKey(),e.getMessage()); }});
        next.sort(Comparator.comparingInt(Rule::priority).reversed().thenComparing(Rule::file)); rules=List.copyOf(next);
    }
}
