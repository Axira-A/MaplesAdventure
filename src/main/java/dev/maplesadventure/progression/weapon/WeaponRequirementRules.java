package dev.maplesadventure.progression.weapon;
import com.google.gson.*;
import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.progression.Attribute;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import java.util.*;
public final class WeaponRequirementRules extends SimpleJsonResourceReloadListener {
    public record Rule(ResourceLocation file, ResourceLocation item, ResourceLocation tag, int priority, WeaponRequirementProfile profile) {}
    public static List<Rule> rules=List.of();
    public WeaponRequirementRules() { super(new Gson(),"maplesadventure/weapon_requirements"); }
    public static Rule parse(ResourceLocation id,JsonObject json) {
        for(String key:json.keySet()) if(!Set.of("item","tag","priority","requirements","disabled").contains(key)) throw new IllegalArgumentException("Unknown field "+key);
        if(json.has("item")==json.has("tag")) throw new IllegalArgumentException("Specify exactly one item or tag");
        ResourceLocation item=json.has("item")?ResourceLocation.parse(json.get("item").getAsString()):null;
        ResourceLocation tag=json.has("tag")?ResourceLocation.parse(json.get("tag").getAsString()):null;
        boolean disabled=false;
        if(json.has("disabled")) {
            if(!json.get("disabled").isJsonPrimitive() || !json.getAsJsonPrimitive("disabled").isBoolean()) throw new IllegalArgumentException("disabled must be boolean");
            disabled=json.get("disabled").getAsBoolean();
        }
        if(!disabled && !json.has("requirements")) throw new IllegalArgumentException("Missing requirements");
        int[] values=new int[5]; var req=json.has("requirements")?json.getAsJsonObject("requirements"):new JsonObject();
        for(String key:req.keySet()) {
            var attribute=Attribute.parse(key).orElseThrow(()->new IllegalArgumentException("Unknown attribute "+key));
            int index=WeaponRequirementProfile.ATTRIBUTES.indexOf(attribute);
            if(index<0) throw new IllegalArgumentException("Not a weapon attribute "+key);
            values[index]=integer(req.get(key),0,99);
        }
        return new Rule(id,item,tag,json.has("priority")?integer(json.get("priority"),-10000,10000):0,
                new WeaponRequirementProfile(values[0],values[1],values[2],values[3],values[4],
                        disabled?"DISABLED":item!=null?"EXACT_JSON":"TAG_JSON","OVERRIDE",id.toString()));
    }
    private static int integer(JsonElement e,int min,int max) {
        if(!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Expected integer");
        int value=e.getAsBigDecimal().intValueExact(); if(value<min||value>max) throw new IllegalArgumentException("Integer out of bounds"); return value;
    }
    protected void apply(Map<ResourceLocation,JsonElement> input,ResourceManager manager,ProfilerFiller profiler) {
        List<Rule> next=new ArrayList<>();
        input.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry->{
            try { if(next.size()>=4096) throw new IllegalArgumentException("Max 4096 rules"); next.add(parse(entry.getKey(),entry.getValue().getAsJsonObject())); }
            catch(RuntimeException e) { MaplesAdventure.LOGGER.error("Rejected weapon requirement rule {}: {}",entry.getKey(),e.getMessage()); }
        });
        next.sort(Comparator.comparingInt(Rule::priority).reversed().thenComparing(Rule::file)); rules=List.copyOf(next);
    }
}
