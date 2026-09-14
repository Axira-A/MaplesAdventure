package dev.maplesadventure.progression.weapon;

import com.google.gson.*;
import dev.maplesadventure.MaplesAdventure;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import java.util.*;

public final class WeaponInfusionEligibilityRules extends SimpleJsonResourceReloadListener {
    public record Rule(ResourceLocation file,ResourceLocation item,ResourceLocation tag,int priority,boolean infusible,Set<ResourceLocation> allowed) {}
    public static volatile List<Rule> rules=List.of();
    public WeaponInfusionEligibilityRules() { super(new Gson(),"maplesadventure/weapon_infusion_eligibility"); }
    public static Rule parse(ResourceLocation id,JsonObject json) {
        for(String key:json.keySet()) if(!Set.of("item","tag","priority","infusible","allowed").contains(key)) throw new IllegalArgumentException("Unknown eligibility field "+key);
        if(json.has("item")==json.has("tag")) throw new IllegalArgumentException("Specify exactly one item or tag");
        var selector=new JsonObject();
        if(json.has("item")) selector.add("item",json.get("item")); else selector.add("tag",json.get("tag"));
        if(json.has("priority")) selector.add("priority",json.get("priority")); selector.add("scaling",new JsonObject());
        var common=WeaponScalingRules.parse(id,selector);
        if(json.has("infusible")&&(!json.get("infusible").isJsonPrimitive()||!json.getAsJsonPrimitive("infusible").isBoolean()))
            throw new IllegalArgumentException("infusible must be boolean");
        boolean infusible=!json.has("infusible")||json.get("infusible").getAsBoolean();
        var allowed=new HashSet<ResourceLocation>();
        if(json.has("allowed")) {
            var array=json.getAsJsonArray("allowed"); if(array.size()>WeaponInfusionEligibility.MAX_ALLOWED) throw new IllegalArgumentException("Too many allowed infusions");
            for(var value:array) {
                if(!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isString()||value.getAsString().length()>256) throw new IllegalArgumentException("Allowed infusion id bounds");
                if(!allowed.add(ResourceLocation.parse(value.getAsString()))) throw new IllegalArgumentException("Duplicate allowed infusion");
            }
        }
        return new Rule(id,common.item(),common.tag(),common.priority(),infusible,Set.copyOf(allowed));
    }
    @Override protected void apply(Map<ResourceLocation,JsonElement> input,ResourceManager manager,ProfilerFiller profiler) {
        var next=new ArrayList<Rule>();
        input.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry->{
            try { if(next.size()>=4096) throw new IllegalArgumentException("Max 4096 rules"); next.add(parse(entry.getKey(),entry.getValue().getAsJsonObject())); }
            catch(RuntimeException error) { MaplesAdventure.LOGGER.error("Rejected infusion eligibility {}: {}",entry.getKey(),error.getMessage()); }
        });
        next.sort(Comparator.comparingInt(Rule::priority).reversed().thenComparing(Rule::file)); rules=List.copyOf(next);
    }
}
