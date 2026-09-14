package dev.maplesadventure.progression.weapon;

import com.google.gson.*;
import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.progression.Attribute;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import java.util.*;

/** Same exact/tag, priority and per-file rejection contract as requirement rules. */
public final class WeaponScalingRules extends SimpleJsonResourceReloadListener {
    public record Rule(ResourceLocation file, ResourceLocation item, ResourceLocation tag, int priority, WeaponScalingProfile profile) {}
    public static List<Rule> rules = List.of();
    public WeaponScalingRules() { super(new Gson(), "maplesadventure/weapon_scaling"); }
    public static Rule parse(ResourceLocation id, JsonObject json) {
        for (String key : json.keySet()) if (!Set.of("item","tag","priority","scaling","max_bonus","disabled").contains(key))
            throw new IllegalArgumentException("Unknown field " + key);
        if (json.has("item") == json.has("tag")) throw new IllegalArgumentException("Specify exactly one item or tag");
        ResourceLocation item=json.has("item")?ResourceLocation.parse(json.get("item").getAsString()):null;
        ResourceLocation tag=json.has("tag")?ResourceLocation.parse(json.get("tag").getAsString()):null;
        boolean disabled=false;
        if (json.has("disabled")) {
            if (!json.get("disabled").isJsonPrimitive() || !json.getAsJsonPrimitive("disabled").isBoolean())
                throw new IllegalArgumentException("disabled must be boolean");
            disabled=json.get("disabled").getAsBoolean();
        }
        if (!disabled && !json.has("scaling")) throw new IllegalArgumentException("Missing scaling");
        double[] values=new double[5];
        var scaling=json.has("scaling")?json.getAsJsonObject("scaling"):new JsonObject();
        for (String key : scaling.keySet()) {
            var attribute=Attribute.parse(key).orElseThrow(()->new IllegalArgumentException("Unknown attribute " + key));
            int index=WeaponRequirementProfile.ATTRIBUTES.indexOf(attribute);
            if (index<0) throw new IllegalArgumentException("Not a weapon attribute " + key);
            values[index]=number(scaling.get(key),1.5);
        }
        int priority=0;
        if (json.has("priority")) {
            var p=json.get("priority");
            if (!p.isJsonPrimitive() || !p.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("priority must be integer");
            priority=p.getAsBigDecimal().intValueExact();
            if (Math.abs((long)priority)>10000) throw new IllegalArgumentException("priority bounds");
        }
        return new Rule(id,item,tag,priority,new WeaponScalingProfile(values[0],values[1],values[2],values[3],values[4],
                json.has("max_bonus")?number(json.get("max_bonus"),2):WeaponScalingProfile.DEFAULT_MAX_BONUS,
                disabled?"DISABLED":item!=null?"EXACT_JSON":"TAG_JSON","OVERRIDE",id.toString()));
    }
    private static double number(JsonElement e,double max) {
        if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Expected number");
        double n=e.getAsDouble();
        if (!Double.isFinite(n) || n<0 || n>max) throw new IllegalArgumentException("Number outside 0.."+max);
        return n;
    }
    @Override protected void apply(Map<ResourceLocation,JsonElement> input,ResourceManager manager,ProfilerFiller profiler) {
        List<Rule> next=new ArrayList<>();
        input.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry->{
            try {
                if (next.size()>=4096) throw new IllegalArgumentException("Max 4096 rules");
                next.add(parse(entry.getKey(),entry.getValue().getAsJsonObject()));
            } catch(RuntimeException e) { MaplesAdventure.LOGGER.error("Rejected weapon scaling rule {}: {}",entry.getKey(),e.getMessage()); }
        });
        next.sort(Comparator.comparingInt(Rule::priority).reversed().thenComparing(Rule::file));
        rules=List.copyOf(next);
    }
}
